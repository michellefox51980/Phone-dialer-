package com.example.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Android Telecom CallScreeningService enabling ClarityDial to register for
 * RoleManager.ROLE_CALL_SCREENING and automatically block or silence incoming calls
 * based on user-configured Call Shield policies and filter rules.
 */
class ClarityCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        if (!isIncoming) {
            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .build()
            )
            return
        }

        val rawHandle = callDetails.handle?.schemeSpecificPart.orEmpty().trim()
        val isPrivateNumber = rawHandle.isEmpty()

        val result = ActiveCallManager.evaluateAndEmitScreenedCall(
            rawNumber = rawHandle,
            isPrivateNumber = isPrivateNumber
        )

        val response = when (result.verdict) {
            "BLOCKED" -> {
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
            }
            "SILENCED" -> {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
            else -> {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .build()
            }
        }

        respondToCall(callDetails, response)
    }
}
