package com.example.domain

import com.example.data.ContactEntity
import com.example.data.ImportedContactCandidate
import java.util.UUID

object ContactImportParser {

    /**
     * Automatically detects whether input content is vCard (.vcf) or CSV/TSV lines
     * and parses all valid contact candidates with deduplication flags against existing contacts.
     */
    fun parseContent(
        rawText: String,
        sourceTag: String,
        existingContacts: List<ContactEntity>
    ): List<ImportedContactCandidate> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val rawCandidates = if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            parseVCard(trimmed, sourceTag)
        } else {
            parseCsvOrLines(trimmed, sourceTag)
        }

        return markDuplicates(rawCandidates, existingContacts)
    }

    fun markDuplicates(
        candidates: List<ImportedContactCandidate>,
        existingContacts: List<ContactEntity>
    ): List<ImportedContactCandidate> {
        val existingByNormalized = existingContacts
            .filter { it.normalizedNumber.isNotEmpty() }
            .associateBy { it.normalizedNumber }

        val seenInBatch = mutableSetOf<String>()
        return candidates.map { candidate ->
            val norm = candidate.normalizedNumber
            val existing = existingByNormalized[norm]
            val duplicateInBatch = norm.isNotEmpty() && !seenInBatch.add(norm)
            val isDupe = existing != null || duplicateInBatch
            candidate.copy(
                isDuplicate = isDupe,
                existingContactId = existing?.id,
                isSelected = !duplicateInBatch // Keep unique candidates selected by default
            )
        }
    }

    private fun parseVCard(
        vcfText: String,
        sourceTag: String
    ): List<ImportedContactCandidate> {
        val results = mutableListOf<ImportedContactCandidate>()
        // Unfold RFC 6350 folded lines (CRLF + space/tab)
        val unfolded = vcfText.replace(Regex("\r?\n[ \t]"), "")
        val blocks = unfolded.split(Regex("(?i)BEGIN:VCARD"))

        for (block in blocks) {
            if (!block.contains("END:VCARD", ignoreCase = true)) continue
            val lines = block.lines()
            var fullName = ""
            var structuredName = ""
            var phone = ""
            var label = "Mobile"
            var email = ""
            var org = ""
            var note = ""
            var isVip = false

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("END:VCARD", ignoreCase = true)) continue

                val colonIndex = line.indexOf(':')
                if (colonIndex <= 0) continue

                val propAndParams = line.substring(0, colonIndex)
                val rawValue = line.substring(colonIndex + 1).trim()
                if (rawValue.isEmpty()) continue

                val propParts = propAndParams.split(';')
                val propName = propParts.first().uppercase()
                val paramsUpper = propParts.drop(1).joinToString(";").uppercase()

                val value = decodeQuotedPrintableIfNeeded(rawValue, paramsUpper)

                when (propName) {
                    "FN" -> fullName = value
                    "N" -> {
                        val parts = value.split(';').map { it.trim() }
                        val family = parts.getOrNull(0).orEmpty()
                        val given = parts.getOrNull(1).orEmpty()
                        structuredName = listOf(given, family).filter { it.isNotEmpty() }.joinToString(" ")
                    }
                    "TEL" -> {
                        if (phone.isEmpty() || paramsUpper.contains("PREF") || paramsUpper.contains("CELL")) {
                            phone = value
                            label = when {
                                paramsUpper.contains("WORK") -> "Work"
                                paramsUpper.contains("HOME") -> "Home"
                                paramsUpper.contains("MAIN") -> "Main"
                                else -> "Mobile"
                            }
                        }
                    }
                    "EMAIL" -> if (email.isEmpty()) email = value
                    "ORG" -> org = value.replace(';', ' ').trim()
                    "NOTE" -> note = value.replace("\\n", " ").trim()
                    "CATEGORIES" -> {
                        if (value.contains("VIP", ignoreCase = true) ||
                            value.contains("STARRED", ignoreCase = true)
                        ) {
                            isVip = true
                        }
                    }
                }
            }

            val resolvedName = fullName.ifBlank { structuredName }.ifBlank { "Imported Contact" }
            val normalized = PhoneNumberFormatter.normalize(phone)
            if (normalized.isNotEmpty()) {
                results.add(
                    ImportedContactCandidate(
                        tempId = UUID.randomUUID().toString(),
                        name = resolvedName,
                        phoneNumber = PhoneNumberFormatter.formatForDisplay(phone),
                        normalizedNumber = normalized,
                        label = label,
                        email = email,
                        organization = org,
                        notes = note,
                        isVip = isVip,
                        importSource = sourceTag
                    )
                )
            }
        }
        return results
    }

    private fun decodeQuotedPrintableIfNeeded(value: String, paramsUpper: String): String {
        if (!paramsUpper.contains("QUOTED-PRINTABLE")) {
            return value.replace("\\,", ",").replace("\\;", ";")
        }
        return try {
            val sb = StringBuilder()
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '=' && i + 2 < value.length) {
                    val hex = value.substring(i + 1, i + 3)
                    val code = hex.toIntOrNull(16)
                    if (code != null) {
                        sb.append(code.toChar())
                        i += 3
                        continue
                    }
                }
                sb.append(c)
                i++
            }
            sb.toString()
        } catch (_: Exception) {
            value
        }
    }

    private fun parseCsvOrLines(
        csvText: String,
        sourceTag: String
    ): List<ImportedContactCandidate> {
        val results = mutableListOf<ImportedContactCandidate>()
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        // Detect optional header row
        val firstLineLower = lines.first().lowercase()
        val hasHeader = firstLineLower.contains("name") ||
            firstLineLower.contains("phone") ||
            firstLineLower.contains("number")

        val dataLines = if (hasHeader && lines.size > 1) lines.drop(1) else lines

        for (line in dataLines) {
            val columns = splitCsvLine(line)
            if (columns.isEmpty()) continue

            if (columns.size == 1) {
                // Single column: try "Name - (415) 555-1234" or just phone
                val single = columns[0]
                val digits = PhoneNumberFormatter.normalize(single)
                if (digits.length >= 7) {
                    val namePart = single.takeWhile { !it.isDigit() && it != '(' && it != '+' }
                        .trim(' ', '-', ':', ',')
                        .ifBlank { "Contact ${digits.takeLast(4)}" }
                    results.add(
                        ImportedContactCandidate(
                            tempId = UUID.randomUUID().toString(),
                            name = namePart,
                            phoneNumber = PhoneNumberFormatter.formatForDisplay(digits),
                            normalizedNumber = digits,
                            importSource = sourceTag
                        )
                    )
                }
                continue
            }

            // Find which column holds the phone number (at least 7 digits)
            val phoneColIndex = columns.indexOfFirst { col ->
                col.count { it.isDigit() } >= 7
            }
            if (phoneColIndex == -1) continue

            val rawPhone = columns[phoneColIndex]
            val normalized = PhoneNumberFormatter.normalize(rawPhone)
            if (normalized.isEmpty()) continue

            val nameCol = columns.getOrNull(if (phoneColIndex == 0) 1 else 0)
                ?.ifBlank { "Contact ${normalized.takeLast(4)}" }
                ?: "Contact ${normalized.takeLast(4)}"

            val remainingCols = columns.filterIndexed { index, _ ->
                index != phoneColIndex && index != (if (phoneColIndex == 0) 1 else 0)
            }

            var label = "Mobile"
            var email = ""
            var org = ""
            var isVip = false

            for (extra in remainingCols) {
                val clean = extra.trim()
                when {
                    clean.contains("@") -> email = clean
                    clean.equals("VIP", ignoreCase = true) ||
                        clean.equals("true", ignoreCase = true) ||
                        clean.equals("starred", ignoreCase = true) -> isVip = true
                    clean.equals("Work", ignoreCase = true) ||
                        clean.equals("Home", ignoreCase = true) ||
                        clean.equals("Mobile", ignoreCase = true) ||
                        clean.equals("Medical", ignoreCase = true) ||
                        clean.equals("Main", ignoreCase = true) -> label = clean.replaceFirstChar { it.uppercase() }
                    org.isEmpty() && clean.length > 2 -> org = clean
                }
            }

            results.add(
                ImportedContactCandidate(
                    tempId = UUID.randomUUID().toString(),
                    name = nameCol,
                    phoneNumber = PhoneNumberFormatter.formatForDisplay(rawPhone),
                    normalizedNumber = normalized,
                    label = label,
                    email = email,
                    organization = org,
                    isVip = isVip,
                    importSource = sourceTag
                )
            )
        }
        return results
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                (ch == ',' || ch == ';' || ch == '\t') && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    fun exportToVCard(contacts: List<ContactEntity>): String {
        val sb = StringBuilder()
        for (contact in contacts) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")
            sb.appendLine("FN:${contact.name}")
            sb.appendLine("TEL;TYPE=${contact.label.uppercase()}:${contact.phoneNumber}")
            if (contact.email.isNotBlank()) {
                sb.appendLine("EMAIL:${contact.email}")
            }
            if (contact.organization.isNotBlank()) {
                sb.appendLine("ORG:${contact.organization}")
            }
            if (contact.isVip) {
                sb.appendLine("CATEGORIES:VIP")
            }
            if (contact.notes.isNotBlank()) {
                sb.appendLine("NOTE:${contact.notes}")
            }
            sb.appendLine("END:VCARD")
        }
        return sb.toString()
    }

    // Curated Directory Packs for instant one-tap testing in Full-Screen Import Studio
    val sampleExecutiveVCardPack = """
        BEGIN:VCARD
        VERSION:3.0
        FN:Hannah Abbott
        ORG:Apex Venture Partners
        TEL;TYPE=WORK:(415) 821-3309
        EMAIL:hannah@apexvp.com
        CATEGORIES:VIP
        NOTE:Managing Director — Priority bypass enabled
        END:VCARD
        BEGIN:VCARD
        VERSION:3.0
        FN:Liam O'Connor
        ORG:CloudScale Infrastructure
        TEL;TYPE=CELL:(650) 918-4420
        EMAIL:liam@cloudscale.io
        CATEGORIES:VIP
        NOTE:On-call Systems Lead
        END:VCARD
        BEGIN:VCARD
        VERSION:3.0
        FN:Nadia Rostova
        ORG:Rostova Editorial Studio
        TEL;TYPE=WORK:(212) 794-1182
        EMAIL:nadia@rostovastudio.com
        NOTE:Creative Director
        END:VCARD
        BEGIN:VCARD
        VERSION:3.0
        FN:Mateo Silva
        ORG:Silva Architecture
        TEL;TYPE=CELL:(305) 642-8910
        EMAIL:mateo@silva-arch.com
        END:VCARD
    """.trimIndent()

    val sampleMedicalAndFamilyCsvPack = """
        Name,Phone,Label,Email,Organization,VIP
        Dr. Priya Patel,(415) 602-9940,Medical,ppatel@sfcardio.org,SF Cardiology Group,VIP
        St. Marys Urgent Care,(415) 750-5500,Medical,triage@stmarys.org,St. Marys Medical,VIP
        Arthur Fox (Dad),(503) 441-2901,Home,arthur.fox@gmail.com,Family,VIP
        Maya Lin-Fox (Sister),(206) 832-1944,Mobile,maya.linfox@gmail.com,Family,VIP
        GreenCross Pharmacy,(415) 339-8100,Work,rx@greencross.com,Neighborhood Pharmacy,false
    """.trimIndent()
}
