package com.example.telecom

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.data.CallSimulationResult
import com.example.data.ContactEntity
import com.example.data.FilterRuleEntity
import com.example.data.ShieldSettings
import com.example.domain.CallFilterEngine
import com.example.domain.PhoneNumberFormatter
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ActiveCallStatus {
    RINGING,
    DIALING,
    CONNECTED,
    ON_HOLD,
    ENDED
}

data class ActiveCallSession(
    val callId: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val formattedNumber: String,
    val contactName: String,
    val contactLabel: String = "Mobile",
    val isVip: Boolean = false,
    val direction: String = "OUTGOING", // OUTGOING or INCOMING
    val status: ActiveCallStatus = ActiveCallStatus.DIALING,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val elapsedSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isBluetoothOn: Boolean = true,
    val audioRoute: CallAudioRoute = CallAudioRoute.BLUETOOTH,
    val activeBluetoothDeviceName: String = "Studio Pro Hands-Free",
    val isOnHold: Boolean = false,
    val dtmfInput: String = "",
    val shieldVerdict: String = "ALLOWED",
    val shieldRuleLabel: String = "Verified Call",
    val isTelecomBacked: Boolean = false
)

object ActiveCallManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    private val _screenedCallEvents = MutableSharedFlow<CallSimulationResult>(extraBufferCapacity = 16)
    val screenedCallEvents: SharedFlow<CallSimulationResult> = _screenedCallEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var boundInCallService: InCallService? = null
    private var boundTelecomCall: Call? = null

    // Cached snapshot of user's rules & contacts for ClarityCallScreeningService
    @Volatile
    private var cachedSettings: ShieldSettings = ShieldSettings()

    @Volatile
    private var cachedContacts: List<ContactEntity> = emptyList()

    @Volatile
    private var cachedRules: List<FilterRuleEntity> = emptyList()

    private val telecomCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            updateFromTelecomState(call, state)
        }
    }

    fun updateCachedFilterState(
        settings: ShieldSettings,
        contacts: List<ContactEntity>,
        rules: List<FilterRuleEntity>
    ) {
        cachedSettings = settings
        cachedContacts = contacts
        cachedRules = rules
    }

    fun evaluateAndEmitScreenedCall(rawNumber: String, isPrivateNumber: Boolean): CallSimulationResult {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val result = CallFilterEngine.evaluateIncomingCall(
            rawNumber = rawNumber,
            isPrivateNumber = isPrivateNumber,
            simulatedHour = currentHour,
            settings = cachedSettings,
            contacts = cachedContacts,
            rules = cachedRules
        )
        _screenedCallEvents.tryEmit(result)
        return result
    }

    fun startInAppCall(
        rawNumber: String,
        contacts: List<ContactEntity>,
        direction: String = "OUTGOING",
        initialStatus: ActiveCallStatus = ActiveCallStatus.DIALING,
        shieldVerdict: String = "ALLOWED",
        shieldRuleLabel: String = "Outbound Dial"
    ) {
        val clean = rawNumber.trim()
        if (clean.isEmpty()) return
        val normalized = PhoneNumberFormatter.normalize(clean)
        val matchedContact = contacts.firstOrNull { it.normalizedNumber == normalized }
        val formatted = matchedContact?.phoneNumber ?: PhoneNumberFormatter.formatForDisplay(clean)
        val name = matchedContact?.name ?: if (direction == "INCOMING") "Incoming Caller" else "Outbound Number"
        val label = matchedContact?.label ?: "Mobile"
        val isVip = matchedContact?.isVip ?: false

        val initialRoute = when {
            cachedSettings.autoRouteBluetooth -> CallAudioRoute.BLUETOOTH
            cachedSettings.defaultAudioRoute == "SPEAKER" -> CallAudioRoute.SPEAKER
            cachedSettings.defaultAudioRoute == "EARPIECE" -> CallAudioRoute.EARPIECE
            else -> CallAudioRoute.BLUETOOTH
        }
        val btDeviceName = cachedSettings.preferredBluetoothDeviceName.ifBlank { "Studio Pro Hands-Free" }

        val session = ActiveCallSession(
            phoneNumber = clean,
            formattedNumber = formatted,
            contactName = name,
            contactLabel = label,
            isVip = isVip,
            direction = direction,
            status = initialStatus,
            startedAtMillis = System.currentTimeMillis(),
            elapsedSeconds = 0,
            isSpeakerOn = initialRoute == CallAudioRoute.SPEAKER,
            isBluetoothOn = initialRoute == CallAudioRoute.BLUETOOTH,
            audioRoute = initialRoute,
            activeBluetoothDeviceName = btDeviceName,
            shieldVerdict = if (isVip) "VIP_BYPASS" else shieldVerdict,
            shieldRuleLabel = if (isVip) "VIP Contact" else shieldRuleLabel,
            isTelecomBacked = false
        )
        _activeCall.value = session
        startCallLifecycleTimer()
    }

    fun bindTelecomCall(service: InCallService, call: Call) {
        boundInCallService = service
        boundTelecomCall?.unregisterCallback(telecomCallback)
        boundTelecomCall = call
        call.registerCallback(telecomCallback)

        val rawNumber = call.details?.handle?.schemeSpecificPart.orEmpty().ifBlank { "Unknown" }
        val normalized = PhoneNumberFormatter.normalize(rawNumber)
        val matchedContact = cachedContacts.firstOrNull { it.normalizedNumber == normalized }
        val formatted = matchedContact?.phoneNumber ?: PhoneNumberFormatter.formatForDisplay(rawNumber)
        val name = matchedContact?.name ?: call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: formatted
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            call.details?.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            @Suppress("DEPRECATION")
            call.state == Call.STATE_RINGING
        }

        val initialRoute = if (cachedSettings.autoRouteBluetooth) CallAudioRoute.BLUETOOTH else CallAudioRoute.EARPIECE
        val btDeviceName = cachedSettings.preferredBluetoothDeviceName.ifBlank { "Studio Pro Hands-Free" }

        val mappedStatus = mapTelecomState(call)
        _activeCall.value = ActiveCallSession(
            phoneNumber = rawNumber,
            formattedNumber = formatted,
            contactName = name,
            contactLabel = matchedContact?.label ?: "Telecom Line",
            isVip = matchedContact?.isVip ?: false,
            direction = if (isIncoming) "INCOMING" else "OUTGOING",
            status = mappedStatus,
            startedAtMillis = System.currentTimeMillis(),
            elapsedSeconds = 0,
            isSpeakerOn = initialRoute == CallAudioRoute.SPEAKER,
            isBluetoothOn = initialRoute == CallAudioRoute.BLUETOOTH,
            audioRoute = initialRoute,
            activeBluetoothDeviceName = btDeviceName,
            shieldVerdict = if (matchedContact?.isVip == true) "VIP_BYPASS" else "ALLOWED",
            shieldRuleLabel = if (isIncoming) "Screened Incoming Call" else "Default Dialer Call",
            isTelecomBacked = true
        )
        startCallLifecycleTimer()
    }

    fun unbindTelecomCall(call: Call) {
        if (boundTelecomCall == call) {
            call.unregisterCallback(telecomCallback)
            boundTelecomCall = null
            timerJob?.cancel()
            _activeCall.value = null
        }
    }

    private fun updateFromTelecomState(call: Call, state: Int) {
        val current = _activeCall.value ?: return
        val newStatus = when (state) {
            Call.STATE_RINGING -> ActiveCallStatus.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> ActiveCallStatus.DIALING
            Call.STATE_ACTIVE -> ActiveCallStatus.CONNECTED
            Call.STATE_HOLDING -> ActiveCallStatus.ON_HOLD
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> ActiveCallStatus.ENDED
            else -> current.status
        }
        if (newStatus == ActiveCallStatus.ENDED) {
            timerJob?.cancel()
            _activeCall.value = null
        } else {
            _activeCall.value = current.copy(
                status = newStatus,
                isOnHold = newStatus == ActiveCallStatus.ON_HOLD
            )
        }
    }

    private fun mapTelecomState(call: Call): ActiveCallStatus {
        @Suppress("DEPRECATION")
        val state = call.state
        return when (state) {
            Call.STATE_RINGING -> ActiveCallStatus.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> ActiveCallStatus.DIALING
            Call.STATE_ACTIVE -> ActiveCallStatus.CONNECTED
            Call.STATE_HOLDING -> ActiveCallStatus.ON_HOLD
            else -> ActiveCallStatus.DIALING
        }
    }

    private fun startCallLifecycleTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val initial = _activeCall.value ?: return@launch
            if (!initial.isTelecomBacked && initial.status == ActiveCallStatus.DIALING) {
                delay(1200L)
                val afterDial = _activeCall.value ?: return@launch
                if (afterDial.status == ActiveCallStatus.DIALING) {
                    _activeCall.value = afterDial.copy(status = ActiveCallStatus.CONNECTED)
                }
            }

            while (true) {
                delay(1000L)
                val curr = _activeCall.value ?: break
                if (curr.status == ActiveCallStatus.CONNECTED || curr.status == ActiveCallStatus.ON_HOLD) {
                    _activeCall.value = curr.copy(elapsedSeconds = curr.elapsedSeconds + 1)
                }
            }
        }
    }

    fun answerCall() {
        val curr = _activeCall.value ?: return
        try {
            @Suppress("DEPRECATION")
            boundTelecomCall?.answer(0)
        } catch (_: Exception) {
        }
        _activeCall.value = curr.copy(status = ActiveCallStatus.CONNECTED)
        startCallLifecycleTimer()
    }

    fun toggleMute() {
        val curr = _activeCall.value ?: return
        val nextMute = !curr.isMuted
        try {
            boundInCallService?.setMuted(nextMute)
        } catch (_: Exception) {
        }
        _activeCall.value = curr.copy(isMuted = nextMute)
    }

    fun toggleSpeaker() {
        val curr = _activeCall.value ?: return
        val nextRoute = if (curr.audioRoute == CallAudioRoute.SPEAKER) {
            CallAudioRoute.EARPIECE
        } else {
            CallAudioRoute.SPEAKER
        }
        setAudioRoute(nextRoute, curr.activeBluetoothDeviceName)
    }

    fun toggleBluetoothAudio(preferredDeviceName: String? = null) {
        val curr = _activeCall.value ?: return
        val nextRoute = if (curr.audioRoute == CallAudioRoute.BLUETOOTH) {
            CallAudioRoute.EARPIECE
        } else {
            CallAudioRoute.BLUETOOTH
        }
        setAudioRoute(
            route = nextRoute,
            bluetoothDeviceName = preferredDeviceName ?: curr.activeBluetoothDeviceName
        )
    }

    fun setAudioRoute(route: CallAudioRoute, bluetoothDeviceName: String? = null) {
        val curr = _activeCall.value ?: return
        try {
            val telecomRoute = when (route) {
                CallAudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                CallAudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
                CallAudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
            }
            boundInCallService?.setAudioRoute(telecomRoute)
        } catch (_: Exception) {
        }
        _activeCall.value = curr.copy(
            audioRoute = route,
            isSpeakerOn = route == CallAudioRoute.SPEAKER,
            isBluetoothOn = route == CallAudioRoute.BLUETOOTH,
            activeBluetoothDeviceName = bluetoothDeviceName ?: curr.activeBluetoothDeviceName
        )
    }

    fun toggleHold() {
        val curr = _activeCall.value ?: return
        val nextHold = !curr.isOnHold
        try {
            if (nextHold) {
                boundTelecomCall?.hold()
            } else {
                boundTelecomCall?.unhold()
            }
        } catch (_: Exception) {
        }
        _activeCall.value = curr.copy(
            isOnHold = nextHold,
            status = if (nextHold) ActiveCallStatus.ON_HOLD else ActiveCallStatus.CONNECTED
        )
    }

    fun sendDtmfDigit(digit: Char) {
        val curr = _activeCall.value ?: return
        try {
            boundTelecomCall?.playDtmfTone(digit)
            scope.launch {
                delay(140L)
                boundTelecomCall?.stopDtmfTone()
            }
        } catch (_: Exception) {
        }
        if (cachedSettings.keypadTonesEnabled) {
            playLocalDtmfTone(digit)
        }
        _activeCall.value = curr.copy(dtmfInput = (curr.dtmfInput + digit).takeLast(18))
    }

    fun playDialpadKeyToneIfEnabled(digitStr: String) {
        if (!cachedSettings.keypadTonesEnabled) return
        val ch = digitStr.firstOrNull() ?: return
        playLocalDtmfTone(ch)
    }

    fun endActiveCall(): ActiveCallSession? {
        val finished = _activeCall.value
        timerJob?.cancel()
        try {
            @Suppress("DEPRECATION")
            if (boundTelecomCall?.state == Call.STATE_RINGING) {
                boundTelecomCall?.reject(false, null)
            } else {
                boundTelecomCall?.disconnect()
            }
        } catch (_: Exception) {
        }
        boundTelecomCall = null
        _activeCall.value = null
        return finished
    }

    private fun playLocalDtmfTone(digit: Char) {
        try {
            val toneType = when (digit) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_DTMF_1
            }
            val generator = ToneGenerator(AudioManager.STREAM_DTMF, 65)
            generator.startTone(toneType, 110)
            scope.launch {
                delay(130L)
                generator.release()
            }
        } catch (_: Exception) {
        }
    }
}
