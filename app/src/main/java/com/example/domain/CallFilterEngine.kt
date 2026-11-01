package com.example.domain

import com.example.data.CallSimulationResult
import com.example.data.ContactEntity
import com.example.data.EvaluationStep
import com.example.data.FilterRuleEntity
import com.example.data.ShieldSettings

object PhoneNumberFormatter {
    fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length == 11 && digits.startsWith("1")) {
            digits.substring(1)
        } else {
            digits
        }
    }

    fun formatForDisplay(raw: String): String {
        if (raw.isBlank()) return ""
        if (raw.equals("Private Number", ignoreCase = true)) return "Private Number"
        val hasPlus = raw.trim().startsWith("+")
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return raw

        return when {
            digits.length == 10 -> {
                val area = digits.substring(0, 3)
                val prefix = digits.substring(3, 6)
                val line = digits.substring(6, 10)
                "($area) $prefix-$line"
            }
            digits.length == 11 && digits.startsWith("1") -> {
                val area = digits.substring(1, 4)
                val prefix = digits.substring(4, 7)
                val line = digits.substring(7, 11)
                "+1 ($area) $prefix-$line"
            }
            digits.length in 4..7 -> {
                val prefix = digits.substring(0, 3)
                val rest = digits.substring(3)
                "$prefix-$rest"
            }
            else -> if (hasPlus) "+$digits" else raw
        }
    }

    private val t9Map = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun nameToT9Digits(name: String): String {
        val sb = StringBuilder()
        for (ch in name.lowercase()) {
            val mapped = t9Map[ch]
            if (mapped != null) sb.append(mapped)
        }
        return sb.toString()
    }

    fun matchesDialQuery(contact: ContactEntity, dialQuery: String): Boolean {
        val cleanQuery = dialQuery.filter { it.isDigit() }
        if (cleanQuery.isEmpty()) return false
        if (contact.normalizedNumber.contains(cleanQuery) ||
            contact.phoneNumber.filter { it.isDigit() }.contains(cleanQuery)
        ) {
            return true
        }
        val t9Name = nameToT9Digits(contact.name)
        return t9Name.contains(cleanQuery)
    }
}

object CallFilterEngine {

    fun isWithinQuietHours(hour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour == endHour) {
            true
        } else if (startHour < endHour) {
            hour in startHour until endHour
        } else {
            hour >= startHour || hour < endHour
        }
    }

    fun evaluateIncomingCall(
        rawNumber: String,
        isPrivateNumber: Boolean,
        simulatedHour: Int,
        settings: ShieldSettings,
        contacts: List<ContactEntity>,
        rules: List<FilterRuleEntity>
    ): CallSimulationResult {
        val steps = mutableListOf<EvaluationStep>()
        val normalized = if (isPrivateNumber) "" else PhoneNumberFormatter.normalize(rawNumber)
        val rawDigits = if (isPrivateNumber) "" else rawNumber.filter { it.isDigit() }

        val matchedContact = if (!isPrivateNumber && normalized.isNotEmpty()) {
            contacts.firstOrNull { it.normalizedNumber == normalized }
        } else null

        val displayContactName = when {
            isPrivateNumber -> "Private / Hidden Caller"
            matchedContact != null -> matchedContact.name
            else -> "Unknown Caller"
        }

        // Step 1: Master Shield Check
        if (!settings.masterShieldEnabled) {
            steps.add(
                EvaluationStep(
                    stepName = "Master Call Shield",
                    passedOrTriggered = true,
                    detail = "Master Call Shield is paused — all incoming calls ring through."
                )
            )
            return CallSimulationResult(
                phoneNumber = if (isPrivateNumber) "Private Number" else PhoneNumberFormatter.formatForDisplay(rawNumber),
                contactName = displayContactName,
                isPrivateNumber = isPrivateNumber,
                simulatedHour = simulatedHour,
                verdict = "ALLOWED",
                matchedRuleLabel = "Shield Paused",
                matchedRuleId = null,
                suggestedAutoReply = "",
                evaluationSteps = steps
            )
        }
        steps.add(
            EvaluationStep(
                stepName = "Master Call Shield",
                passedOrTriggered = false,
                detail = "Active — inspecting caller identity and filter rules."
            )
        )

        // Step 2: VIP Contact Bypass
        if (matchedContact != null && matchedContact.isVip && settings.vipBypassQuietHours) {
            steps.add(
                EvaluationStep(
                    stepName = "VIP Contact Bypass",
                    passedOrTriggered = true,
                    detail = "${matchedContact.name} is starred as a VIP Contact — bypassing all filters and Quiet Hours."
                )
            )
            return CallSimulationResult(
                phoneNumber = PhoneNumberFormatter.formatForDisplay(rawNumber),
                contactName = matchedContact.name,
                isPrivateNumber = false,
                simulatedHour = simulatedHour,
                verdict = "VIP_BYPASS",
                matchedRuleLabel = "VIP Starred Contact (${matchedContact.name})",
                matchedRuleId = null,
                suggestedAutoReply = "",
                evaluationSteps = steps
            )
        } else {
            steps.add(
                EvaluationStep(
                    stepName = "VIP Contact Check",
                    passedOrTriggered = false,
                    detail = if (matchedContact != null) {
                        "${matchedContact.name} is in Contacts (standard priority)."
                    } else {
                        "Number is not in VIP Starred Contacts."
                    }
                )
            )
        }

        // Step 3: Private / Withheld Caller ID Check
        if (isPrivateNumber || rawDigits.isEmpty()) {
            if (settings.blockPrivateNumbers) {
                steps.add(
                    EvaluationStep(
                        stepName = "Private / Hidden Caller Protection",
                        passedOrTriggered = true,
                        detail = "Caller ID is hidden/withheld and 'Block Private Numbers' is enabled."
                    )
                )
                return CallSimulationResult(
                    phoneNumber = "Private Number",
                    contactName = "Hidden Caller ID",
                    isPrivateNumber = true,
                    simulatedHour = simulatedHour,
                    verdict = "BLOCKED",
                    matchedRuleLabel = "Block Private & Hidden Numbers",
                    matchedRuleId = null,
                    suggestedAutoReply = "",
                    evaluationSteps = steps
                )
            } else {
                steps.add(
                    EvaluationStep(
                        stepName = "Private / Hidden Caller Protection",
                        passedOrTriggered = false,
                        detail = "Private numbers are currently allowed."
                    )
                )
            }
        }

        // Step 4: Specific Numbers & Custom Pattern Rules
        val enabledRules = rules.filter { it.isEnabled }
        for (rule in enabledRules) {
            val cleanPattern = rule.pattern.trim()
            val patternDigits = cleanPattern.filter { it.isDigit() }
            val matched = when (rule.matchType) {
                "PREFIX" -> {
                    patternDigits.isNotEmpty() && (
                        rawDigits.startsWith(patternDigits) ||
                            normalized.startsWith(patternDigits)
                        )
                }
                "EXACT" -> {
                    patternDigits.isNotEmpty() && (
                        rawDigits == patternDigits ||
                            normalized == PhoneNumberFormatter.normalize(patternDigits)
                        )
                }
                "CONTAINS" -> {
                    patternDigits.isNotEmpty() && (
                        rawDigits.contains(patternDigits) ||
                            normalized.contains(patternDigits)
                        )
                }
                "REGEX" -> {
                    try {
                        val regex = Regex(cleanPattern)
                        regex.containsMatchIn(rawDigits) || regex.containsMatchIn(normalized)
                    } catch (_: Exception) {
                        false
                    }
                }
                else -> false
            }

            if (matched) {
                val verdict = when (rule.action) {
                    "ALLOW" -> "ALLOWED"
                    "SILENCE" -> "SILENCED"
                    else -> "BLOCKED"
                }
                steps.add(
                    EvaluationStep(
                        stepName = "Rule Match: ${rule.label}",
                        passedOrTriggered = true,
                        detail = "Matched ${rule.matchType} '${rule.pattern}' -> Action: $verdict"
                    )
                )
                val reply = rule.autoReplyText.ifBlank {
                    if (settings.defaultAutoReplyEnabled) settings.defaultAutoReplyText else ""
                }
                return CallSimulationResult(
                    phoneNumber = PhoneNumberFormatter.formatForDisplay(rawNumber),
                    contactName = displayContactName,
                    isPrivateNumber = false,
                    simulatedHour = simulatedHour,
                    verdict = verdict,
                    matchedRuleLabel = rule.label,
                    matchedRuleId = rule.id,
                    suggestedAutoReply = if (verdict == "ALLOWED") "" else reply,
                    evaluationSteps = steps
                )
            }
        }
        steps.add(
            EvaluationStep(
                stepName = "Specific Numbers & Pattern Rules (${enabledRules.size} active)",
                passedOrTriggered = false,
                detail = "Caller did not match any specific number or pattern rules."
            )
        )

        // Step 5: Quiet Hours Schedule
        if (settings.quietHoursEnabled) {
            val inQuietWindow = isWithinQuietHours(
                hour = simulatedHour,
                startHour = settings.quietStartHour,
                endHour = settings.quietEndHour
            )
            if (inQuietWindow) {
                steps.add(
                    EvaluationStep(
                        stepName = "Quiet Hours Schedule (${formatHour(settings.quietStartHour)} – ${formatHour(settings.quietEndHour)})",
                        passedOrTriggered = true,
                        detail = "Call arrived at ${formatHour(simulatedHour)} during active Quiet Hours — silencing ringer."
                    )
                )
                return CallSimulationResult(
                    phoneNumber = PhoneNumberFormatter.formatForDisplay(rawNumber),
                    contactName = displayContactName,
                    isPrivateNumber = isPrivateNumber,
                    simulatedHour = simulatedHour,
                    verdict = "SILENCED",
                    matchedRuleLabel = "Quiet Hours (${formatHour(settings.quietStartHour)}–${formatHour(settings.quietEndHour)})",
                    matchedRuleId = null,
                    suggestedAutoReply = if (settings.defaultAutoReplyEnabled) {
                        "I have Quiet Hours enabled right now. Please send a text message or I will return your call later."
                    } else "",
                    evaluationSteps = steps
                )
            } else {
                steps.add(
                    EvaluationStep(
                        stepName = "Quiet Hours Schedule",
                        passedOrTriggered = false,
                        detail = "Call time (${formatHour(simulatedHour)}) is outside Quiet Hours."
                    )
                )
            }
        }

        // Step 6: Numbers Not in Contacts (Block or Silence Unknown Callers)
        if (matchedContact == null) {
            if (settings.blockUnknownCallers) {
                steps.add(
                    EvaluationStep(
                        stepName = "Block Numbers Not in Contacts",
                        passedOrTriggered = true,
                        detail = "Caller is not in your Contacts directory and 'Block Numbers Not in Contacts' is enabled."
                    )
                )
                return CallSimulationResult(
                    phoneNumber = PhoneNumberFormatter.formatForDisplay(rawNumber),
                    contactName = displayContactName,
                    isPrivateNumber = isPrivateNumber,
                    simulatedHour = simulatedHour,
                    verdict = "BLOCKED",
                    matchedRuleLabel = "Not in Contacts (Blocked)",
                    matchedRuleId = null,
                    suggestedAutoReply = if (settings.defaultAutoReplyEnabled) settings.defaultAutoReplyText else "",
                    evaluationSteps = steps
                )
            } else if (settings.silenceUnknownCallers) {
                steps.add(
                    EvaluationStep(
                        stepName = "Silence Unknown Callers",
                        passedOrTriggered = true,
                        detail = "Caller is not in your Contacts directory and 'Silence Unknown Callers' is enabled."
                    )
                )
                return CallSimulationResult(
                    phoneNumber = PhoneNumberFormatter.formatForDisplay(rawNumber),
                    contactName = displayContactName,
                    isPrivateNumber = isPrivateNumber,
                    simulatedHour = simulatedHour,
                    verdict = "SILENCED",
                    matchedRuleLabel = "Unknown Caller (Silenced)",
                    matchedRuleId = null,
                    suggestedAutoReply = if (settings.defaultAutoReplyEnabled) settings.defaultAutoReplyText else "",
                    evaluationSteps = steps
                )
            }
        }

        steps.add(
            EvaluationStep(
                stepName = "Final Screening Verdict",
                passedOrTriggered = true,
                detail = "All screening checks passed — ringing device normally."
            )
        )
        return CallSimulationResult(
            phoneNumber = if (isPrivateNumber) "Private Number" else PhoneNumberFormatter.formatForDisplay(rawNumber),
            contactName = displayContactName,
            isPrivateNumber = isPrivateNumber,
            simulatedHour = simulatedHour,
            verdict = "ALLOWED",
            matchedRuleLabel = if (matchedContact != null) "Saved Contact (${matchedContact.name})" else "Passed All Filters",
            matchedRuleId = null,
            suggestedAutoReply = "",
            evaluationSteps = steps
        )
    }

    fun formatHour(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        val period = if (h < 12) "AM" else "PM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$displayHour:00 $period"
    }
}
