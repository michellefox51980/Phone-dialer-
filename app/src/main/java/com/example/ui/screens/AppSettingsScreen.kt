package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ShieldSettings
import com.example.telecom.BluetoothAudioController
import com.example.telecom.BluetoothAudioDevice
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

@Composable
fun AppSettingsScreen(
    userDisplayName: String,
    settings: ShieldSettings,
    roleState: DefaultDialerRoleState,
    pairedBluetoothDevices: List<BluetoothAudioDevice>,
    contactsCount: Int,
    callLogsCount: Int,
    blockedCallsCount: Int,
    filterRulesCount: Int,
    messagesCount: Int,
    onBack: () -> Unit,
    onUpdateSettings: ((ShieldSettings) -> ShieldSettings) -> Unit,
    onRequestDefaultDialerRole: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onOpenSystemDefaultApps: () -> Unit,
    onSimulateIncomingRingingCall: () -> Unit,
    onRefreshBluetoothDevices: () -> Unit,
    onAddBluetoothProfile: (String, String) -> Unit,
    onToggleBluetoothDeviceConnection: (String) -> Unit,
    onSelectPreferredBluetoothDevice: (String) -> Unit,
    onOpenSystemBluetoothSettings: () -> Unit,
    onExportAllContactsVcf: () -> Unit,
    onRestoreStarterData: () -> Unit,
    onClearCallHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var hasBtPermission by remember {
        mutableStateOf(BluetoothAudioController.hasBluetoothConnectPermission(context))
    }
    var showAddBtDialog by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPermission = granted
        onRefreshBluetoothDevices()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "App Settings",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Default Dialer roles, Bluetooth audio, keypad & Cloud sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // SECTION 1: DEFAULT PHONE & CALL SCREENING ROLES
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                                    text = "Default Phone & Call Screening",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Android RoleManager registration & Active Call Screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ROLE_DIALER row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Default Phone App (ROLE_DIALER)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = if (roleState.isDefaultDialer) EmeraldContainer else AmberContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = if (roleState.isDefaultDialer) "Default" else "Companion",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (roleState.isDefaultDialer) EmeraldOnContainer else AmberOnContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Handles tel: links, ACTION_DIAL, and ClarityInCallService",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onRequestDefaultDialerRole,
                                    modifier = Modifier.testTag("settings_request_dialer_role_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(if (roleState.isDefaultDialer) "Verified" else "Set Default")
                                }
                            }
                        }

                        // ROLE_CALL_SCREENING row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Call Screening (ROLE_CALL_SCREENING)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = if (roleState.isDefaultCallScreener) EmeraldContainer else CobaltContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = if (roleState.isDefaultCallScreener) "Active" else "Ready",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (roleState.isDefaultCallScreener) EmeraldOnContainer else CobaltOnContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Binds ClarityCallScreeningService for automated call blocking",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = onRequestCallScreeningRole,
                                    modifier = Modifier.testTag("settings_request_screening_role_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(if (roleState.isDefaultCallScreener) "Verified" else "Enable")
                                }
                            }
                        }

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
                                    text = "Launch ClarityDial's full-screen Active Call UI with Bluetooth routing and System Dialer fallback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.preferInAppCallUi,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings { it.copy(preferInAppCallUi = enabled) }
                                },
                                modifier = Modifier.testTag("settings_in_app_call_ui_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSimulateIncomingRingingCall,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_test_ringing_call_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RingVolume,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Incoming Call")
                            }
                            OutlinedButton(
                                onClick = onOpenSystemDefaultApps,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_system_default_apps_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsApplications,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Default Apps")
                            }
                        }
                    }
                }
            }

            // SECTION 2: BLUETOOTH & CALL AUDIO ROUTING
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(CobaltContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothAudio,
                                        contentDescription = null,
                                        tint = CobaltOnContainer
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Bluetooth & Call Audio",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Hands-free routing & paired audio device manager",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onRefreshBluetoothDevices,
                                modifier = Modifier.testTag("refresh_bluetooth_devices_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Bluetooth devices"
                                )
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBtPermission) {
                            Surface(
                                color = AmberContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Bluetooth Device Discovery Permission",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AmberOnContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Grant BLUETOOTH_CONNECT to inspect hardware-bonded headsets and car kits.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AmberOnContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                        },
                                        modifier = Modifier.testTag("grant_bluetooth_permission_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Allow")
                                    }
                                }
                            }
                        }

                        // Auto-route calls to Bluetooth switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Route Calls to Bluetooth",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Automatically route call audio to your preferred Bluetooth headset or car hands-free kit when connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoRouteBluetooth,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings { it.copy(autoRouteBluetooth = enabled) }
                                },
                                modifier = Modifier.testTag("auto_route_bluetooth_switch")
                            )
                        }

                        // Default Audio Route Selector
                        Text(
                            text = "Default Call Audio Output",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val routes = listOf(
                                Triple("BLUETOOTH", "Bluetooth", Icons.Default.BluetoothConnected),
                                Triple("EARPIECE", "Earpiece", Icons.Default.Hearing),
                                Triple("SPEAKER", "Speaker", Icons.AutoMirrored.Filled.VolumeUp)
                            )
                            routes.forEach { (routeKey, label, icon) ->
                                FilterChip(
                                    selected = settings.defaultAudioRoute == routeKey,
                                    onClick = {
                                        onUpdateSettings { it.copy(defaultAudioRoute = routeKey) }
                                    },
                                    label = { Text(label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("default_audio_route_$routeKey")
                                )
                            }
                        }

                        HorizontalDivider()

                        // Paired Bluetooth Audio Devices List
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paired Bluetooth Audio Devices (${pairedBluetoothDevices.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(
                                onClick = { showAddBtDialog = true },
                                modifier = Modifier.testTag("add_bluetooth_device_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Hands-Free")
                            }
                        }

                        pairedBluetoothDevices.forEach { device ->
                            val isPreferred = settings.preferredBluetoothDeviceName.equals(device.name, ignoreCase = true)
                            Surface(
                                color = if (isPreferred) {
                                    CobaltContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPreferredBluetoothDevice(device.name) }
                                    .testTag("bt_device_card_${device.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when (device.category) {
                                                "CAR_KIT" -> Icons.Default.DirectionsCar
                                                else -> Icons.Default.Headphones
                                            },
                                            contentDescription = null,
                                            tint = if (isPreferred) CobaltAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = device.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (isPreferred) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = CobaltAccent,
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = "Preferred",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${device.category} · ${device.address} · ${if (device.isConnected) "Connected" else "Paired"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onToggleBluetoothDeviceConnection(device.id)
                                            onSelectPreferredBluetoothDevice(device.name)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (isPreferred) "Active" else "Use Audio",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenSystemBluetoothSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_system_bluetooth_settings_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open System Bluetooth Settings")
                        }
                    }
                }
            }

            // SECTION 3: KEYPAD TONES, HAPTICS & THEME APPEARANCE
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CobaltContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = CobaltOnContainer
                                )
                            }
                            Column {
                                Text(
                                    text = "Keypad, Haptics & Appearance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dialpad acoustic tones, tactile feedback, and Daylight/Dark Slate theme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dialpad DTMF Audio Tones",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Play audible DTMF acoustic tones when pressing numbers on the keypad",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.keypadTonesEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings { it.copy(keypadTonesEnabled = enabled) }
                                },
                                modifier = Modifier.testTag("settings_keypad_tones_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tactile Haptic Feedback",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Vibrate gently on dialpad presses and call actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.hapticsEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings { it.copy(hapticsEnabled = enabled) }
                                },
                                modifier = Modifier.testTag("settings_haptics_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dark Slate Appearance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Switch between Minimalist Daylight Slate and Dark Slate theme",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.darkModeEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings { it.copy(darkModeEnabled = enabled) }
                                },
                                modifier = Modifier.testTag("settings_dark_mode_switch")
                            )
                        }
                    }
                }
            }

            // SECTION 4: CLOUD ACCOUNT SYNC & DATA MANAGEMENT
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = EmeraldOnContainer
                                )
                            }
                            Column {
                                Text(
                                    text = "Cloud Account Sync & Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Signed in as $userDisplayName · Real-time Cloud Firestore",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SyncStatColumn("Contacts", contactsCount.toString())
                                SyncStatColumn("Call Logs", callLogsCount.toString())
                                SyncStatColumn("Blocked", blockedCallsCount.toString())
                                SyncStatColumn("Rules", filterRulesCount.toString())
                                SyncStatColumn("Messages", messagesCount.toString())
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExportAllContactsVcf,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_export_vcf_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export .VCF")
                            }

                            OutlinedButton(
                                onClick = onRestoreStarterData,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_restore_starter_data_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Demo")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClearCallHistory,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_clear_call_logs_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Call Log")
                            }

                            Button(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_sign_out_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonBlock,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddBtDialog) {
        AddBluetoothProfileDialog(
            onDismiss = { showAddBtDialog = false },
            onSave = { name, category ->
                onAddBluetoothProfile(name, category)
                showAddBtDialog = false
            }
        )
    }
}

@Composable
private fun SyncStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddBluetoothProfileDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("HEADSET") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bluetooth Audio Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name (e.g., Pixel Buds Pro, Car Audio)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_bt_profile_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HEADSET", "EARBUDS", "CAR_KIT").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.replace("_", " ")) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), category)
                    }
                },
                modifier = Modifier.testTag("save_bt_profile_button")
            ) {
                Text("Pair & Set Preferred")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
