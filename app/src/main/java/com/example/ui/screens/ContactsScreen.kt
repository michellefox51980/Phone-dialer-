package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CallLogEntity
import com.example.data.ContactEntity
import com.example.data.ImportedContactCandidate
import com.example.domain.ContactImportParser
import com.example.domain.PhoneNumberFormatter
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.ContactAvatarPalette
import com.example.ui.theme.EmeraldCall
import java.io.BufferedReader
import java.io.InputStreamReader

private enum class ContactDirectoryFilter(val label: String) {
    ALL("All"),
    VIP("VIP Bypass"),
    IMPORTED("Imported"),
    WORK("Work"),
    MEDICAL("Medical")
}

@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    callLogs: List<CallLogEntity>,
    isFullScreenImportOpen: Boolean,
    importCandidates: List<ImportedContactCandidate>,
    initialAddPhone: String?,
    initialAddName: String?,
    onClearInitialAddPhone: () -> Unit,
    onOpenFullScreenImport: () -> Unit,
    onCloseFullScreenImport: () -> Unit,
    onParseImportContent: (String, String) -> Unit,
    onSystemPickedContact: (String, String) -> Unit,
    onToggleCandidateSelection: (String) -> Unit,
    onToggleCandidateVip: (String) -> Unit,
    onSelectAllCandidates: (Boolean) -> Unit,
    onMarkAllSelectedVip: (Boolean) -> Unit,
    onCommitStagedContacts: (Boolean) -> Unit,
    onSaveContact: (ContactEntity?, String, String, String, String, String, Boolean, String) -> Unit,
    onToggleVip: (ContactEntity) -> Unit,
    onDeleteContact: (String) -> Unit,
    onCallContact: (String) -> Unit,
    onMessageContact: (String) -> Unit,
    onExportVCard: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ContactDirectoryFilter.ALL) }
    var selectedContactForDetail by remember { mutableStateOf<ContactEntity?>(null) }
    var editingContact by remember { mutableStateOf<ContactEntity?>(null) }
    var isCreatingContact by remember { mutableStateOf(false) }

    // Open Add Contact dialog if triggered from Keypad or Call History
    if (initialAddPhone != null && !isCreatingContact) {
        isCreatingContact = true
    }

    val filteredContacts by remember(contacts, searchQuery, selectedFilter) {
        derivedStateOf {
            contacts.filter { c ->
                val matchesFilter = when (selectedFilter) {
                    ContactDirectoryFilter.ALL -> true
                    ContactDirectoryFilter.VIP -> c.isVip
                    ContactDirectoryFilter.IMPORTED -> c.importSource != "MANUAL" && c.importSource != "STARTER"
                    ContactDirectoryFilter.WORK -> c.label.equals("Work", ignoreCase = true)
                    ContactDirectoryFilter.MEDICAL -> c.label.equals("Medical", ignoreCase = true)
                }
                val q = searchQuery.trim().lowercase()
                val matchesSearch = q.isEmpty() ||
                    c.name.lowercase().contains(q) ||
                    c.phoneNumber.lowercase().contains(q) ||
                    c.organization.lowercase().contains(q) ||
                    c.email.lowercase().contains(q)
                matchesFilter && matchesSearch
            }
        }
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
            // Top Directory Header & Full-Screen Import Studio Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Contacts Directory",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${contacts.size} cloud contacts · ${contacts.count { it.isVip }} VIP bypass",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenFullScreenImport,
                        modifier = Modifier.testTag("open_fullscreen_import_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = {
                            editingContact = null
                            isCreatingContact = true
                        },
                        modifier = Modifier.testTag("add_new_contact_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Search & Export Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, phone, company...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contacts_search_input")
                )

                if (contacts.isNotEmpty()) {
                    FilledTonalIconButton(
                        onClick = {
                            val vcf = ContactImportParser.exportToVCard(contacts)
                            onExportVCard(vcf)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("export_contacts_vcf_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Export contacts as vCard (.vcf)"
                        )
                    }
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ContactDirectoryFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("contacts_filter_${filter.name.lowercase()}")
                    )
                }
            }

            // Contacts List
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching contacts found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Import' to launch the Full-Screen Contact Import Studio (.vcf, .csv, System Contacts, or starter packs).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onOpenFullScreenImport) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Full-Screen Import Studio")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("contacts_directory_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredContacts, key = { it.id.ifBlank { it.phoneNumber } }) { contact ->
                        ContactRowCard(
                            contact = contact,
                            onRowClick = { selectedContactForDetail = contact },
                            onToggleVip = { onToggleVip(contact) },
                            onCallClick = { onCallContact(contact.phoneNumber) },
                            onMessageClick = { onMessageContact(contact.phoneNumber) }
                        )
                    }
                }
            }
        }
    }

    // Full-Screen Contact Profile Detail Modal
    if (selectedContactForDetail != null) {
        val currentContact = contacts.firstOrNull { it.id == selectedContactForDetail?.id }
            ?: selectedContactForDetail!!
        val contactCalls = remember(callLogs, currentContact.normalizedNumber) {
            callLogs.filter {
                PhoneNumberFormatter.normalize(it.phoneNumber) == currentContact.normalizedNumber
            }
        }
        FullScreenContactProfileDialog(
            contact = currentContact,
            recentCalls = contactCalls,
            onDismiss = { selectedContactForDetail = null },
            onToggleVip = { onToggleVip(currentContact) },
            onEdit = {
                editingContact = currentContact
                selectedContactForDetail = null
                isCreatingContact = true
            },
            onDelete = {
                onDeleteContact(currentContact.id)
                selectedContactForDetail = null
            },
            onCall = {
                selectedContactForDetail = null
                onCallContact(currentContact.phoneNumber)
            },
            onMessage = {
                selectedContactForDetail = null
                onMessageContact(currentContact.phoneNumber)
            },
            onShareVcf = {
                val singleVcf = ContactImportParser.exportToVCard(listOf(currentContact))
                onExportVCard(singleVcf)
            }
        )
    }

    // Add / Edit Contact Dialog
    if (isCreatingContact) {
        AddOrEditContactDialog(
            existing = editingContact,
            prefillPhone = initialAddPhone,
            prefillName = initialAddName,
            onDismiss = {
                isCreatingContact = false
                editingContact = null
                onClearInitialAddPhone()
            },
            onSave = { name, phone, label, email, org, isVip, notes ->
                onSaveContact(editingContact, name, phone, label, email, org, isVip, notes)
                isCreatingContact = false
                editingContact = null
                onClearInitialAddPhone()
            }
        )
    }

    // Full-Screen Importable Contacts Studio
    if (isFullScreenImportOpen) {
        FullScreenContactImportStudioDialog(
            candidates = importCandidates,
            onDismiss = onCloseFullScreenImport,
            onParseImportContent = onParseImportContent,
            onSystemPickedContact = onSystemPickedContact,
            onToggleCandidateSelection = onToggleCandidateSelection,
            onToggleCandidateVip = onToggleCandidateVip,
            onSelectAllCandidates = onSelectAllCandidates,
            onMarkAllSelectedVip = onMarkAllSelectedVip,
            onCommitStagedContacts = onCommitStagedContacts
        )
    }
}

@Composable
private fun ContactRowCard(
    contact: ContactEntity,
    onRowClick: () -> Unit,
    onToggleVip: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    val avatarColor = ContactAvatarPalette[contact.avatarColorIndex % ContactAvatarPalette.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onRowClick() }
            .testTag("contact_row_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.isVip) {
                        Surface(
                            color = CobaltContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "VIP",
                                style = MaterialTheme.typography.labelSmall,
                                color = CobaltOnContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (contact.importSource != "MANUAL" && contact.importSource != "STARTER") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = contact.importSource.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${contact.phoneNumber} · ${contact.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (contact.organization.isNotBlank()) {
                    Text(
                        text = contact.organization,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onToggleVip) {
                Icon(
                    imageVector = if (contact.isVip) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle VIP status",
                    tint = if (contact.isVip) CobaltAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMessageClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Message ${contact.name}",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = EmeraldCall
                )
            }
        }
    }
}

@Composable
private fun FullScreenContactImportStudioDialog(
    candidates: List<ImportedContactCandidate>,
    onDismiss: () -> Unit,
    onParseImportContent: (String, String) -> Unit,
    onSystemPickedContact: (String, String) -> Unit,
    onToggleCandidateSelection: (String) -> Unit,
    onToggleCandidateVip: (String) -> Unit,
    onSelectAllCandidates: (Boolean) -> Unit,
    onMarkAllSelectedVip: (Boolean) -> Unit,
    onCommitStagedContacts: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var rawPasteText by remember { mutableStateOf("") }
    var showPasteBox by remember { mutableStateOf(false) }
    var overwriteDuplicates by remember { mutableStateOf(false) }

    BackHandler {
        onDismiss()
    }

    // Zero-Permission Storage Access Framework launcher for .vcf and .csv files
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = readTextFromUri(context, uri)
            if (content.isNotBlank()) {
                val tag = if (content.contains("BEGIN:VCARD", ignoreCase = true)) "VCF_FILE" else "CSV_FILE"
                onParseImportContent(content, tag)
            }
        }
    }

    // Zero-Permission Android System Contact Picker
    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val picked = extractContactFromUri(context, uri)
            if (picked != null) {
                onSystemPickedContact(picked.first, picked.second)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("fullscreen_contact_import_studio"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_fullscreen_import_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Import Studio"
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Full-Screen Contact Import Studio",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Import .vcf vCards, .csv spreadsheets, or system contacts to Cloud Firestore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Import Source Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            documentLauncher.launch(
                                arrayOf(
                                    "text/x-vcard",
                                    "text/vcard",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_from_file_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick .VCF / .CSV", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { pickContactLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_system_contact_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPage,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("System Picker", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onParseImportContent(
                                ContactImportParser.sampleExecutiveVCardPack,
                                "VCF_FILE"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_executive_vcf_pack_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load vCard Pack", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = {
                            onParseImportContent(
                                ContactImportParser.sampleMedicalAndFamilyCsvPack,
                                "CSV_FILE"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_medical_csv_pack_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load CSV Pack", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showPasteBox = !showPasteBox },
                        modifier = Modifier.testTag("toggle_paste_import_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste")
                    }
                }

                if (showPasteBox) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = rawPasteText,
                                onValueChange = { rawPasteText = it },
                                label = { Text("Paste BEGIN:VCARD blocks or CSV rows (Name, Phone, Label, VIP)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .testTag("paste_import_text_field")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (rawPasteText.isNotBlank()) {
                                            onParseImportContent(rawPasteText, "BATCH_PASTE")
                                            showPasteBox = false
                                        }
                                    },
                                    modifier = Modifier.testTag("parse_pasted_contacts_button")
                                ) {
                                    Text("Parse Contacts")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Staging & Deduplication Table
                if (candidates.isEmpty()) {
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
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ready to Stage Contacts for Import",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Choose a .vcf or .csv file, pick from Android System Contacts, paste raw vCard/CSV rows, or load a starter pack above to preview and deduplicate before importing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val selectedCount = candidates.count { it.isSelected }
                    val duplicateCount = candidates.count { it.isDuplicate }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Staged Candidates (${candidates.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$selectedCount selected · $duplicateCount existing duplicates detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { onSelectAllCandidates(true) }) {
                                Text("Select All")
                            }
                            TextButton(onClick = { onMarkAllSelectedVip(true) }) {
                                Text("Star VIP")
                            }
                        }
                    }

                    if (duplicateCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Update existing contacts when duplicate phone number matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = overwriteDuplicates,
                                onCheckedChange = { overwriteDuplicates = it }
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.tempId }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (item.isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onToggleCandidateSelection(item.tempId) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { onToggleCandidateSelection(item.tempId) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (item.isDuplicate) {
                                                Surface(
                                                    color = AmberContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Duplicate",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AmberOnContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${item.phoneNumber} · ${item.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.organization.isNotBlank() || item.email.isNotBlank()) {
                                            Text(
                                                text = listOf(item.organization, item.email).filter { it.isNotBlank() }.joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    IconButton(onClick = { onToggleCandidateVip(item.tempId) }) {
                                        Icon(
                                            imageVector = if (item.isVip) Icons.Default.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "Mark imported contact as VIP",
                                            tint = if (item.isVip) CobaltAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Commit Bar
                    Button(
                        onClick = { onCommitStagedContacts(overwriteDuplicates) },
                        enabled = selectedCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("commit_imported_contacts_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import $selectedCount Contacts to Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenContactProfileDialog(
    contact: ContactEntity,
    recentCalls: List<CallLogEntity>,
    onDismiss: () -> Unit,
    onToggleVip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onShareVcf: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close profile")
                    }
                    Row {
                        IconButton(onClick = onShareVcf) {
                            Icon(imageVector = Icons.Default.IosShare, contentDescription = "Share vCard")
                        }
                        IconButton(onClick = onEdit) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit contact")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete contact",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val avatarColor = ContactAvatarPalette[contact.avatarColorIndex % ContactAvatarPalette.size]
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${contact.phoneNumber} · ${contact.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (contact.organization.isNotBlank()) {
                    Text(
                        text = contact.organization,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCall,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldCall,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call")
                    }

                    FilledTonalButton(
                        onClick = onMessage,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Message")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VIP Call Shield Bypass",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Always allow calls during Quiet Hours and strict filters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = contact.isVip,
                                onCheckedChange = { onToggleVip() }
                            )
                        }

                        if (contact.email.isNotBlank()) {
                            HorizontalDivider()
                            Text("Email: ${contact.email}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (contact.notes.isNotBlank()) {
                            HorizontalDivider()
                            Text("Notes: ${contact.notes}", style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider()
                        Text(
                            text = "Source: ${contact.importSource.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (recentCalls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Call History with ${contact.name} (${recentCalls.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            recentCalls.take(5).forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${log.direction} (${log.filterVerdict})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${log.durationSeconds}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun AddOrEditContactDialog(
    existing: ContactEntity?,
    prefillPhone: String?,
    prefillName: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Boolean, String) -> Unit
) {
    var name by remember(existing, prefillName) {
        mutableStateOf(existing?.name ?: prefillName.orEmpty())
    }
    var phone by remember(existing, prefillPhone) {
        mutableStateOf(existing?.phoneNumber ?: prefillPhone.orEmpty())
    }
    var label by remember(existing) { mutableStateOf(existing?.label ?: "Mobile") }
    var email by remember(existing) { mutableStateOf(existing?.email.orEmpty()) }
    var organization by remember(existing) { mutableStateOf(existing?.organization.orEmpty()) }
    var isVip by remember(existing) { mutableStateOf(existing?.isVip ?: false) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Contact" else "Edit Contact") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Mobile", "Work", "Home", "Medical", "Main")) { item ->
                        FilterChip(
                            selected = label.equals(item, ignoreCase = true),
                            onClick = { label = item },
                            label = { Text(item) }
                        )
                    }
                }
                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text("Company / Organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Star as VIP (Bypass Call Filters)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = isVip, onCheckedChange = { isVip = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && PhoneNumberFormatter.normalize(phone).isNotEmpty()) {
                        onSave(name, phone, label, email, organization, isVip, notes)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun readTextFromUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }.orEmpty()
    } catch (_: Exception) {
        ""
    }
}

private fun extractContactFromUri(context: Context, contactUri: Uri): Pair<String, String>? {
    return try {
        var displayName = ""
        var contactId = ""
        context.contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                if (idIdx >= 0) contactId = cursor.getString(idIdx).orEmpty()
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty()
            }
        }
        var phoneNumber = ""
        if (contactId.isNotEmpty()) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx >= 0) phoneNumber = phoneCursor.getString(numIdx).orEmpty()
                }
            }
        }
        if (displayName.isNotEmpty() || phoneNumber.isNotEmpty()) {
            displayName to phoneNumber.ifBlank { "(415) 555-0188" }
        } else null
    } catch (_: Exception) {
        null
    }
}
