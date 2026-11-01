package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.PhoneNumberFormatter
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ClarityFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- CONTACTS ---

    fun observeContacts(userId: String = requireUserId()): Flow<List<ContactEntity>> = flow {
        val path = "users/$userId/contacts"
        emitAll(
            db.collection("users").document(userId).collection("contacts")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContactEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareByDescending<ContactEntity> { it.isVip }.thenBy { it.name.lowercase() })
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getContactById(targetUserId: String, contactId: String): Result<ContactEntity?> = runCatching {
        val path = "users/$targetUserId/contacts/$contactId"
        try {
            val snap = db.collection("users").document(targetUserId)
                .collection("contacts").document(contactId)
                .get()
                .await()
            snap.toObject(ContactEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun saveContact(contact: ContactEntity): Result<String> = runCatching {
        val uid = requireUserId()
        val contactsRef = db.collection("users").document(uid).collection("contacts")
        val docId = contact.id.ifBlank { UUID.randomUUID().toString().replace("-", "_") }
        val docRef = contactsRef.document(docId)
        try {
            if (contact.id.isNotBlank() && contact.createdAt != null) {
                val payload = mapOf(
                    "name" to contact.name.take(120),
                    "phoneNumber" to contact.phoneNumber.take(40),
                    "normalizedNumber" to contact.normalizedNumber.take(30),
                    "label" to contact.label.take(30),
                    "email" to contact.email.take(160),
                    "organization" to contact.organization.take(120),
                    "isVip" to contact.isVip,
                    "avatarColorIndex" to contact.avatarColorIndex.coerceIn(0, 20),
                    "notes" to contact.notes.take(500),
                    "importSource" to contact.importSource.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to contact.name.take(120).ifBlank { "Unnamed Contact" },
                    "phoneNumber" to contact.phoneNumber.take(40),
                    "normalizedNumber" to contact.normalizedNumber.take(30),
                    "label" to contact.label.take(30).ifBlank { "Mobile" },
                    "email" to contact.email.take(160),
                    "organization" to contact.organization.take(120),
                    "isVip" to contact.isVip,
                    "avatarColorIndex" to contact.avatarColorIndex.coerceIn(0, 20),
                    "notes" to contact.notes.take(500),
                    "importSource" to contact.importSource.take(30).ifBlank { "MANUAL" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteContact(contactId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("contacts").document(contactId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- CALL LOGS ---

    fun observeCallLogs(userId: String = requireUserId()): Flow<List<CallLogEntity>> = flow {
        val path = "users/$userId/callLogs"
        emitAll(
            db.collection("users").document(userId).collection("callLogs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CallLogEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.timestamp }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addCallLog(log: CallLogEntity): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = log.id.ifBlank { UUID.randomUUID().toString().replace("-", "_") }
        val docRef = db.collection("users").document(uid).collection("callLogs").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "phoneNumber" to log.phoneNumber.take(40).ifBlank { "Private Number" },
            "contactName" to log.contactName.take(120).ifBlank { "Unknown Caller" },
            "direction" to log.direction,
            "filterVerdict" to log.filterVerdict,
            "matchedRuleLabel" to log.matchedRuleLabel.take(120),
            "autoReplyDispatched" to log.autoReplyDispatched,
            "durationSeconds" to log.durationSeconds.coerceIn(0, 86400),
            "timestamp" to log.timestamp.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markCallLogAutoReplyDispatched(logId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("callLogs").document(logId)
        try {
            docRef.update(
                mapOf(
                    "autoReplyDispatched" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCallLog(logId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("callLogs").document(logId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun clearAllCallLogs(currentLogs: List<CallLogEntity>): Result<Unit> = runCatching {
        val uid = requireUserId()
        for (log in currentLogs) {
            if (log.id.isNotBlank()) {
                val docRef = db.collection("users").document(uid).collection("callLogs").document(log.id)
                try {
                    docRef.delete().await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, docRef.path)
                }
            }
        }
    }

    // --- FILTER RULES ---

    fun observeFilterRules(userId: String = requireUserId()): Flow<List<FilterRuleEntity>> = flow {
        val path = "users/$userId/filterRules"
        emitAll(
            db.collection("users").document(userId).collection("filterRules")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FilterRuleEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareByDescending<FilterRuleEntity> { it.isEnabled }.thenByDescending { it.blockedCount })
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveFilterRule(rule: FilterRuleEntity): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = rule.id.ifBlank { UUID.randomUUID().toString().replace("-", "_") }
        val docRef = db.collection("users").document(uid).collection("filterRules").document(docId)
        try {
            if (rule.id.isNotBlank() && rule.createdAt != null) {
                val payload = mapOf(
                    "label" to rule.label.take(100),
                    "pattern" to rule.pattern.take(80),
                    "matchType" to rule.matchType,
                    "action" to rule.action,
                    "isEnabled" to rule.isEnabled,
                    "blockedCount" to rule.blockedCount.coerceAtLeast(0),
                    "autoReplyText" to rule.autoReplyText.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "label" to rule.label.take(100).ifBlank { "Custom Filter Rule" },
                    "pattern" to rule.pattern.take(80),
                    "matchType" to rule.matchType,
                    "action" to rule.action,
                    "isEnabled" to rule.isEnabled,
                    "blockedCount" to rule.blockedCount.coerceAtLeast(0),
                    "autoReplyText" to rule.autoReplyText.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun incrementRuleBlockedCount(ruleId: String, currentCount: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("filterRules").document(ruleId)
        try {
            docRef.update(
                mapOf(
                    "blockedCount" to (currentCount + 1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFilterRule(ruleId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("filterRules").document(ruleId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- MESSAGES ---

    fun observeMessages(userId: String = requireUserId()): Flow<List<MessageEntity>> = flow {
        val path = "users/$userId/messages"
        emitAll(
            db.collection("users").document(userId).collection("messages")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MessageEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.timestamp }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addMessage(message: MessageEntity): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = message.id.ifBlank { UUID.randomUUID().toString().replace("-", "_") }
        val docRef = db.collection("users").document(uid).collection("messages").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "phoneNumber" to message.phoneNumber.take(40),
            "normalizedNumber" to message.normalizedNumber.take(30),
            "contactName" to message.contactName.take(120).ifBlank { message.phoneNumber.take(40) },
            "body" to message.body.take(1000),
            "timestamp" to message.timestamp.coerceAtLeast(0L),
            "isOutgoing" to message.isOutgoing,
            "isAutoReply" to message.isAutoReply,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMessagesForThread(normalizedNumber: String, allMessages: List<MessageEntity>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val target = allMessages.filter { it.normalizedNumber == normalizedNumber && it.id.isNotBlank() }
        for (msg in target) {
            val docRef = db.collection("users").document(uid).collection("messages").document(msg.id)
            try {
                docRef.delete().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
            }
        }
    }

    // --- REPLY TEMPLATES ---

    fun observeReplyTemplates(userId: String = requireUserId()): Flow<List<ReplyTemplateEntity>> = flow {
        val path = "users/$userId/replyTemplates"
        emitAll(
            db.collection("users").document(userId).collection("replyTemplates")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReplyTemplateEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addReplyTemplate(title: String, body: String, category: String): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = UUID.randomUUID().toString().replace("-", "_")
        val docRef = db.collection("users").document(uid).collection("replyTemplates").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(80).ifBlank { "Quick Reply" },
            "body" to body.take(500),
            "category" to category.take(40).ifBlank { "CUSTOM" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteReplyTemplate(templateId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("replyTemplates").document(templateId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- SHIELD SETTINGS (FIRESTORE) ---

    fun observeShieldSettings(userId: String = requireUserId()): Flow<ShieldSettings> = flow {
        val path = "users/$userId/settings/shield"
        emitAll(
            db.collection("users").document(userId).collection("settings").document("shield")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ShieldSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: ShieldSettings(userId = userId)
                    } else {
                        ShieldSettings(userId = userId)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveShieldSettings(settings: ShieldSettings, existingCreatedAt: Timestamp?): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("shield")
        try {
            val safeRoute = if (settings.defaultAudioRoute in listOf("BLUETOOTH", "EARPIECE", "SPEAKER")) {
                settings.defaultAudioRoute
            } else {
                "BLUETOOTH"
            }
            if (existingCreatedAt != null) {
                val payload = mapOf(
                    "masterShieldEnabled" to settings.masterShieldEnabled,
                    "blockUnknownCallers" to settings.blockUnknownCallers,
                    "silenceUnknownCallers" to settings.silenceUnknownCallers,
                    "blockPrivateNumbers" to settings.blockPrivateNumbers,
                    "quietHoursEnabled" to settings.quietHoursEnabled,
                    "quietStartHour" to settings.quietStartHour.coerceIn(0, 23),
                    "quietEndHour" to settings.quietEndHour.coerceIn(0, 23),
                    "vipBypassQuietHours" to settings.vipBypassQuietHours,
                    "defaultAutoReplyEnabled" to settings.defaultAutoReplyEnabled,
                    "defaultAutoReplyText" to settings.defaultAutoReplyText.take(300),
                    "darkModeEnabled" to settings.darkModeEnabled,
                    "keypadTonesEnabled" to settings.keypadTonesEnabled,
                    "hapticsEnabled" to settings.hapticsEnabled,
                    "preferInAppCallUi" to settings.preferInAppCallUi,
                    "autoRouteBluetooth" to settings.autoRouteBluetooth,
                    "defaultAudioRoute" to safeRoute,
                    "preferredBluetoothDeviceName" to settings.preferredBluetoothDeviceName.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "masterShieldEnabled" to settings.masterShieldEnabled,
                    "blockUnknownCallers" to settings.blockUnknownCallers,
                    "silenceUnknownCallers" to settings.silenceUnknownCallers,
                    "blockPrivateNumbers" to settings.blockPrivateNumbers,
                    "quietHoursEnabled" to settings.quietHoursEnabled,
                    "quietStartHour" to settings.quietStartHour.coerceIn(0, 23),
                    "quietEndHour" to settings.quietEndHour.coerceIn(0, 23),
                    "vipBypassQuietHours" to settings.vipBypassQuietHours,
                    "defaultAutoReplyEnabled" to settings.defaultAutoReplyEnabled,
                    "defaultAutoReplyText" to settings.defaultAutoReplyText.take(300),
                    "darkModeEnabled" to settings.darkModeEnabled,
                    "keypadTonesEnabled" to settings.keypadTonesEnabled,
                    "hapticsEnabled" to settings.hapticsEnabled,
                    "preferInAppCallUi" to settings.preferInAppCallUi,
                    "autoRouteBluetooth" to settings.autoRouteBluetooth,
                    "defaultAudioRoute" to safeRoute,
                    "preferredBluetoothDeviceName" to settings.preferredBluetoothDeviceName.take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- STARTER SEEDING FOR NEW USERS ---

    suspend fun seedStarterDataIfEmpty(force: Boolean = false): Result<Boolean> = runCatching {
        val uid = requireUserId()
        if (!force) {
            val existing = db.collection("users").document(uid).collection("contacts")
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return@runCatching false
        }

        val now = System.currentTimeMillis()
        val starterContacts = listOf(
            ContactEntity(
                name = "Elena Vance",
                phoneNumber = "(415) 890-2314",
                normalizedNumber = PhoneNumberFormatter.normalize("(415) 890-2314"),
                label = "Mobile",
                email = "elena.vance@studio-arch.io",
                organization = "Vance Architectural Studio",
                isVip = true,
                avatarColorIndex = 0,
                notes = "Lead architect — always allow during Quiet Hours",
                importSource = "STARTER"
            ),
            ContactEntity(
                name = "Dr. Marcus Chen",
                phoneNumber = "(650) 442-8810",
                normalizedNumber = PhoneNumberFormatter.normalize("(650) 442-8810"),
                label = "Medical",
                email = "mchen@bayhealth.org",
                organization = "Bay Area Medical Center",
                isVip = true,
                avatarColorIndex = 1,
                notes = "Family physician & clinic line",
                importSource = "STARTER"
            ),
            ContactEntity(
                name = "Claire Sterling",
                phoneNumber = "(212) 555-0148",
                normalizedNumber = PhoneNumberFormatter.normalize("(212) 555-0148"),
                label = "Work",
                email = "claire@sterlinglegal.com",
                organization = "Sterling & Partners",
                isVip = true,
                avatarColorIndex = 2,
                notes = "Partner desk",
                importSource = "STARTER"
            ),
            ContactEntity(
                name = "Julian Mercer",
                phoneNumber = "(312) 609-7721",
                normalizedNumber = PhoneNumberFormatter.normalize("(312) 609-7721"),
                label = "Mobile",
                email = "julian.mercer@proton.me",
                organization = "Mercer Design Lab",
                isVip = false,
                avatarColorIndex = 3,
                notes = "Industrial design collaborator",
                importSource = "STARTER"
            ),
            ContactEntity(
                name = "Sofia Lindqvist",
                phoneNumber = "(206) 714-9032",
                normalizedNumber = PhoneNumberFormatter.normalize("(206) 714-9032"),
                label = "Home",
                email = "sofia@nordiccraft.se",
                organization = "Nordic Craft Co.",
                isVip = false,
                avatarColorIndex = 4,
                notes = "Imported via vCard backup",
                importSource = "VCF_FILE"
            )
        )
        starterContacts.forEach { saveContact(it) }

        val starterRules = listOf(
            FilterRuleEntity(
                label = "Toll-Free Telemarketers (800)",
                pattern = "1800",
                matchType = "PREFIX",
                action = "BLOCK",
                isEnabled = true,
                blockedCount = 14,
                autoReplyText = "This number does not accept automated promotional calls."
            ),
            FilterRuleEntity(
                label = "Specific Blocked Spam Number",
                pattern = "4155550199",
                matchType = "EXACT",
                action = "BLOCK",
                isEnabled = true,
                blockedCount = 6,
                autoReplyText = "Your number has been blocked by ClarityDial."
            ),
            FilterRuleEntity(
                label = "Silence Unverified Exchange (555-019)",
                pattern = "555019",
                matchType = "CONTAINS",
                action = "SILENCE",
                isEnabled = true,
                blockedCount = 5,
                autoReplyText = "Hi, I'm screening calls right now. Please send an SMS with your name."
            )
        )
        starterRules.forEach { saveFilterRule(it) }

        val starterLogs = listOf(
            CallLogEntity(
                phoneNumber = "(415) 890-2314",
                contactName = "Elena Vance",
                timestamp = now - 15 * 60 * 1000L,
                direction = "INCOMING",
                filterVerdict = "VIP_BYPASS",
                matchedRuleLabel = "VIP Starred Contact Bypass",
                autoReplyDispatched = false,
                durationSeconds = 245
            ),
            CallLogEntity(
                phoneNumber = "(312) 609-7721",
                contactName = "Julian Mercer",
                timestamp = now - 35 * 60 * 1000L,
                direction = "MISSED",
                filterVerdict = "ALLOWED",
                matchedRuleLabel = "Missed Incoming Call",
                autoReplyDispatched = false,
                durationSeconds = 0
            ),
            CallLogEntity(
                phoneNumber = "+1 (800) 555-0199",
                contactName = "Unknown Telemarketer",
                timestamp = now - 52 * 60 * 1000L,
                direction = "MISSED",
                filterVerdict = "BLOCKED",
                matchedRuleLabel = "Toll-Free Telemarketers (800)",
                autoReplyDispatched = true,
                durationSeconds = 0
            ),
            CallLogEntity(
                phoneNumber = "Private Number",
                contactName = "Hidden Caller ID",
                timestamp = now - 2 * 3600 * 1000L,
                direction = "MISSED",
                filterVerdict = "BLOCKED",
                matchedRuleLabel = "Block Private & Hidden Numbers",
                autoReplyDispatched = false,
                durationSeconds = 0
            ),
            CallLogEntity(
                phoneNumber = "(650) 442-8810",
                contactName = "Dr. Marcus Chen",
                timestamp = now - 5 * 3600 * 1000L,
                direction = "OUTGOING",
                filterVerdict = "ALLOWED",
                matchedRuleLabel = "Outbound Dial",
                autoReplyDispatched = false,
                durationSeconds = 180
            ),
            CallLogEntity(
                phoneNumber = "(310) 555-0194",
                contactName = "Unlisted Caller",
                timestamp = now - 9 * 3600 * 1000L,
                direction = "MISSED",
                filterVerdict = "SILENCED",
                matchedRuleLabel = "Silence Unverified Exchange (555-019)",
                autoReplyDispatched = true,
                durationSeconds = 0
            )
        )
        starterLogs.forEach { addCallLog(it) }

        val starterMessages = listOf(
            MessageEntity(
                phoneNumber = "(415) 890-2314",
                normalizedNumber = PhoneNumberFormatter.normalize("(415) 890-2314"),
                contactName = "Elena Vance",
                body = "Hi Elena, reviewing the blueprint revisions now. Ready for our 3 PM call.",
                timestamp = now - 3 * 3600 * 1000L,
                isOutgoing = true,
                isAutoReply = false
            ),
            MessageEntity(
                phoneNumber = "(415) 890-2314",
                normalizedNumber = PhoneNumberFormatter.normalize("(415) 890-2314"),
                contactName = "Elena Vance",
                body = "Sounds great! I'll ring you once I'm back from the site visit.",
                timestamp = now - 2 * 3600 * 1000L,
                isOutgoing = false,
                isAutoReply = false
            ),
            MessageEntity(
                phoneNumber = "(310) 555-0194",
                normalizedNumber = PhoneNumberFormatter.normalize("(310) 555-0194"),
                contactName = "Unlisted Caller",
                body = "Hi, I'm screening calls right now. Please send an SMS with your name.",
                timestamp = now - 9 * 3600 * 1000L,
                isOutgoing = true,
                isAutoReply = true
            )
        )
        starterMessages.forEach { addMessage(it) }

        val starterTemplates = listOf(
             Triple(
                "In a Meeting — Text Instead",
                "Hi, I'm currently in a meeting and can't answer. Please text me if it's urgent.",
                "BUSY"
            ),
            Triple(
                "Call Screened — Identify Yourself",
                "Your call was filtered by automated call screening. Please reply with your name and reason for calling.",
                "FILTER_REPLY"
            ),
            Triple(
                "Quiet Hours Active",
                "I have Quiet Hours enabled right now. I'll return your call tomorrow morning.",
                "FILTER_REPLY"
            )
        )
        starterTemplates.forEach { (title, body, cat) ->
            addReplyTemplate(title, body, cat)
        }

        saveShieldSettings(ShieldSettings(userId = uid), existingCreatedAt = null)
        true
    }
}
