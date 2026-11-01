package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.example.MainActivity

/**
 * Android Telecom InCallService enabling ClarityDial to register for RoleManager.ROLE_DIALER
 * (Default Phone App) and present the In-App Active Call UI when placing or receiving calls.
 */
class ClarityInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        ActiveCallManager.bindTelecomCall(this, call)

        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_FROM_INCALL_SERVICE, true)
            }
            startActivity(launchIntent)
        } catch (_: Exception) {
        }
    }

    override fun onCallRemoved(call: Call) {
        ActiveCallManager.unbindTelecomCall(call)
        super.onCallRemoved(call)
    }

    companion object {
        const val EXTRA_FROM_INCALL_SERVICE = "extra_from_incall_service"
    }
}
