package com.example.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class CallAudioRoute(val label: String) {
    EARPIECE("Phone Earpiece"),
    SPEAKER("Speakerphone"),
    BLUETOOTH("Bluetooth Audio")
}

data class BluetoothAudioDevice(
    val id: String,
    val name: String,
    val address: String,
    val category: String, // HEADSET, CAR_KIT, EARBUDS
    val isConnected: Boolean,
    val isPreferred: Boolean = false,
    val isHardwareBonded: Boolean = false
)

object BluetoothAudioController {

    private val defaultProfiles = mutableListOf(
        BluetoothAudioDevice(
            id = "bt_studio_buds",
            name = "Studio Pro Hands-Free",
            address = "AA:BB:CC:11:22:01",
            category = "EARBUDS",
            isConnected = true,
            isPreferred = true,
            isHardwareBonded = false
        ),
        BluetoothAudioDevice(
            id = "bt_car_handsfree",
            name = "Car Hands-Free Audio",
            address = "AA:BB:CC:11:22:02",
            category = "CAR_KIT",
            isConnected = false,
            isPreferred = false,
            isHardwareBonded = false
        ),
        BluetoothAudioDevice(
            id = "bt_office_headset",
            name = "Clarity Boom Headset",
            address = "AA:BB:CC:11:22:03",
            category = "HEADSET",
            isConnected = false,
            isPreferred = false,
            isHardwareBonded = false
        )
    )

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBluetoothAdapterEnabled(context: Context): Boolean {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            btManager?.adapter?.isEnabled ?: true
        } catch (_: Exception) {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun queryPairedAudioDevices(
        context: Context,
        preferredDeviceName: String
    ): List<BluetoothAudioDevice> {
        val discovered = mutableListOf<BluetoothAudioDevice>()

        // 1. Inspect AudioManager output devices for active Bluetooth SCO/BLE endpoints
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val outputDevices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            for (info in outputDevices) {
                val isBt = info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                        info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                        ))
                if (isBt) {
                    val productName = info.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio Output"
                    if (discovered.none { it.name.equals(productName, ignoreCase = true) }) {
                        discovered.add(
                            BluetoothAudioDevice(
                                id = "audio_dev_${info.id}",
                                name = productName,
                                address = "Audio Endpoint #${info.id}",
                                category = "HEADSET",
                                isConnected = true,
                                isPreferred = productName.equals(preferredDeviceName, ignoreCase = true),
                                isHardwareBonded = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Inspect BluetoothAdapter bonded devices if permission is granted
        if (hasBluetoothConnectPermission(context)) {
            try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bonded = btManager?.adapter?.bondedDevices.orEmpty()
                for (device in bonded) {
                    val devName = device.name?.takeIf { it.isNotBlank() } ?: "Paired Bluetooth Device"
                    val devAddr = device.address ?: "00:00:00:00:00:00"
                    val majorClass = device.bluetoothClass?.majorDeviceClass
                    val category = when (majorClass) {
                        BluetoothClass.Device.Major.AUDIO_VIDEO -> "HEADSET"
                        else -> "CAR_KIT"
                    }
                    if (discovered.none { it.address == devAddr || it.name.equals(devName, ignoreCase = true) }) {
                        discovered.add(
                            BluetoothAudioDevice(
                                id = "bonded_$devAddr",
                                name = devName,
                                address = devAddr,
                                category = category,
                                isConnected = devName.equals(preferredDeviceName, ignoreCase = true),
                                isPreferred = devName.equals(preferredDeviceName, ignoreCase = true),
                                isHardwareBonded = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Merge with user's configured hands-free profiles so Bluetooth management works on emulators too
        defaultProfiles.forEach { profile ->
            if (discovered.none { it.name.equals(profile.name, ignoreCase = true) }) {
                val isPref = profile.name.equals(preferredDeviceName, ignoreCase = true)
                discovered.add(
                    profile.copy(
                        isPreferred = isPref,
                        isConnected = profile.isConnected || isPref
                    )
                )
            }
        }

        return discovered.sortedWith(
            compareByDescending<BluetoothAudioDevice> { it.isPreferred }
                .thenByDescending { it.isConnected }
                .thenBy { it.name.lowercase() }
        )
    }

    fun addCustomBluetoothProfile(name: String, category: String): BluetoothAudioDevice {
        val cleanName = name.trim().ifBlank { "Custom Bluetooth Headset" }
        val existing = defaultProfiles.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
        if (existing != null) return existing

        val suffix = (10..99).random()
        val created = BluetoothAudioDevice(
            id = "bt_custom_${System.currentTimeMillis()}",
            name = cleanName,
            address = "AA:BB:CC:44:55:$suffix",
            category = category,
            isConnected = true,
            isPreferred = true,
            isHardwareBonded = false
        )
        defaultProfiles.add(0, created)
        return created
    }

    fun toggleProfileConnection(deviceId: String): List<BluetoothAudioDevice> {
        val idx = defaultProfiles.indexOfFirst { it.id == deviceId }
        if (idx >= 0) {
            val current = defaultProfiles[idx]
            defaultProfiles[idx] = current.copy(isConnected = !current.isConnected)
        }
        return defaultProfiles.toList()
    }

    fun applyHardwareAudioRoute(
        context: Context,
        route: CallAudioRoute
    ) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            when (route) {
                CallAudioRoute.BLUETOOTH -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                }
                CallAudioRoute.SPEAKER -> {
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                }
                CallAudioRoute.EARPIECE -> {
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                }
            }
        } catch (_: Exception) {
        }
    }

    fun openSystemBluetoothSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
