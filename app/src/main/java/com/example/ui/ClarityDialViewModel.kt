package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogEntity
import com.example.data.CallSimulationResult
import com.example.data.ClarityFirestoreRepository
import com.example.data.ContactEntity
import com.example.data.FilterRuleEntity
import com.example.data.ImportedContactCandidate
import com.example.data.MessageEntity
import com.example.data.MessageThreadSummary
import com.example.data.ReplyTemplateEntity
import com.example.data.ShieldSettings
import com.example.domain.CallFilterEngine
import com.example.domain.ContactImportParser
import com.example.domain.PhoneNumberFormatter
import com.example.telecom.ActiveCallManager
import com.example.telecom.ActiveCallSession
import com.example.telecom.ActiveCallStatus
import com.example.telecom.CallAudioRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    KEYPAD,
    RECENTS,
    CONTACTS,
    MESSAGES,
    SHIELD,
    SETTINGS
}

class ClarityDialViewModel(
    private val repository: ClarityFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(AppTab.KEYPAD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _dialInput = MutableStateFlow("")
    val dialInput: StateFlow<String> = _dialInput.asStateFlow()

    private val _activeThreadNumber = MutableStateFlow<String?>(null)
    val activeThreadNumber: StateFlow<String?> = _activeThreadNumber.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _lastSimulationResult = MutableStateFlow<CallSimulationResult?>(null)
    val lastSimulationResult: StateFlow<CallSimulationResult?> = _lastSimulationResult.asStateFlow()

    private val _isFullScreenImportOpen = MutableStateFlow(false)
    val isFullScreenImportOpen: StateFlow<Boolean> = _isFullScreenImportOpen.asStateFlow()

    private val _importCandidates = MutableStateFlow<List<ImportedContactCandidate>>(emptyList())
    val importCandidates: StateFlow<List<ImportedContactCandidate>> = _importCandidates.asStateFlow()

    private val _selectedCallLogForDetail = MutableStateFlow<CallLogEntity?>(null)
    val selectedCallLogForDetail: StateFlow<CallLogEntity?> = _selectedCallLogForDetail.asStateFlow()

    val activeCallSession: StateFlow<ActiveCallSession?> = ActiveCallManager.activeCall

    val contacts: StateFlow<List<ContactEntity>> = repository.observeContacts(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing contacts", e)
            _statusBannerMessage.value = "Could not sync contacts: ${e.localizedMessage ?: "error"}"
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.observeCallLogs(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing call logs", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filterRules: StateFlow<List<FilterRuleEntity>> = repository.observeFilterRules(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing filter rules", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messages: StateFlow<List<MessageEntity>> = repository.observeMessages(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing messages", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val replyTemplates: StateFlow<List<ReplyTemplateEntity>> = repository.observeReplyTemplates(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing reply templates", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shieldSettings: StateFlow<ShieldSettings> = repository.observeShieldSettings(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing shield settings", e)
            emit(ShieldSettings(userId = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ShieldSettings(userId = currentUserId))

    val messageThreads: StateFlow<List<MessageThreadSummary>> = combine(
        messages,
        contacts
    ) { msgList, contactList ->
        val contactByNorm = contactList.associateBy { it.normalizedNumber }
        msgList.groupBy { it.normalizedNumber.ifBlank { PhoneNumberFormatter.normalize(it.phoneNumber) } }
            .filterKeys { it.isNotBlank() }
            .map { (norm, threadMessages) ->
                val sorted = threadMessages.sortedBy { it.timestamp }
                val latest = sorted.last()
                val contact = contactByNorm[norm]
                MessageThreadSummary(
                    normalizedNumber = norm,
                    displayNumber = contact?.phoneNumber ?: PhoneNumberFormatter.formatForDisplay(latest.phoneNumber),
                    contactName = contact?.name ?: latest.contactName.ifBlank { PhoneNumberFormatter.formatForDisplay(latest.phoneNumber) },
                    isVip = contact?.isVip ?: false,
                    avatarColorIndex = contact?.avatarColorIndex ?: (norm.hashCode().and(0x7FFFFFFF) % 7),
                    lastMessageBody = latest.body,
                    lastTimestamp = latest.timestamp,
                    messageCount = sorted.size,
                    hasAutoReply = sorted.any { it.isAutoReply }
                )
            }
            .sortedByDescending { it.lastTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.seedStarterDataIfEmpty()
        }

        // Keep ActiveCallManager's cached rules, contacts, and settings synced for ClarityCallScreeningService
        viewModelScope.launch {
            combine(shieldSettings, contacts, filterRules) { settings, contactList, ruleList ->
                Triple(settings, contactList, ruleList)
            }.collect { (settings, contactList, ruleList) ->
                ActiveCallManager.updateCachedFilterState(settings, contactList, ruleList)
            }
        }

        // Observe calls screened by Android's ClarityCallScreeningService and log them to Cloud Firestore
        viewModelScope.launch {
            ActiveCallManager.screenedCallEvents.collect { screened ->
                val direction = if (screened.verdict == "BLOCKED" || screened.verdict == "SILENCED") {
                    "MISSED"
                } else {
                    "INCOMING"
                }
                repository.addCallLog(
                    CallLogEntity(
                        userId = currentUserId,
                        phoneNumber = screened.phoneNumber,
                        contactName = screened.contactName,
                        timestamp = System.currentTimeMillis(),
                        direction = direction,
                        filterVerdict = screened.verdict,
                        matchedRuleLabel = screened.matchedRuleLabel,
                        autoReplyDispatched = false,
                        durationSeconds = 0
                    )
                )
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusBannerMessage.value = message
    }

    fun handleIncomingTelIntentNumber(phoneNumber: String) {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return
        _dialInput.value = clean
        _selectedTab.value = AppTab.KEYPAD
        _statusBannerMessage.value = "Loaded ${PhoneNumberFormatter.formatForDisplay(clean)} from tel: link"
    }

    // --- DIALER & ACTIVE CALL ACTIONS ---

    fun appendDialDigit(char: String) {
        if (_dialInput.value.length < 20) {
            _dialInput.value += char
            ActiveCallManager.playDialpadKeyToneIfEnabled(char)
        }
    }

    fun backspaceDialDigit() {
        val current = _dialInput.value
        if (current.isNotEmpty()) {
            _dialInput.value = current.dropLast(1)
        }
    }

    fun clearDialInput() {
        _dialInput.value = ""
    }

    fun setDialNumber(number: String) {
        _dialInput.value = number
    }

    fun initiateCall(rawNumber: String, onFallbackToSystemDialer: (String) -> Unit) {
        val clean = rawNumber.trim()
        if (clean.isEmpty()) return

        if (shieldSettings.value.preferInAppCallUi) {
            ActiveCallManager.startInAppCall(
                rawNumber = clean,
                contacts = contacts.value,
                direction = "OUTGOING",
                initialStatus = ActiveCallStatus.DIALING
            )
        } else {
            recordOutgoingCall(clean, onFallbackToSystemDialer)
        }
    }

    fun recordOutgoingCall(rawNumber: String, onReadyToDial: (String) -> Unit) {
        val clean = rawNumber.trim()
        if (clean.isEmpty()) return
        val normalized = PhoneNumberFormatter.normalize(clean)
        val matchedContact = contacts.value.firstOrNull { it.normalizedNumber == normalized }
        val formatted = matchedContact?.phoneNumber ?: PhoneNumberFormatter.formatForDisplay(clean)
        val contactName = matchedContact?.name ?: "Outbound Number"

        viewModelScope.launch {
            repository.addCallLog(
                CallLogEntity(
                    phoneNumber = formatted,
                    contactName = contactName,
                    timestamp = System.currentTimeMillis(),
                    direction = "OUTGOING",
                    filterVerdict = "ALLOWED",
                    matchedRuleLabel = "Outbound Dial",
                    autoReplyDispatched = false,
                    durationSeconds = 60
                )
            )
        }
        onReadyToDial(clean)
    }

    fun answerActiveCall() {
        ActiveCallManager.answerCall()
    }

    fun toggleActiveCallMute() {
        ActiveCallManager.toggleMute()
    }

    fun selectActiveCallAudioRoute(route: CallAudioRoute, bluetoothDeviceName: String?) {
        ActiveCallManager.setAudioRoute(route, bluetoothDeviceName)
    }

    fun toggleActiveCallHold() {
        ActiveCallManager.toggleHold()
    }

    fun sendActiveCallDtmf(digit: Char) {
        ActiveCallManager.sendDtmfDigit(digit)
    }

    fun endActiveCallAndLog() {
        val endedSession = ActiveCallManager.endActiveCall() ?: return
        val wasMissedIncoming = endedSession.direction == "INCOMING" && endedSession.status == ActiveCallStatus.RINGING
        val direction = if (wasMissedIncoming) "MISSED" else endedSession.direction
        val duration = if (wasMissedIncoming) 0 else endedSession.elapsedSeconds.coerceAtLeast(1)
        val routeSuffix = when (endedSession.audioRoute) {
            CallAudioRoute.BLUETOOTH -> " · BT (${endedSession.activeBluetoothDeviceName})"
            CallAudioRoute.SPEAKER -> " · Speaker"
            CallAudioRoute.EARPIECE -> ""
        }

        viewModelScope.launch {
            repository.addCallLog(
                CallLogEntity(
                    userId = currentUserId,
                    phoneNumber = endedSession.formattedNumber,
                    contactName = endedSession.contactName,
                    timestamp = System.currentTimeMillis(),
                    direction = direction,
                    filterVerdict = endedSession.shieldVerdict,
                    matchedRuleLabel = "${endedSession.shieldRuleLabel}$routeSuffix",
                    autoReplyDispatched = false,
                    durationSeconds = duration
                )
            )
        }
    }

    fun simulateIncomingRingingCall(rawNumber: String = "(415) 890-2314") {
        ActiveCallManager.startInAppCall(
            rawNumber = rawNumber,
            contacts = contacts.value,
            direction = "INCOMING",
            initialStatus = ActiveCallStatus.RINGING,
            shieldVerdict = "ALLOWED",
            shieldRuleLabel = "Screened Incoming Call"
        )
    }

    // --- CALL HISTORY DETAIL & ACTIONS ---

    fun openCallLogDetail(log: CallLogEntity) {
        _selectedCallLogForDetail.value = log
    }

    fun closeCallLogDetail() {
        _selectedCallLogForDetail.value = null
    }

    fun logCustomCallEntry(
        phoneNumber: String,
        contactName: String,
        direction: String, // INCOMING, OUTGOING, MISSED
        durationSeconds: Int
    ) {
        val norm = PhoneNumberFormatter.normalize(phoneNumber)
        val matchedContact = contacts.value.firstOrNull { it.normalizedNumber == norm }
        val resolvedName = contactName.ifBlank { matchedContact?.name ?: "Unknown Caller" }
        val formattedNumber = PhoneNumberFormatter.formatForDisplay(phoneNumber)
        val statusLabel = when (direction) {
            "MISSED" -> "Missed Incoming Call"
            "INCOMING" -> "Answered Incoming Call"
            else -> "Outbound Dial"
        }

        viewModelScope.launch {
            repository.addCallLog(
                CallLogEntity(
                    phoneNumber = formattedNumber,
                    contactName = resolvedName,
                    timestamp = System.currentTimeMillis(),
                    direction = direction,
                    filterVerdict = if (matchedContact?.isVip == true) "VIP_BYPASS" else "ALLOWED",
                    matchedRuleLabel = statusLabel,
                    durationSeconds = if (direction == "MISSED") 0 else durationSeconds
                )
            )
            _statusBannerMessage.value = "Logged $direction call for $resolvedName"
        }
    }

    fun deleteCallLog(logId: String) {
        viewModelScope.launch {
            repository.deleteCallLog(logId)
            if (_selectedCallLogForDetail.value?.id == logId) {
                _selectedCallLogForDetail.value = null
            }
        }
    }

    fun clearAllCallLogs() {
        val current = callLogs.value
        viewModelScope.launch {
            repository.clearAllCallLogs(current)
            _statusBannerMessage.value = "Call history cleared"
        }
    }

    fun restoreStarterDemoData() {
        viewModelScope.launch {
            repository.seedStarterDataIfEmpty(force = true)
            _statusBannerMessage.value = "Restored starter contacts, call logs, and filter rules in Cloud Firestore."
        }
    }

    // --- CONTACTS & FULL-SCREEN IMPORT STUDIO ---

    fun openFullScreenImport() {
        _isFullScreenImportOpen.value = true
    }

    fun closeFullScreenImport() {
        _isFullScreenImportOpen.value = false
        _importCandidates.value = emptyList()
    }

    fun parseImportText(rawText: String, sourceTag: String) {
        val parsed = ContactImportParser.parseContent(
            rawText = rawText,
            sourceTag = sourceTag,
            existingContacts = contacts.value
        )
        _importCandidates.value = parsed
        if (parsed.isEmpty()) {
            _statusBannerMessage.value = "No valid phone numbers found in the provided input."
        } else {
            val dupes = parsed.count { it.isDuplicate }
            _statusBannerMessage.value = "Parsed ${parsed.size} contacts (${dupes} existing duplicates flagged)."
        }
    }

    fun addSystemPickedContactCandidate(name: String, phone: String) {
        val normalized = PhoneNumberFormatter.normalize(phone)
        if (normalized.isEmpty()) {
            _statusBannerMessage.value = "Selected system contact has no valid phone number."
            return
        }
        val candidate = ImportedContactCandidate(
            tempId = java.util.UUID.randomUUID().toString(),
            name = name.ifBlank { "System Contact" },
            phoneNumber = PhoneNumberFormatter.formatForDisplay(phone),
            normalizedNumber = normalized,
            label = "Mobile",
            importSource = "SYSTEM_PICKER"
        )
        val updated = ContactImportParser.markDuplicates(
            candidates = _importCandidates.value + candidate,
            existingContacts = contacts.value
        )
        _importCandidates.value = updated
        _statusBannerMessage.value = "Added ${candidate.name} to import staging list."
    }

    fun toggleCandidateSelection(tempId: String) {
        _importCandidates.value = _importCandidates.value.map {
            if (it.tempId == tempId) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun toggleCandidateVip(tempId: String) {
        _importCandidates.value = _importCandidates.value.map {
            if (it.tempId == tempId) it.copy(isVip = !it.isVip) else it
        }
    }

    fun setAllCandidatesSelected(selected: Boolean) {
        _importCandidates.value = _importCandidates.value.map { it.copy(isSelected = selected) }
    }

    fun setAllSelectedCandidatesVip(vip: Boolean) {
        _importCandidates.value = _importCandidates.value.map {
            if (it.isSelected) it.copy(isVip = vip) else it
        }
    }

    fun commitStagedContacts(overwriteDuplicates: Boolean) {
        val toCommit = _importCandidates.value.filter { it.isSelected }
        if (toCommit.isEmpty()) {
            _statusBannerMessage.value = "Select at least one contact to import."
            return
        }
        val existingMap = contacts.value.associateBy { it.normalizedNumber }
        viewModelScope.launch {
            var importedCount = 0
            var skippedCount = 0
            toCommit.forEachIndexed { idx, candidate ->
                val existing = existingMap[candidate.normalizedNumber]
                if (existing != null && !overwriteDuplicates) {
                    skippedCount++
                } else {
                    val entity = ContactEntity(
                        id = existing?.id ?: "",
                        userId = currentUserId,
                        name = candidate.name,
                        phoneNumber = candidate.phoneNumber,
                        normalizedNumber = candidate.normalizedNumber,
                        label = candidate.label,
                        email = candidate.email,
                        organization = candidate.organization,
                        isVip = candidate.isVip || (existing?.isVip == true),
                        avatarColorIndex = existing?.avatarColorIndex ?: ((contacts.value.size + idx) % 7),
                        notes = candidate.notes.ifBlank { existing?.notes.orEmpty() },
                        importSource = candidate.importSource,
                        createdAt = existing?.createdAt
                    )
                    repository.saveContact(entity)
                    importedCount++
                }
            }
            _importCandidates.value = emptyList()
            _isFullScreenImportOpen.value = false
            _statusBannerMessage.value = if (skippedCount > 0) {
                "Imported $importedCount contacts to Cloud Firestore ($skippedCount duplicates skipped)."
            } else {
                "Imported $importedCount contacts to Cloud Firestore."
            }
        }
    }

    fun saveContact(
        existing: ContactEntity?,
        name: String,
        phoneNumber: String,
        label: String,
        email: String,
        organization: String,
        isVip: Boolean,
        notes: String
    ) {
        val norm = PhoneNumberFormatter.normalize(phoneNumber)
        if (name.isBlank() || norm.isEmpty()) return
        val formatted = PhoneNumberFormatter.formatForDisplay(phoneNumber)
        val colorIdx = existing?.avatarColorIndex ?: (contacts.value.size % 7)

        viewModelScope.launch {
            val entity = ContactEntity(
                id = existing?.id ?: "",
                userId = currentUserId,
                name = name.trim(),
                phoneNumber = formatted,
                normalizedNumber = norm,
                label = label,
                email = email.trim(),
                organization = organization.trim(),
                isVip = isVip,
                avatarColorIndex = colorIdx,
                notes = notes.trim(),
                importSource = existing?.importSource ?: "MANUAL",
                createdAt = existing?.createdAt
            )
            repository.saveContact(entity)
            _statusBannerMessage.value = "Saved ${name.trim()} to Cloud Contacts."
        }
    }

    fun toggleContactVip(contact: ContactEntity) {
        viewModelScope.launch {
            repository.saveContact(contact.copy(isVip = !contact.isVip))
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            _statusBannerMessage.value = "Contact removed."
        }
    }

    // --- MESSAGING & TEMPLATES ---

    fun openMessageThreadForNumber(rawNumber: String) {
        val norm = PhoneNumberFormatter.normalize(rawNumber)
        if (norm.isEmpty()) return
        _activeThreadNumber.value = norm
        _selectedTab.value = AppTab.MESSAGES
    }

    fun closeActiveMessageThread() {
        _activeThreadNumber.value = null
    }

    fun sendOrLogMessage(
        rawNumber: String,
        body: String,
        isAutoReply: Boolean = false,
        onLaunchSystemSms: ((String, String) -> Unit)? = null
    ) {
        val cleanBody = body.trim()
        val norm = PhoneNumberFormatter.normalize(rawNumber)
        if (cleanBody.isEmpty() || norm.isEmpty()) return

        val matchedContact = contacts.value.firstOrNull { it.normalizedNumber == norm }
        val displayPhone = matchedContact?.phoneNumber ?: PhoneNumberFormatter.formatForDisplay(rawNumber)
        val displayName = matchedContact?.name ?: displayPhone

        viewModelScope.launch {
            repository.addMessage(
                MessageEntity(
                    userId = currentUserId,
                    phoneNumber = displayPhone,
                    normalizedNumber = norm,
                    contactName = displayName,
                    body = cleanBody,
                    timestamp = System.currentTimeMillis(),
                    isOutgoing = true,
                    isAutoReply = isAutoReply
                )
            )
        }
        onLaunchSystemSms?.invoke(rawNumber, cleanBody)
    }

    fun deleteMessageThread(normalizedNumber: String) {
        val currentMsgs = messages.value
        viewModelScope.launch {
            repository.deleteMessagesForThread(normalizedNumber, currentMsgs)
            if (_activeThreadNumber.value == normalizedNumber) {
                _activeThreadNumber.value = null
            }
        }
    }

    fun addReplyTemplate(title: String, body: String, category: String) {
        if (title.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            repository.addReplyTemplate(title.trim(), body.trim(), category)
            _statusBannerMessage.value = "Saved reply template '${title.trim()}'."
        }
    }

    fun deleteReplyTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteReplyTemplate(templateId)
        }
    }

    // --- AUTOMATED CALL FILTER RULES & SHIELD SETTINGS ---

    fun updateShieldSettings(transform: (ShieldSettings) -> ShieldSettings) {
        val current = shieldSettings.value
        val updated = transform(current)
        viewModelScope.launch {
            repository.saveShieldSettings(updated, current.createdAt)
        }
    }

    fun addOrUpdateFilterRule(
        existing: FilterRuleEntity? = null,
        label: String,
        pattern: String,
        matchType: String,
        action: String,
        autoReplyText: String
    ) {
        if (label.isBlank() || pattern.isBlank()) return
        viewModelScope.launch {
            repository.saveFilterRule(
                FilterRuleEntity(
                    id = existing?.id ?: "",
                    userId = currentUserId,
                    label = label.trim(),
                    pattern = pattern.trim(),
                    matchType = matchType,
                    action = action,
                    isEnabled = existing?.isEnabled ?: true,
                    blockedCount = existing?.blockedCount ?: 0,
                    autoReplyText = autoReplyText.trim(),
                    createdAt = existing?.createdAt
                )
            )
            _statusBannerMessage.value = "Filter rule '${label.trim()}' saved."
        }
    }

    fun quickBlockSpecificNumber(phoneNumber: String, callerLabel: String) {
        val norm = PhoneNumberFormatter.normalize(phoneNumber)
        if (norm.isEmpty()) {
            updateShieldSettings { it.copy(blockPrivateNumbers = true) }
            _statusBannerMessage.value = "Enabled blocking for Private / Hidden numbers."
            return
        }
        val existingRule = filterRules.value.firstOrNull {
            PhoneNumberFormatter.normalize(it.pattern) == norm && it.matchType == "EXACT"
        }
        viewModelScope.launch {
            if (existingRule != null) {
                repository.saveFilterRule(existingRule.copy(isEnabled = true, action = "BLOCK"))
            } else {
                repository.saveFilterRule(
                    FilterRuleEntity(
                        userId = currentUserId,
                        label = "Blocked: ${callerLabel.ifBlank { PhoneNumberFormatter.formatForDisplay(phoneNumber) }}",
                        pattern = norm,
                        matchType = "EXACT",
                        action = "BLOCK",
                        isEnabled = true,
                        blockedCount = 1,
                        autoReplyText = ""
                    )
                )
            }
            _statusBannerMessage.value = "Blocked ${PhoneNumberFormatter.formatForDisplay(phoneNumber)}."
        }
    }

    fun toggleFilterRule(rule: FilterRuleEntity) {
        viewModelScope.launch {
            repository.saveFilterRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteFilterRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteFilterRule(ruleId)
            _statusBannerMessage.value = "Filter rule removed."
        }
    }

    // --- LIVE INCOMING CALL SCREENING SIMULATOR ---

    fun simulateIncomingCall(
        rawNumber: String,
        isPrivateNumber: Boolean,
        simulatedHour: Int
    ) {
        val result = CallFilterEngine.evaluateIncomingCall(
            rawNumber = rawNumber,
            isPrivateNumber = isPrivateNumber,
            simulatedHour = simulatedHour,
            settings = shieldSettings.value,
            contacts = contacts.value,
            rules = filterRules.value
        )
        _lastSimulationResult.value = result

        val direction = if (result.verdict == "BLOCKED" || result.verdict == "SILENCED") {
            "MISSED"
        } else {
            "INCOMING"
        }

        viewModelScope.launch {
            val matchedId = result.matchedRuleId
            if (!matchedId.isNullOrBlank()) {
                val rule = filterRules.value.firstOrNull { it.id == matchedId }
                if (rule != null) {
                    repository.incrementRuleBlockedCount(matchedId, rule.blockedCount)
                }
            }

            repository.addCallLog(
                CallLogEntity(
                    userId = currentUserId,
                    phoneNumber = result.phoneNumber,
                    contactName = result.contactName,
                    timestamp = System.currentTimeMillis(),
                    direction = direction,
                    filterVerdict = result.verdict,
                    matchedRuleLabel = result.matchedRuleLabel,
                    autoReplyDispatched = false,
                    durationSeconds = if (direction == "INCOMING") 95 else 0
                )
            )
        }
    }

    fun clearSimulationResult() {
        _lastSimulationResult.value = null
    }

    private companion object {
        const val TAG = "ClarityDialVM"
    }
}
