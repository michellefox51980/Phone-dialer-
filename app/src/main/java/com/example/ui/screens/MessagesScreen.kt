package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ContactEntity
import com.example.data.MessageEntity
import com.example.data.MessageThreadSummary
import com.example.data.ReplyTemplateEntity
import com.example.domain.PhoneNumberFormatter
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.ContactAvatarPalette
import com.example.ui.theme.EmeraldCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
    threads: List<MessageThreadSummary>,
    allMessages: List<MessageEntity>,
    contacts: List<ContactEntity>,
    replyTemplates: List<ReplyTemplateEntity>,
    activeThreadNormalizedNumber: String?,
    onOpenThread: (String) -> Unit,
    onCloseThread: () -> Unit,
    onSendMessage: (String, String, Boolean) -> Unit,
    onDeleteThread: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onAddReplyTemplate: (String, String, String) -> Unit,
    onDeleteReplyTemplate: (String) -> Unit
) {
    var showNewMessageDialog by remember { mutableStateOf(false) }
    var showTemplateManagerDialog by remember { mutableStateOf(false) }

    if (activeThreadNormalizedNumber != null) {
        val activeContact = remember(contacts, activeThreadNormalizedNumber) {
            contacts.firstOrNull { it.normalizedNumber == activeThreadNormalizedNumber }
        }
        val threadMessages by remember(allMessages, activeThreadNormalizedNumber) {
            derivedStateOf {
                allMessages.filter {
                    it.normalizedNumber.ifBlank { PhoneNumberFormatter.normalize(it.phoneNumber) } == activeThreadNormalizedNumber
                }.sortedBy { it.timestamp }
            }
        }
        val displayPhone = activeContact?.phoneNumber
            ?: threadMessages.lastOrNull()?.phoneNumber
            ?: PhoneNumberFormatter.formatForDisplay(activeThreadNormalizedNumber)
        val displayName = activeContact?.name
            ?: threadMessages.lastOrNull()?.contactName
            ?: displayPhone

        BackHandler {
            onCloseThread()
        }

        ConversationThreadDetailView(
            displayName = displayName,
            displayPhone = displayPhone,
            messages = threadMessages,
            templates = replyTemplates,
            onBack = onCloseThread,
            onCall = { onCallNumber(displayPhone) },
            onDeleteThread = { onDeleteThread(activeThreadNormalizedNumber) },
            onSend = { body ->
                onSendMessage(displayPhone, body, false)
            }
        )
    } else {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Text Messages",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${threads.size} active threads · ${replyTemplates.size} quick templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showTemplateManagerDialog = true },
                            modifier = Modifier.testTag("manage_templates_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesomeMotion,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Templates")
                        }

                        Button(
                            onClick = { showNewMessageDialog = true },
                            modifier = Modifier.testTag("new_message_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Text")
                        }
                    }
                }

                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No text message threads yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'New Text' to start a conversation or send a quick template reply via SMS.",
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
                            .testTag("messages_thread_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(threads, key = { it.normalizedNumber }) { summary ->
                            val avatarColor = ContactAvatarPalette[summary.avatarColorIndex % ContactAvatarPalette.size]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                    .clickable { onOpenThread(summary.normalizedNumber) }
                                    .testTag("thread_item_${summary.normalizedNumber}"),
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
                                            text = summary.contactName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = summary.contactName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (summary.isVip) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "VIP",
                                                        tint = CobaltAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = formatShortTime(summary.lastTimestamp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = summary.lastMessageBody,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (summary.hasAutoReply) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = CobaltContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Includes Automated Call Filter Reply",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CobaltOnContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        }
    }

    if (showNewMessageDialog) {
        NewMessageDialog(
            contacts = contacts,
            templates = replyTemplates,
            onDismiss = { showNewMessageDialog = false },
            onSend = { phone, body ->
                onSendMessage(phone, body, false)
                showNewMessageDialog = false
            }
        )
    }

    if (showTemplateManagerDialog) {
        ReplyTemplateManagerDialog(
            templates = replyTemplates,
            onDismiss = { showTemplateManagerDialog = false },
            onAdd = onAddReplyTemplate,
            onDelete = onDeleteReplyTemplate
        )
    }
}

@Composable
private fun ConversationThreadDetailView(
    displayName: String,
    displayPhone: String,
    messages: List<MessageEntity>,
    templates: List<ReplyTemplateEntity>,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onDeleteThread: () -> Unit,
    onSend: (String) -> Unit
) {
    var draftText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Thread Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to threads"
                    )
                }
                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row {
                IconButton(onClick = onCall) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call $displayName",
                        tint = EmeraldCall
                    )
                }
                IconButton(onClick = onDeleteThread) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete conversation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Messages Timeline
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id.ifBlank { "${it.timestamp}" } }) { msg ->
                val isOut = msg.isOutgoing
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isOut) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        color = if (isOut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isOut) 18.dp else 4.dp,
                            bottomEnd = if (isOut) 4.dp else 18.dp
                        ),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            if (msg.isAutoReply) {
                                Text(
                                    text = "AUTOMATED CALL SHIELD REPLY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOut) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = msg.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatShortTime(msg.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOut) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick Reply Template Chips
        if (templates.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id.ifBlank { it.title } }) { tpl ->
                    AssistChip(
                        onClick = { draftText = tpl.body },
                        label = { Text(tpl.title, maxLines = 1) }
                    )
                }
            }
        }

        // Composer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                placeholder = { Text("Write text message (launches SMS)...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_composer_input"),
                shape = RoundedCornerShape(16.dp)
            )

            FilledTonalIconButton(
                onClick = {
                    if (draftText.isNotBlank()) {
                        onSend(draftText)
                        draftText = ""
                    }
                },
                enabled = draftText.isNotBlank(),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("send_message_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send via SMS"
                )
            }
        }
    }
}

@Composable
private fun NewMessageDialog(
    contacts: List<ContactEntity>,
    templates: List<ReplyTemplateEntity>,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    var recipientPhone by remember { mutableStateOf("") }
    var messageBody by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Text Message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = recipientPhone,
                    onValueChange = { recipientPhone = it },
                    label = { Text("Recipient Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (contacts.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(contacts.take(6), key = { it.id.ifBlank { it.phoneNumber } }) { c ->
                            AssistChip(
                                onClick = { recipientPhone = c.phoneNumber },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    label = { Text("Message Body") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                if (templates.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(templates, key = { it.id.ifBlank { it.title } }) { tpl ->
                            AssistChip(
                                onClick = { messageBody = tpl.body },
                                label = { Text(tpl.title) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipientPhone.isNotBlank() && messageBody.isNotBlank()) {
                        onSend(recipientPhone, messageBody)
                    }
                }
            ) {
                Text("Send via SMS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReplyTemplateManagerDialog(
    templates: List<ReplyTemplateEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Reply SMS Templates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Template Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newBody,
                    onValueChange = { newBody = it },
                    label = { Text("Reply Text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newBody.isNotBlank()) {
                            onAdd(newTitle, newBody, "CUSTOM")
                            newTitle = ""
                            newBody = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Template")
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(templates, key = { it.id.ifBlank { it.title } }) { tpl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tpl.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    tpl.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                            if (tpl.id.isNotBlank()) {
                                IconButton(onClick = { onDelete(tpl.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete template"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private fun formatShortTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
