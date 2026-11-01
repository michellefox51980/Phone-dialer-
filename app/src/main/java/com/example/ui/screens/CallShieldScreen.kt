package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CallLogEntity
import com.example.data.CallSimulationResult
import com.example.data.ContactEntity
import com.example.data.FilterRuleEntity
import com.example.data.ShieldSettings
import com.example.domain.CallFilterEngine
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
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

private enum class ShieldSubTab(val label: String) {
    RULES("Filter Rules & Settings"),
    BLOCKED_LOG("Blocked Calls Log"),
    SIMULATOR("Test Call Simulator")
}

@Composable
fun CallShieldScreen(
    settings: ShieldSettings,
    rules: List<FilterRuleEntity>,
    callLogs: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    lastSimulationResult: CallSimulationResult?,
    onUpdateSettings: ((ShieldSettings) -> ShieldSettings) -> Unit,
    onSaveFilterRule: (FilterRuleEntity?, String, String, String, String, String) -> Unit,
    onToggleFilterRule: (FilterRuleEntity) -> Unit,
    onDeleteFilterRule: (String) -> Unit,
    onRunSimulation: (String, Boolean, Int) -> Unit,
    onDispatchAutoReplySms: (String, String) -> Unit,
    onCallBackFromLog: (String) -> Unit,
    onDeleteCallLog: (String) -> Unit
) {
    var activeSubTab by remember { mutableStateOf(ShieldSubTab.RULES) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    val blockedAndSilencedLogs by remember(callLogs) {
        derivedStateOf {
            callLogs.filter { it.filterVerdict == "BLOCKED" || it.filterVerdict == "SILENCED" }
                .sortedByDescending { it.timestamp }
        }
    }

    val totalBlockedCount = remember(rules, blockedAndSilencedLogs) {
        val fromRules = rules.sumOf { it.blockedCount }
        maxOf(fromRules, blockedAndSilencedLogs.size)
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
            // Sub-Navigation Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ShieldSubTab.entries) { sub ->
                    val labelText = if (sub == ShieldSubTab.BLOCKED_LOG && blockedAndSilencedLogs.isNotEmpty()) {
                        "${sub.label} (${blockedAndSilencedLogs.size})"
                    } else {
                        sub.label
                    }
                    FilterChip(
                        selected = activeSubTab == sub,
                        onClick = { activeSubTab = sub },
                        label = { Text(labelText) },
                        modifier = Modifier.testTag("shield_subtab_${sub.name.lowercase()}")
                    )
                }
            }

            when (activeSubTab) {
                ShieldSubTab.RULES -> {
                    ShieldRulesAndSettingsContent(
                        settings = settings,
                        rules = rules,
                        totalBlockedCount = totalBlockedCount,
                        onUpdateSettings = onUpdateSettings,
                        onOpenAddRule = { showAddRuleDialog = true },
                        onToggleFilterRule = onToggleFilterRule,
                        onDeleteFilterRule = onDeleteFilterRule,
                        onSwitchToSimulator = { activeSubTab = ShieldSubTab.SIMULATOR },
                        onSwitchToBlockedLog = { activeSubTab = ShieldSubTab.BLOCKED_LOG }
                    )
                }
                ShieldSubTab.BLOCKED_LOG -> {
                    BlockedCallsLogView(
                        blockedLogs = blockedAndSilencedLogs,
                        onAddRuleClick = { showAddRuleDialog = true },
                        onCallBack = onCallBackFromLog,
                        onDeleteLog = onDeleteCallLog
                    )
                }
                ShieldSubTab.SIMULATOR -> {
                    LiveCallScreeningSimulatorContent(
                        contacts = contacts,
                        lastResult = lastSimulationResult,
                        onRunSimulation = onRunSimulation,
                        onDispatchAutoReplySms = onDispatchAutoReplySms
                    )
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddFilterRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onSave = { label, pattern, matchType, action, autoReply ->
                onSaveFilterRule(null, label, pattern, matchType, action, autoReply)
                showAddRuleDialog = false
            }
        )
    }
}

@Composable
private fun ShieldRulesAndSettingsContent(
    settings: ShieldSettings,
    rules: List<FilterRuleEntity>,
    totalBlockedCount: Int,
    onUpdateSettings: ((ShieldSettings) -> ShieldSettings) -> Unit,
    onOpenAddRule: () -> Unit,
    onToggleFilterRule: (FilterRuleEntity) -> Unit,
    onDeleteFilterRule: (String) -> Unit,
    onSwitchToSimulator: () -> Unit,
    onSwitchToBlockedLog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("call_shield_rules_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Architectural Hero Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (settings.masterShieldEnabled) EmeraldContainer else CrimsonContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (settings.masterShieldEnabled) EmeraldOnContainer else CrimsonOnContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (settings.masterShieldEnabled) {
                                        "Automated Call Shield Active"
                                    } else {
                                        "Call Shield Paused"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${rules.count { it.isEnabled }} active rules · $totalBlockedCount calls filtered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.masterShieldEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(masterShieldEnabled = enabled) }
                            },
                            modifier = Modifier.testTag("master_shield_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_shield_hero_art),
                        contentDescription = "Automated Call Filtering diagram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onSwitchToBlockedLog,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_blocked_calls_log_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Blocked Log")
                        }

                        Button(
                            onClick = onSwitchToSimulator,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_simulator_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Filter")
                        }
                    }
                }
            }
        }

        // Global Call Filtering Policies (Non-Contacts, Unknown/Private Callers, Quiet Hours)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Automated Caller Policies",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 1. Block all numbers not in contacts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block All Numbers Not in Contacts",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Automatically reject any caller not saved in your Contacts directory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.blockUnknownCallers,
                            onCheckedChange = { checked ->
                                onUpdateSettings {
                                    it.copy(
                                        blockUnknownCallers = checked,
                                        silenceUnknownCallers = if (checked) false else it.silenceUnknownCallers
                                    )
                                }
                            },
                            modifier = Modifier.testTag("switch_block_non_contacts")
                        )
                    }

                    HorizontalDivider()

                    // 2. Silence calls from unknown callers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Silence Calls from Unknown Callers",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Mute ringer for unrecognized numbers while still logging them in Call History",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.silenceUnknownCallers,
                            onCheckedChange = { checked ->
                                onUpdateSettings {
                                    it.copy(
                                        silenceUnknownCallers = checked,
                                        blockUnknownCallers = if (checked) false else it.blockUnknownCallers
                                    )
                                }
                            },
                            modifier = Modifier.testTag("switch_silence_unknown_callers")
                        )
                    }

                    HorizontalDivider()

                    // 3. Block Private / Hidden Caller IDs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block Private & Withheld Caller IDs",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Reject anonymous calls with hidden or restricted caller ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.blockPrivateNumbers,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(blockPrivateNumbers = checked) }
                            },
                            modifier = Modifier.testTag("switch_block_private_numbers")
                        )
                    }

                    HorizontalDivider()

                    // 4. Quiet Hours & VIP Bypass
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quiet Hours (${CallFilterEngine.formatHour(settings.quietStartHour)} – ${CallFilterEngine.formatHour(settings.quietEndHour)})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Silence calls overnight except starred VIP Contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.quietHoursEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(quietHoursEnabled = checked) }
                            },
                            modifier = Modifier.testTag("switch_quiet_hours")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Starred VIP Contact Bypass",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Allow VIP contacts to ring through Quiet Hours and strict filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.vipBypassQuietHours,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(vipBypassQuietHours = checked) }
                            }
                        )
                    }

                    HorizontalDivider()

                    // 5. Dark Mode Appearance Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark Slate Theme",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Switch between Minimalist Daylight Slate and Dark Obsidian",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.darkModeEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(darkModeEnabled = checked) }
                            },
                            modifier = Modifier.testTag("switch_dark_mode")
                        )
                    }
                }
            }
        }

        // Specific Numbers & Custom Filter Rules Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Specific Numbers & Pattern Rules (${rules.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Block or silence specific phone numbers, prefixes, or patterns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenAddRule,
                    modifier = Modifier.testTag("add_filter_rule_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Rule")
                }
            }
        }

        items(rules, key = { it.id.ifBlank { it.pattern } }) { rule ->
            FilterRuleRowCard(
                rule = rule,
                onToggle = { onToggleFilterRule(rule) },
                onDelete = {
                    if (rule.id.isNotBlank()) onDeleteFilterRule(rule.id)
                }
            )
        }
    }
}

@Composable
private fun FilterRuleRowCard(
    rule: FilterRuleEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeBg = when (rule.action) {
        "BLOCK" -> CrimsonContainer
        "SILENCE" -> AmberContainer
        else -> EmeraldContainer
    }
    val badgeFg = when (rule.action) {
        "BLOCK" -> CrimsonOnContainer
        "SILENCE" -> AmberOnContainer
        else -> EmeraldOnContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .testTag("filter_rule_card_${rule.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = rule.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = rule.action,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${rule.matchType}: ${rule.pattern} · ${rule.blockedCount} calls matched",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (rule.autoReplyText.isNotBlank()) {
                    Text(
                        text = "Auto-SMS: \"${rule.autoReplyText}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() }
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BlockedCallsLogView(
    blockedLogs: List<CallLogEntity>,
    onAddRuleClick: () -> Unit,
    onCallBack: (String) -> Unit,
    onDeleteLog: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Blocked & Silenced Calls Log",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${blockedLogs.size} calls intercepted by automated call filtering",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onAddRuleClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Block Number")
            }
        }

        if (blockedLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No blocked or silenced calls logged yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Use the 'Test Call Simulator' tab to test how incoming calls are filtered and logged here.",
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
                    .testTag("blocked_calls_log_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(blockedLogs, key = { it.id.ifBlank { "${it.phoneNumber}_${it.timestamp}" } }) { log ->
                    val isBlocked = log.filterVerdict == "BLOCKED"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isBlocked) CrimsonContainer else AmberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBlocked) Icons.Default.Block else Icons.Default.NotificationsOff,
                                    contentDescription = log.filterVerdict,
                                    tint = if (isBlocked) CrimsonOnContainer else AmberOnContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.contactName} (${log.phoneNumber})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isBlocked) CrimsonBlock else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Rule: ${log.matchedRuleLabel.ifBlank { log.filterVerdict }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onCallBack(log.phoneNumber) }) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call back",
                                    tint = EmeraldCall
                                )
                            }
                            if (log.id.isNotBlank()) {
                                IconButton(onClick = { onDeleteLog(log.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete log entry",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCallScreeningSimulatorContent(
    contacts: List<ContactEntity>,
    lastResult: CallSimulationResult?,
    onRunSimulation: (String, Boolean, Int) -> Unit,
    onDispatchAutoReplySms: (String, String) -> Unit
) {
    var testNumber by remember { mutableStateOf("+1 (800) 555-0199") }
    var isPrivateNumber by remember { mutableStateOf(false) }
    var simulatedHour by remember { mutableIntStateOf(23) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Interactive Incoming Call Simulator",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Test any phone number and arrival time against your active automated call filter rules. Each simulation logs to Call History and Blocked Calls Log.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick Preset Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            AssistChip(
                                onClick = {
                                    isPrivateNumber = false
                                    testNumber = "+1 (800) 555-0199"
                                },
                                label = { Text("800 Telemarketer") }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    isPrivateNumber = true
                                    testNumber = "Private Number"
                                },
                                label = { Text("Private / Hidden ID") }
                            )
                        }
                        item {
                            val vip = contacts.firstOrNull { it.isVip }
                            AssistChip(
                                onClick = {
                                    isPrivateNumber = false
                                    testNumber = vip?.phoneNumber ?: "(415) 890-2314"
                                },
                                label = { Text("VIP Contact (${vip?.name ?: "Elena"})") }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    isPrivateNumber = false
                                    testNumber = "(415) 992-4012"
                                },
                                label = { Text("Unknown Non-Contact") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = if (isPrivateNumber) "Private / Withheld Caller ID" else testNumber,
                        onValueChange = {
                            isPrivateNumber = false
                            testNumber = it
                        },
                        enabled = !isPrivateNumber,
                        label = { Text("Simulated Incoming Phone Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulator_phone_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulated Time: ${CallFilterEngine.formatHour(simulatedHour)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Slider(
                        value = simulatedHour.toFloat(),
                        onValueChange = { simulatedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Button(
                        onClick = { onRunSimulation(testNumber, isPrivateNumber, simulatedHour) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("run_call_simulation_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Call Filter Simulation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (lastResult != null) {
            item {
                val (badgeBg, badgeFg, icon) = when (lastResult.verdict) {
                    "BLOCKED" -> Triple(CrimsonContainer, CrimsonOnContainer, Icons.Default.Block)
                    "SILENCED" -> Triple(AmberContainer, AmberOnContainer, Icons.Default.NotificationsOff)
                    "VIP_BYPASS" -> Triple(CobaltContainer, CobaltOnContainer, Icons.Default.VerifiedUser)
                    else -> Triple(EmeraldContainer, EmeraldOnContainer, Icons.Default.CheckCircle)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .testTag("simulation_result_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(badgeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = badgeFg)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Verdict: ${lastResult.verdict}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = badgeFg
                                )
                                Text(
                                    text = "${lastResult.contactName} (${lastResult.phoneNumber}) · ${lastResult.matchedRuleLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Step-by-Step Rule Pipeline Trace",
                            style = MaterialTheme.typography.labelLarge
                        )

                        lastResult.evaluationSteps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.width(22.dp)
                                )
                                Column {
                                    Text(
                                        text = step.stepName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = step.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (lastResult.suggestedAutoReply.isNotBlank() && !lastResult.isPrivateNumber) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    onDispatchAutoReplySms(
                                        lastResult.phoneNumber,
                                        lastResult.suggestedAutoReply
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Auto-Reply via SMS")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFilterRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf("EXACT") }
    var action by remember { mutableStateOf("BLOCK") }
    var autoReply by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Call Filter Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Rule Name (e.g. Specific Spam Caller)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_label_input")
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Phone Number or Prefix (e.g. 4155550199 or 1800)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_pattern_input")
                )

                Text("Match Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("EXACT", "PREFIX", "CONTAINS", "REGEX")) { type ->
                        FilterChip(
                            selected = matchType == type,
                            onClick = { matchType = type },
                            label = {
                                Text(
                                    when (type) {
                                        "EXACT" -> "Specific Number"
                                        "PREFIX" -> "Starts With"
                                        "CONTAINS" -> "Contains"
                                        else -> "Regex"
                                    }
                                )
                            }
                        )
                    }
                }

                Text("Filter Action", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BLOCK", "SILENCE", "ALLOW").forEach { act ->
                        FilterChip(
                            selected = action == act,
                            onClick = { action = act },
                            label = { Text(act) }
                        )
                    }
                }

                OutlinedTextField(
                    value = autoReply,
                    onValueChange = { autoReply = it },
                    label = { Text("Optional Auto-Reply SMS") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pattern.isNotBlank()) {
                        onSave(
                            label.ifBlank { "Rule: $pattern" },
                            pattern,
                            matchType,
                            action,
                            autoReply
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_save_rule_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
