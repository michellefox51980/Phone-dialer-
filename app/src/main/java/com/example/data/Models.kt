package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ContactEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val normalizedNumber: String = "",
    val label: String = "Mobile", // Mobile, Work, Home, Main, Medical
    val email: String = "",
    val organization: String = "",
    @get:PropertyName("isVip") @set:PropertyName("isVip")
    var isVip: Boolean = false,
    val avatarColorIndex: Int = 0,
    val notes: String = "",
    val importSource: String = "MANUAL", // MANUAL, VCF_FILE, CSV_FILE, SYSTEM_PICKER, BATCH_PASTE, STARTER
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FilterRuleEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val pattern: String = "",
    val matchType: String = "PREFIX", // PREFIX, CONTAINS, EXACT, REGEX
    val action: String = "BLOCK", // BLOCK, SILENCE, ALLOW
    @get:PropertyName("isEnabled") @set:PropertyName("isEnabled")
    var isEnabled: Boolean = true,
    val blockedCount: Int = 0,
    val autoReplyText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallLogEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phoneNumber: String = "",
    val contactName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val direction: String = "OUTGOING", // INCOMING, OUTGOING, MISSED, INCOMING_SIMULATED
    val filterVerdict: String = "ALLOWED", // ALLOWED, BLOCKED, SILENCED, VIP_BYPASS
    val matchedRuleLabel: String = "",
    val autoReplyDispatched: Boolean = false,
    val durationSeconds: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MessageEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phoneNumber: String = "",
    val normalizedNumber: String = "",
    val contactName: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isOutgoing") @set:PropertyName("isOutgoing")
    var isOutgoing: Boolean = true,
    @get:PropertyName("isAutoReply") @set:PropertyName("isAutoReply")
    var isAutoReply: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReplyTemplateEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "FILTER_REPLY", // FILTER_REPLY, BUSY, CUSTOM
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShieldSettings(
    val userId: String = "",
    val masterShieldEnabled: Boolean = true,
    val blockUnknownCallers: Boolean = false,
    val silenceUnknownCallers: Boolean = false,
    val blockPrivateNumbers: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val vipBypassQuietHours: Boolean = true,
    val defaultAutoReplyEnabled: Boolean = true,
    val defaultAutoReplyText: String = "Hi, I'm currently unavailable to take your call. Please send a text message with details.",
    val darkModeEnabled: Boolean = false,
    val keypadTonesEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val preferInAppCallUi: Boolean = true,
    val autoRouteBluetooth: Boolean = true,
    val defaultAudioRoute: String = "BLUETOOTH", // BLUETOOTH, EARPIECE, SPEAKER
    val preferredBluetoothDeviceName: String = "Studio Pro Hands-Free",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ImportedContactCandidate(
    val tempId: String,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val label: String = "Mobile",
    val email: String = "",
    val organization: String = "",
    val notes: String = "",
    val isVip: Boolean = false,
    val isSelected: Boolean = true,
    val isDuplicate: Boolean = false,
    val existingContactId: String? = null,
    val importSource: String = "VCF_FILE"
)

data class EvaluationStep(
    val stepName: String,
    val passedOrTriggered: Boolean,
    val detail: String
)

data class CallSimulationResult(
    val phoneNumber: String,
    val contactName: String,
    val isPrivateNumber: Boolean,
    val simulatedHour: Int,
    val verdict: String, // ALLOWED, BLOCKED, SILENCED, VIP_BYPASS
    val matchedRuleLabel: String,
    val matchedRuleId: String? = null,
    val suggestedAutoReply: String,
    val evaluationSteps: List<EvaluationStep>
)

data class MessageThreadSummary(
    val normalizedNumber: String,
    val displayNumber: String,
    val contactName: String,
    val isVip: Boolean,
    val avatarColorIndex: Int,
    val lastMessageBody: String,
    val lastTimestamp: Long,
    val messageCount: Int,
    val hasAutoReply: Boolean
)
