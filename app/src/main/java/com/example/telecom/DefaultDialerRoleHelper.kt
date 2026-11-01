package com.example.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager

data class DefaultDialerRoleState(
    val isDefaultDialer: Boolean = false,
    val isDialerRoleAvailable: Boolean = true,
    val isDefaultCallScreener: Boolean = false,
    val isCallScreeningRoleAvailable: Boolean = true
)

object DefaultDialerRoleHelper {

    fun queryRoleState(context: Context): DefaultDialerRoleState {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                val dialerAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) ?: true
                val dialerHeld = roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) ?: false
                val screeningAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) ?: true
                val screeningHeld = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
                DefaultDialerRoleState(
                    isDefaultDialer = dialerHeld,
                    isDialerRoleAvailable = dialerAvailable,
                    isDefaultCallScreener = screeningHeld,
                    isCallScreeningRoleAvailable = screeningAvailable
                )
            } else {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val isDefault = telecomManager?.defaultDialerPackage == context.packageName
                DefaultDialerRoleState(
                    isDefaultDialer = isDefault,
                    isDialerRoleAvailable = true,
                    isDefaultCallScreener = false,
                    isCallScreeningRoleAvailable = false
                )
            }
        } catch (_: Exception) {
            DefaultDialerRoleState()
        }
    }

    fun createDialerRoleRequestIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                } else {
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                }
            } else {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(
                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                        context.packageName
                    )
                }
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
    }

    fun createCallScreeningRoleRequestIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                } else {
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                }
            } else {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
    }

    fun openSystemDefaultAppsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun extractPhoneNumberFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW || action == Intent.ACTION_CALL) {
            val data: Uri? = intent.data
            if (data != null && (data.scheme.equals("tel", ignoreCase = true) || data.scheme.equals("voicemail", ignoreCase = true))) {
                val raw = Uri.decode(data.schemeSpecificPart.orEmpty())
                    .filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                if (raw.isNotEmpty()) {
                    return raw
                }
            }
        }
        return null
    }
}
