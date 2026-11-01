package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.telecom.ActiveCallSession
import com.example.telecom.ActiveCallStatus
import com.example.telecom.BluetoothAudioDevice
import com.example.telecom.CallAudioRoute
import com.example.telecom.DefaultDialerRoleState
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CrimsonBlock
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import java.util.Locale

private val DtmfRows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('*', '0', '#')
)

@Composable
fun ActiveCallOverlay(
    session: ActiveCallSession,
    pairedBluetoothDevices: List<BluetoothAudioDevice>,
    onAnswerCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onSelectAudioRoute: (CallAudioRoute, String?) -> Unit,
    onToggleHold: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    onHandOffToSystemDialer: (String) -> Unit,
    onQuickSmsReply: (String, String) -> Unit
) {
    var showDtmfKeypad by remember { mutableStateOf(false) }
    var showBluetoothPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Keep active call screen visible until ended or handed off */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler {
            when {
                showDtmfKeypad -> showDtmfKeypad = false
                showBluetoothPicker -> showBluetoothPicker = false
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B1120),
            contentColor = Color(0xFFF8FAFC)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Shield & Bluetooth Status Pill
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (session.isVip) AmberContainer else EmeraldContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (session.isVip) Icons.Default.Star else Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (session.isVip) AmberOnContainer else EmeraldOnContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = session.shieldRuleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (session.isVip) AmberOnContainer else EmeraldOnContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                color = if (session.audioRoute == CallAudioRoute.BLUETOOTH) CobaltAccent else Color(0xFF1E293B),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { showBluetoothPicker = !showBluetoothPicker }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothConnected,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = when (session.audioRoute) {
                                            CallAudioRoute.BLUETOOTH -> session.activeBluetoothDeviceName
                                            CallAudioRoute.SPEAKER -> "Speakerphone"
                                            CallAudioRoute.EARPIECE -> "Phone Earpiece"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Caller Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(2.dp, CobaltAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = session.contactName.take(2).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = session.contactName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${session.formattedNumber} · ${session.contactLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val statusText = when (session.status) {
                            ActiveCallStatus.RINGING -> "Incoming Call..."
                            ActiveCallStatus.DIALING -> "Dialing..."
                            ActiveCallStatus.CONNECTED -> "Connected · ${formatCallTimer(session.elapsedSeconds)}"
                            ActiveCallStatus.ON_HOLD -> "Call on Hold · ${formatCallTimer(session.elapsedSeconds)}"
                            ActiveCallStatus.ENDED -> "Call Ended"
                        }

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (session.status == ActiveCallStatus.ON_HOLD) Color(0xFFFBBF24) else Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (session.dtmfInput.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Keypad tones: ${session.dtmfInput}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }

                    // Middle Controls or Incoming Ringing Actions
                    if (session.status == ActiveCallStatus.RINGING) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Quick SMS Decline",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onQuickSmsReply(
                                            session.phoneNumber,
                                            "I'm in a meeting right now—I'll call you back shortly."
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Message,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("In a meeting", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onQuickSmsReply(
                                            session.phoneNumber,
                                            "Can't talk right now, please send me a text."
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Message,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Text me instead", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 3-Way Audio Route Selector Strip (Bluetooth / Speaker / Earpiece)
                            Surface(
                                color = Color(0xFF151F32),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Audio Output Route",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = if (showBluetoothPicker) "Hide Devices" else "Switch Bluetooth Device",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF60A5FA),
                                            modifier = Modifier
                                                .clickable { showBluetoothPicker = !showBluetoothPicker }
                                                .testTag("open_in_call_bt_devices_button")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AudioRoutePill(
                                            label = "Bluetooth",
                                            icon = Icons.Default.BluetoothAudio,
                                            selected = session.audioRoute == CallAudioRoute.BLUETOOTH,
                                            onClick = {
                                                onSelectAudioRoute(
                                                    CallAudioRoute.BLUETOOTH,
                                                    session.activeBluetoothDeviceName
                                                )
                                            },
                                            testTag = "toggle_bluetooth_button",
                                            modifier = Modifier.weight(1f)
                                        )
                                        AudioRoutePill(
                                            label = "Speaker",
                                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                                            selected = session.audioRoute == CallAudioRoute.SPEAKER,
                                            onClick = {
                                                onSelectAudioRoute(CallAudioRoute.SPEAKER, null)
                                            },
                                            testTag = "toggle_speaker_button",
                                            modifier = Modifier.weight(1f)
                                        )
                                        AudioRoutePill(
                                            label = "Earpiece",
                                            icon = Icons.Default.Hearing,
                                            selected = session.audioRoute == CallAudioRoute.EARPIECE,
                                            onClick = {
                                                onSelectAudioRoute(CallAudioRoute.EARPIECE, null)
                                            },
                                            testTag = "toggle_earpiece_button",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    AnimatedVisibility(visible = showBluetoothPicker) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            pairedBluetoothDevices.forEach { btDevice ->
                                                val isActiveBt = session.audioRoute == CallAudioRoute.BLUETOOTH &&
                                                    session.activeBluetoothDeviceName.equals(btDevice.name, ignoreCase = true)
                                                Surface(
                                                    color = if (isActiveBt) CobaltAccent.copy(alpha = 0.35f) else Color(0xFF1E293B),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            onSelectAudioRoute(CallAudioRoute.BLUETOOTH, btDevice.name)
                                                            showBluetoothPicker = false
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Default.Bluetooth,
                                                                contentDescription = null,
                                                                tint = Color(0xFF93C5FD),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column {
                                                                Text(
                                                                    text = btDevice.name,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Text(
                                                                    text = "${btDevice.category} · ${btDevice.address}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color(0xFF94A3B8)
                                                                )
                                                            }
                                                        }
                                                        if (isActiveBt) {
                                                            Text(
                                                                text = "Active",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color(0xFF34D399),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = !showDtmfKeypad) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    InCallControlTile(
                                        label = if (session.isMuted) "Muted" else "Mute",
                                        icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        active = session.isMuted,
                                        onClick = onToggleMute,
                                        testTag = "toggle_mute_button",
                                        modifier = Modifier.weight(1f)
                                    )
                                    InCallControlTile(
                                        label = if (session.isOnHold) "Resume" else "Hold",
                                        icon = if (session.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        active = session.isOnHold,
                                        onClick = onToggleHold,
                                        testTag = "toggle_hold_button",
                                        modifier = Modifier.weight(1f)
                                    )
                                    InCallControlTile(
                                        label = "Keypad",
                                        icon = Icons.Default.Dialpad,
                                        active = showDtmfKeypad,
                                        onClick = { showDtmfKeypad = true },
                                        testTag = "toggle_dtmf_keypad_button",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showDtmfKeypad) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "In-Call DTMF Keypad",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFF94A3B8)
                                        )
                                        FilledTonalButton(
                                            onClick = { showDtmfKeypad = false },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Hide Keypad")
                                        }
                                    }

                                    DtmfRows.forEach { rowChars ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowChars.forEach { digit ->
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable { onSendDtmf(digit) }
                                                        .testTag("in_call_dtmf_$digit"),
                                                    color = Color(0xFF1E293B),
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = digit.toString(),
                                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
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

                    // Bottom Action Bar: System Dialer Fallback + Answer/End Call
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onHandOffToSystemDialer(session.phoneNumber) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("system_dialer_handoff_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF93C5FD))
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hand Off to System Dialer")
                        }

                        if (session.status == ActiveCallStatus.RINGING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = onEndCall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("decline_incoming_call_button"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CrimsonBlock,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.CallEnd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Decline", style = MaterialTheme.typography.titleMedium)
                                }

                                Button(
                                    onClick = onAnswerCall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("answer_incoming_call_button"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldCall,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Answer", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            Button(
                                onClick = onEndCall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .testTag("end_active_call_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonBlock,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "End Call",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioRoutePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (selected) CobaltAccent else Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InCallControlTile(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (active) CobaltAccent else Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultDialerRoleSheet(
    roleState: DefaultDialerRoleState,
    preferInAppCallUi: Boolean,
    onDismiss: () -> Unit,
    onRequestDefaultDialerRole: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onOpenSystemDefaultApps: () -> Unit,
    onTogglePreferInAppCallUi: (Boolean) -> Unit,
    onSimulateIncomingRingingCall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CobaltContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneCallback,
                        contentDescription = null,
                        tint = CobaltOnContainer
                    )
                }
                Column {
                    Text(
                        text = "Default Phone & Call Screening Roles",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Register ClarityDial with Android Telecom & RoleManager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card 1: ROLE_DIALER
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (roleState.isDefaultDialer) {
                        EmeraldContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (roleState.isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.Call,
                                contentDescription = null,
                                tint = if (roleState.isDefaultDialer) EmeraldCall else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Default Phone App (ROLE_DIALER)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = if (roleState.isDefaultDialer) EmeraldContainer else AmberContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (roleState.isDefaultDialer) "Active Default" else "Companion Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (roleState.isDefaultDialer) EmeraldOnContainer else AmberOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Handles tel: links, ACTION_DIAL intents, and binds ClarityInCallService for active calls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onRequestDefaultDialerRole,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_role_dialer_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (roleState.isDefaultDialer) {
                                "Re-Verify Default Phone Role"
                            } else {
                                "Set as Default Phone App"
                            }
                        )
                    }
                }
            }

            // Card 2: ROLE_CALL_SCREENING
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (roleState.isDefaultCallScreener) {
                        EmeraldContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (roleState.isDefaultCallScreener) EmeraldCall else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call Screening (ROLE_CALL_SCREENING)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = if (roleState.isDefaultCallScreener) EmeraldContainer else CobaltContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (roleState.isDefaultCallScreener) "Active Screener" else "Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (roleState.isDefaultCallScreener) EmeraldOnContainer else CobaltOnContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Binds ClarityCallScreeningService to block or silence unknown and non-contact callers automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = onRequestCallScreeningRole,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_role_screening_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (roleState.isDefaultCallScreener) {
                                "Re-Verify Call Screening Role"
                            } else {
                                "Set as Default Call Screener"
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Active Call Experience Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "In-App Active Call UI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Open ClarityDial's full-screen Active Call UI with Bluetooth routing, Mute, Speaker, Hold, DTMF keypad, and System Dialer fallback",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = preferInAppCallUi,
                    onCheckedChange = onTogglePreferInAppCallUi,
                    modifier = Modifier.testTag("prefer_in_app_call_ui_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulateIncomingRingingCall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_ringing_call_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RingVolume,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Ringing UI")
                }

                OutlinedButton(
                    onClick = onOpenSystemDefaultApps,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_system_default_apps_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsApplications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("System Settings")
                }
            }
        }
    }
}

private fun formatCallTimer(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
