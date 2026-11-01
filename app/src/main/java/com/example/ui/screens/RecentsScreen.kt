package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CallLogEntity
import com.example.data.ContactEntity
import com.example.domain.PhoneNumberFormatter
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.AmberSilence
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CrimsonBlock
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonOnContainer
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    BLOCKED("Blocked / Filtered")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    callLogs: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    selectedLogForDetail: CallLogEntity?,
    onSelectLogForDetail: (CallLogEntity) -> Unit,
    onDismissLogDetail: () -> Unit,
    onCallBack: (String) -> Unit,
    onMessageCaller: (String) -> Unit,
    onQuickBlockCaller: (String, String) -> Unit,
    onQuickSaveContact: (String, String) -> Unit,
    onDeleteCallLog: (String) -> Unit,
    onClearAllLogs: () -> Unit,
    onAddManualCallLog: (String, String, String, Int) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddCallDialog by remember { mutableStateOf(false) }

    val contactByNormalized = remember(contacts) {
        contacts.associateBy { it.normalizedNumber }
    }

    val filteredLogs by remember(callLogs, selectedFilter, searchQuery) {
        derivedStateOf {
            callLogs.filter { log ->
                val matchesCategory = when (selectedFilter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.MISSED -> log.direction == "MISSED" || log.filterVerdict == "BLOCKED" || log.filterVerdict == "SILENCED"
                    HistoryFilter.INCOMING -> log.direction == "INCOMING" || log.direction == "INCOMING_SIMULATED"
                    HistoryFilter.OUTGOING -> log.direction == "OUTGOING"
                    HistoryFilter.BLOCKED -> log.filterVerdict == "BLOCKED" || log.filterVerdict == "SILENCED"
                }
                val q = searchQuery.trim().lowercase()
                val matchesSearch = q.isEmpty() ||
                    log.contactName.lowercase().contains(q) ||
                    log.phoneNumber.lowercase().contains(q) ||
                    log.matchedRuleLabel.lowercase().contains(q)
                matchesCategory && matchesSearch
            }
        }
    }

    val incomingCount = remember(callLogs) {
        callLogs.count { it.direction == "INCOMING" || it.direction == "INCOMING_SIMULATED" }
    }
    val outgoingCount = remember(callLogs) {
        callLogs.count { it.direction == "OUTGOING" }
    }
    val missedOrBlockedCount = remember(callLogs) {
        callLogs.count { it.direction == "MISSED" || it.filterVerdict == "BLOCKED" || it.filterVerdict == "SILENCED" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            // Header & Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Call History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$incomingCount incoming · $outgoingCount outgoing · $missedOrBlockedCount missed/filtered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showAddCallDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("log_call_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Call", style = MaterialTheme.typography.labelMedium)
                    }

                    if (callLogs.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAllLogs,
                            modifier = Modifier.testTag("clear_call_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all call history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search caller name, number, or rule...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("call_history_search_input")
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HistoryFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}")
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching call history entries",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap any entry in Call History to view details or call back, or tap 'Log Call' to record a test call.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("call_history_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredLogs, key = { it.id.ifBlank { "${it.phoneNumber}_${it.timestamp}" } }) { log ->
                        val norm = PhoneNumberFormatter.normalize(log.phoneNumber)
                        val matchedContact = contactByNormalized[norm]
                        CallHistoryRowCard(
                            log = log,
                            resolvedContact = matchedContact,
                            onRowClick = { onSelectLogForDetail(log) },
                            onCallBackClick = {
                                if (norm.isNotEmpty()) {
                                    onCallBack(log.phoneNumber)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Call History Detail Sheet (Tap on an entry to view details or call back)
    if (selectedLogForDetail != null) {
        val log = selectedLogForDetail
        val norm = PhoneNumberFormatter.normalize(log.phoneNumber)
        val matchedContact = contactByNormalized[norm]
        val callerHistory = remember(callLogs, norm, log.phoneNumber) {
            callLogs.filter {
                val itemNorm = PhoneNumberFormatter.normalize(it.phoneNumber)
                (norm.isNotEmpty() && itemNorm == norm) || it.phoneNumber == log.phoneNumber
            }.sortedByDescending { it.timestamp }
        }

        BackHandler {
            onDismissLogDetail()
        }

        ModalBottomSheet(
            onDismissRequest = onDismissLogDetail,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            CallHistoryDetailSheetContent(
                log = log,
                matchedContact = matchedContact,
                callerHistory = callerHistory,
                onCallBack = {
                    onDismissLogDetail()
                    if (norm.isNotEmpty()) {
                        onCallBack(log.phoneNumber)
                    }
                },
                onMessageCaller = {
                    onDismissLogDetail()
                    if (norm.isNotEmpty()) {
                        onMessageCaller(log.phoneNumber)
                    }
                },
                onQuickBlock = {
                    onQuickBlockCaller(log.phoneNumber, matchedContact?.name ?: log.contactName)
                    onDismissLogDetail()
                },
                onQuickSaveContact = {
                    onQuickSaveContact(log.phoneNumber, log.contactName)
                    onDismissLogDetail()
                },
                onDeleteEntry = {
                    onDeleteCallLog(log.id)
                }
            )
        }
    }

    if (showAddCallDialog) {
        AddManualCallLogDialog(
            onDismiss = { showAddCallDialog = false },
            onConfirm = { number, name, direction, duration ->
                onAddManualCallLog(number, name, direction, duration)
                showAddCallDialog = false
            }
        )
    }
}

@Composable
private fun CallHistoryRowCard(
    log: CallLogEntity,
    resolvedContact: ContactEntity?,
    onRowClick: () -> Unit,
    onCallBackClick: () -> Unit
) {
    val displayTitle = resolvedContact?.name
        ?: log.contactName.ifBlank { log.phoneNumber }
    val displayNumber = resolvedContact?.phoneNumber ?: log.phoneNumber
    val callTypeMeta = resolveCallTypeMeta(log)
    val canCallBack = PhoneNumberFormatter.normalize(log.phoneNumber).isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onRowClick() }
            .testTag("call_history_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Directional / Call Type Status Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(callTypeMeta.badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = callTypeMeta.icon,
                    contentDescription = callTypeMeta.typeLabel,
                    tint = callTypeMeta.badgeFg,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (callTypeMeta.isMissedOrBlocked) {
                            CrimsonBlock
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (resolvedContact?.isVip == true || log.filterVerdict == "VIP_BYPASS") {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP Contact",
                            tint = CobaltAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = callTypeMeta.typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = callTypeMeta.badgeFg
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCallTimestamp(log.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (log.durationSeconds > 0 && log.filterVerdict != "BLOCKED" && log.direction != "MISSED") {
                        Text(
                            text = "· ${formatDurationSeconds(log.durationSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (log.filterVerdict == "BLOCKED" || log.filterVerdict == "SILENCED" || log.filterVerdict == "VIP_BYPASS") {
                        Surface(
                            color = callTypeMeta.badgeBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = log.matchedRuleLabel.ifBlank { log.filterVerdict },
                                style = MaterialTheme.typography.labelSmall,
                                color = callTypeMeta.badgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing Actions: Info Indicator + 1-Tap Call Back Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRowClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "View call details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                FilledTonalIconButton(
                    onClick = onCallBackClick,
                    enabled = canCallBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("call_back_button_${log.id}"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = EmeraldContainer,
                        contentColor = EmeraldOnContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call back ${displayTitle}"
                    )
                }
            }
        }
    }
}

@Composable
private fun CallHistoryDetailSheetContent(
    log: CallLogEntity,
    matchedContact: ContactEntity?,
    callerHistory: List<CallLogEntity>,
    onCallBack: () -> Unit,
    onMessageCaller: () -> Unit,
    onQuickBlock: () -> Unit,
    onQuickSaveContact: () -> Unit,
    onDeleteEntry: () -> Unit
) {
    val callTypeMeta = resolveCallTypeMeta(log)
    val displayTitle = matchedContact?.name ?: log.contactName.ifBlank { log.phoneNumber }
    val canCallOrText = PhoneNumberFormatter.normalize(log.phoneNumber).isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 28.dp)
    ) {
        // Caller Identity Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(callTypeMeta.badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = callTypeMeta.icon,
                    contentDescription = null,
                    tint = callTypeMeta.badgeFg,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (matchedContact != null) {
                    Text(
                        text = "${matchedContact.label} · ${matchedContact.organization.ifBlank { "Saved in Contacts" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Primary Action Buttons: Call Back & Message
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCallBack,
                enabled = canCallOrText,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("detail_sheet_call_back_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldCall,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call Back", fontWeight = FontWeight.SemiBold)
            }

            FilledTonalButton(
                onClick = onMessageCaller,
                enabled = canCallOrText,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("detail_sheet_message_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Text", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata Breakdown Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailInfoRow("Call Type", callTypeMeta.typeLabel)
                DetailInfoRow("Date & Time", formatFullDateTime(log.timestamp))
                DetailInfoRow(
                    "Duration",
                    if (log.durationSeconds > 0) formatDurationSeconds(log.durationSeconds) else "0 sec (Unanswered / Filtered)"
                )
                DetailInfoRow("Filter Verdict", log.filterVerdict)
                if (log.matchedRuleLabel.isNotBlank()) {
                    DetailInfoRow("Screening Rule", log.matchedRuleLabel)
                }
                if (log.autoReplyDispatched) {
                    DetailInfoRow("Auto-Reply SMS", "Dispatched automatically")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Secondary Actions: Save Contact, Block Number, Delete Log Entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (matchedContact == null && canCallOrText) {
                OutlinedButton(
                    onClick = onQuickSaveContact,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Contact")
                }
            }

            OutlinedButton(
                onClick = onQuickBlock,
                modifier = Modifier
                    .weight(1f)
                    .testTag("detail_sheet_block_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonBlock)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Block Number")
            }

            IconButton(
                onClick = onDeleteEntry,
                modifier = Modifier.testTag("detail_sheet_delete_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete call log entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (callerHistory.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Recent History with Caller (${callerHistory.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            callerHistory.take(4).forEach { item ->
                val itemMeta = resolveCallTypeMeta(item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = itemMeta.typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = itemMeta.badgeFg
                    )
                    Text(
                        text = formatFullDateTime(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddManualCallLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (phoneNumber: String, contactName: String, direction: String, durationSeconds: Int) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("(415) 555-0142") }
    var contactName by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf("INCOMING") }
    var durationText by remember { mutableStateOf("120") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Call Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Caller Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Call Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INCOMING", "OUTGOING", "MISSED").forEach { type ->
                        FilterChip(
                            selected = selectedDirection == type,
                            onClick = { selectedDirection = type },
                            label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                if (selectedDirection != "MISSED") {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Duration (seconds)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        onConfirm(
                            phoneNumber.trim(),
                            contactName.trim(),
                            selectedDirection,
                            durationText.toIntOrNull() ?: 60
                        )
                    }
                }
            ) {
                Text("Add to Call History")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class CallTypeVisualMeta(
    val icon: ImageVector,
    val typeLabel: String,
    val badgeBg: Color,
    val badgeFg: Color,
    val isMissedOrBlocked: Boolean
)

private fun resolveCallTypeMeta(log: CallLogEntity): CallTypeVisualMeta {
    return when {
        log.filterVerdict == "BLOCKED" -> CallTypeVisualMeta(
            icon = Icons.Default.Block,
            typeLabel = "Blocked Call",
            badgeBg = CrimsonContainer,
            badgeFg = CrimsonOnContainer,
            isMissedOrBlocked = true
        )
        log.filterVerdict == "SILENCED" -> CallTypeVisualMeta(
            icon = Icons.Default.NotificationsOff,
            typeLabel = "Silenced Call",
            badgeBg = AmberContainer,
            badgeFg = AmberOnContainer,
            isMissedOrBlocked = false
        )
        log.direction == "MISSED" -> CallTypeVisualMeta(
            icon = Icons.AutoMirrored.Filled.CallMissed,
            typeLabel = "Missed Call",
            badgeBg = CrimsonContainer,
            badgeFg = CrimsonOnContainer,
            isMissedOrBlocked = true
        )
        log.direction == "OUTGOING" -> CallTypeVisualMeta(
            icon = Icons.AutoMirrored.Filled.CallMade,
            typeLabel = "Outgoing Call",
            badgeBg = CobaltContainer,
            badgeFg = CobaltOnContainer,
            isMissedOrBlocked = false
        )
        log.filterVerdict == "VIP_BYPASS" -> CallTypeVisualMeta(
            icon = Icons.Default.VerifiedUser,
            typeLabel = "Incoming (VIP)",
            badgeBg = EmeraldContainer,
            badgeFg = EmeraldOnContainer,
            isMissedOrBlocked = false
        )
        else -> CallTypeVisualMeta(
            icon = Icons.AutoMirrored.Filled.CallReceived,
            typeLabel = "Incoming Call",
            badgeBg = EmeraldContainer,
            badgeFg = EmeraldOnContainer,
            isMissedOrBlocked = false
        )
    }
}

private fun formatCallTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFullDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy · h:mm:ss a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDurationSeconds(seconds: Int): String {
    val mins = seconds / 60
    val rem = seconds % 60
    return if (mins > 0) "${mins}m ${rem}s" else "${rem}s"
}
