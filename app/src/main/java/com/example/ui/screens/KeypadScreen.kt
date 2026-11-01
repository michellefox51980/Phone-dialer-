package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ContactEntity
import com.example.domain.PhoneNumberFormatter
import com.example.ui.theme.ContactAvatarPalette
import com.example.ui.theme.EmeraldCall

private data class DialKeySpec(
    val digit: String,
    val letters: String
)

private val DialPadRows = listOf(
    listOf(
        DialKeySpec("1", "VOICEMAIL"),
        DialKeySpec("2", "ABC"),
        DialKeySpec("3", "DEF")
    ),
    listOf(
        DialKeySpec("4", "GHI"),
        DialKeySpec("5", "JKL"),
        DialKeySpec("6", "MNO")
    ),
    listOf(
        DialKeySpec("7", "PQRS"),
        DialKeySpec("8", "TUV"),
        DialKeySpec("9", "WXYZ")
    ),
    listOf(
        DialKeySpec("*", ""),
        DialKeySpec("0", "+"),
        DialKeySpec("#", "")
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
    dialInput: String,
    contacts: List<ContactEntity>,
    onDigitPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSelectNumber: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onQuickSaveContact: (String) -> Unit,
    hapticsEnabled: Boolean = true,
    isDefaultDialer: Boolean = false,
    activeBluetoothName: String = "Studio Pro Hands-Free",
    autoRouteBluetooth: Boolean = true,
    onOpenRoleAndAudioSheet: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val vipContacts by remember(contacts) {
        derivedStateOf { contacts.filter { it.isVip } }
    }
    val matchedContacts by remember(dialInput, contacts) {
        derivedStateOf {
            if (dialInput.isBlank()) emptyList()
            else contacts.filter { PhoneNumberFormatter.matchesDialQuery(it, dialInput) }.take(3)
        }
    }

    val formattedReadout = remember(dialInput) {
        PhoneNumberFormatter.formatForDisplay(dialInput)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: Speed-dial VIPs & Live T9 Contact Matches
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (vipContacts.isNotEmpty() && dialInput.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VIP Speed Dial",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(vipContacts, key = { it.id.ifBlank { it.phoneNumber } }) { vip ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectNumber(vip.phoneNumber)
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val avatarColor = ContactAvatarPalette[vip.avatarColorIndex % ContactAvatarPalette.size]
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = vip.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = vip.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Number Readout Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (dialInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQuickSaveContact(dialInput) },
                                    modifier = Modifier.testTag("keypad_add_contact_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Save number to Contacts",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }

                            Text(
                                text = formattedReadout.ifEmpty { "Enter number or T9 name" },
                                style = if (dialInput.isEmpty()) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.displayMedium
                                },
                                color = if (dialInput.isEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialpad_number_display")
                            )

                            if (dialInput.isNotEmpty()) {
                                IconButton(
                                    onClick = onClear,
                                    modifier = Modifier.testTag("keypad_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear number",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }

                        // Live T9 / Number Match Banner
                        AnimatedVisibility(visible = matchedContacts.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                matchedContacts.forEach { match ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onSelectNumber(match.phoneNumber) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = match.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (match.isVip) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "VIP",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = match.phoneNumber,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Middle section: Tactile 4x3 Keypad Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DialPadRows.forEach { rowSpecs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSpecs.forEach { keySpec ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                                    .combinedClickable(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDigitPress(keySpec.digit)
                                        },
                                        onLongClick = {
                                            if (keySpec.digit == "0") {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onDigitPress("+")
                                            }
                                        }
                                    )
                                    .testTag("dialpad_key_${keySpec.digit}"),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = keySpec.digit,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (keySpec.letters.isNotEmpty()) {
                                        Text(
                                            text = keySpec.letters,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar: Message, Call CTA, Backspace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (dialInput.isNotBlank()) {
                            onMessageClick(dialInput)
                        }
                    },
                    enabled = dialInput.isNotBlank(),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("keypad_message_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = "Send text message to number"
                    )
                }

                Button(
                    onClick = {
                        if (dialInput.isNotBlank()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCallClick(dialInput)
                        }
                    },
                    enabled = dialInput.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("keypad_call_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldCall,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackspace()
                    },
                    enabled = dialInput.isNotEmpty(),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("keypad_backspace_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete last digit"
                    )
                }
            }
        }
    }
}
