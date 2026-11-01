package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ClarityFirestoreRepository
import com.example.ui.AppTab
import com.example.ui.ClarityDialViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CallShieldScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.KeypadScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.ClarityDialTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        ClarityDialTheme(darkTheme = false) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        AuthenticatedClarityDialApp(
            currentUserId = user.uid,
            userDisplayName = user.displayName ?: user.email ?: "Signed-In User"
        )
    }
}

private data class NavDestinationSpec(
    val tab: AppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NavDestinations = listOf(
    NavDestinationSpec(
        tab = AppTab.KEYPAD,
        label = "Keypad",
        selectedIcon = Icons.Filled.Dialpad,
        unselectedIcon = Icons.Outlined.Dialpad,
        testTag = "nav_tab_keypad"
    ),
    NavDestinationSpec(
        tab = AppTab.RECENTS,
        label = "Recents",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_recents"
    ),
    NavDestinationSpec(
        tab = AppTab.CONTACTS,
        label = "Contacts",
        selectedIcon = Icons.Filled.Contacts,
        unselectedIcon = Icons.Outlined.Contacts,
        testTag = "nav_tab_contacts"
    ),
    NavDestinationSpec(
        tab = AppTab.MESSAGES,
        label = "Messages",
        selectedIcon = Icons.AutoMirrored.Filled.Message,
        unselectedIcon = Icons.AutoMirrored.Outlined.Message,
        testTag = "nav_tab_messages"
    ),
    NavDestinationSpec(
        tab = AppTab.SHIELD,
        label = "Call Shield",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security,
        testTag = "nav_tab_shield"
    )
)

@Composable
fun AuthenticatedClarityDialApp(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ClarityDialViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ClarityDialViewModel(
                    repository = ClarityFirestoreRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val dialInput by viewModel.dialInput.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val filterRules by viewModel.filterRules.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val messageThreads by viewModel.messageThreads.collectAsStateWithLifecycle()
    val replyTemplates by viewModel.replyTemplates.collectAsStateWithLifecycle()
    val shieldSettings by viewModel.shieldSettings.collectAsStateWithLifecycle()
    val activeThreadNumber by viewModel.activeThreadNumber.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val lastSimulationResult by viewModel.lastSimulationResult.collectAsStateWithLifecycle()
    val isFullScreenImportOpen by viewModel.isFullScreenImportOpen.collectAsStateWithLifecycle()
    val importCandidates by viewModel.importCandidates.collectAsStateWithLifecycle()
    val selectedCallLogForDetail by viewModel.selectedCallLogForDetail.collectAsStateWithLifecycle()

    var pendingQuickSavePhone by remember { mutableStateOf<String?>(null) }
    var pendingQuickSaveName by remember { mutableStateOf<String?>(null) }

    val blockedCallsCount = remember(callLogs) {
        callLogs.count { it.filterVerdict == "BLOCKED" || it.filterVerdict == "SILENCED" }
    }

    ClarityDialTheme(darkTheme = shieldSettings.darkModeEnabled) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAccountAndShieldBar(
                        userDisplayName = userDisplayName,
                        shieldActive = shieldSettings.masterShieldEnabled,
                        onShieldBadgeClick = { viewModel.selectTab(AppTab.SHIELD) },
                        onSignOutClick = {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        }
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar {
                            NavDestinations.forEach { dest ->
                                val isSelected = selectedTab == dest.tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        if (dest.tab == AppTab.SHIELD && blockedCallsCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text(blockedCallsCount.toString()) }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        }
                                    },
                                    label = { Text(dest.label, maxLines = 1) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            NavDestinations.forEach { dest ->
                                val isSelected = selectedTab == dest.tab
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedVisibility(visible = statusBannerMessage != null) {
                            statusBannerMessage?.let { msg ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.dismissBanner() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss notification",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                AppTab.KEYPAD -> KeypadScreen(
                                    dialInput = dialInput,
                                    contacts = contacts,
                                    onDigitPress = viewModel::appendDialDigit,
                                    onBackspace = viewModel::backspaceDialDigit,
                                    onClear = viewModel::clearDialInput,
                                    onSelectNumber = viewModel::setDialNumber,
                                    onCallClick = { number ->
                                        viewModel.recordOutgoingCall(number) { cleanNum ->
                                            launchSystemDialer(context, cleanNum)
                                        }
                                    },
                                    onMessageClick = { number ->
                                        viewModel.openMessageThreadForNumber(number)
                                    },
                                    onQuickSaveContact = { number ->
                                        pendingQuickSavePhone = number
                                        pendingQuickSaveName = ""
                                        viewModel.selectTab(AppTab.CONTACTS)
                                    }
                                )

                                AppTab.RECENTS -> RecentsScreen(
                                    callLogs = callLogs,
                                    contacts = contacts,
                                    selectedLogForDetail = selectedCallLogForDetail,
                                    onSelectLogForDetail = viewModel::openCallLogDetail,
                                    onDismissLogDetail = viewModel::closeCallLogDetail,
                                    onCallBack = { number ->
                                        viewModel.recordOutgoingCall(number) { cleanNum ->
                                            launchSystemDialer(context, cleanNum)
                                        }
                                    },
                                    onMessageCaller = { number ->
                                        viewModel.openMessageThreadForNumber(number)
                                    },
                                    onQuickBlockCaller = viewModel::quickBlockSpecificNumber,
                                    onQuickSaveContact = { number, name ->
                                        pendingQuickSavePhone = number
                                        pendingQuickSaveName = name
                                        viewModel.selectTab(AppTab.CONTACTS)
                                    },
                                    onDeleteCallLog = viewModel::deleteCallLog,
                                    onClearAllLogs = viewModel::clearAllCallLogs,
                                    onAddManualCallLog = viewModel::logCustomCallEntry
                                )

                                AppTab.CONTACTS -> ContactsScreen(
                                    contacts = contacts,
                                    callLogs = callLogs,
                                    isFullScreenImportOpen = isFullScreenImportOpen,
                                    importCandidates = importCandidates,
                                    initialAddPhone = pendingQuickSavePhone,
                                    initialAddName = pendingQuickSaveName,
                                    onClearInitialAddPhone = {
                                        pendingQuickSavePhone = null
                                        pendingQuickSaveName = null
                                    },
                                    onOpenFullScreenImport = viewModel::openFullScreenImport,
                                    onCloseFullScreenImport = viewModel::closeFullScreenImport,
                                    onParseImportContent = viewModel::parseImportText,
                                    onSystemPickedContact = viewModel::addSystemPickedContactCandidate,
                                    onToggleCandidateSelection = viewModel::toggleCandidateSelection,
                                    onToggleCandidateVip = viewModel::toggleCandidateVip,
                                    onSelectAllCandidates = viewModel::setAllCandidatesSelected,
                                    onMarkAllSelectedVip = viewModel::setAllSelectedCandidatesVip,
                                    onCommitStagedContacts = viewModel::commitStagedContacts,
                                    onSaveContact = viewModel::saveContact,
                                    onToggleVip = viewModel::toggleContactVip,
                                    onDeleteContact = viewModel::deleteContact,
                                    onCallContact = { number ->
                                        viewModel.recordOutgoingCall(number) { cleanNum ->
                                            launchSystemDialer(context, cleanNum)
                                        }
                                    },
                                    onMessageContact = { number ->
                                        viewModel.openMessageThreadForNumber(number)
                                    },
                                    onExportVCard = { vcfText ->
                                        shareVCardText(context, vcfText)
                                    }
                                )

                                AppTab.MESSAGES -> MessagesScreen(
                                    threads = messageThreads,
                                    allMessages = messages,
                                    contacts = contacts,
                                    replyTemplates = replyTemplates,
                                    activeThreadNormalizedNumber = activeThreadNumber,
                                    onOpenThread = viewModel::openMessageThreadForNumber,
                                    onCloseThread = viewModel::closeActiveMessageThread,
                                    onSendMessage = { number, body, isAuto ->
                                        viewModel.sendOrLogMessage(number, body, isAuto) { targetNum, msgBody ->
                                            launchSystemSmsComposer(context, targetNum, msgBody)
                                        }
                                    },
                                    onDeleteThread = viewModel::deleteMessageThread,
                                    onCallNumber = { number ->
                                        viewModel.recordOutgoingCall(number) { cleanNum ->
                                            launchSystemDialer(context, cleanNum)
                                        }
                                    },
                                    onAddReplyTemplate = viewModel::addReplyTemplate,
                                    onDeleteReplyTemplate = viewModel::deleteReplyTemplate
                                )

                                AppTab.SHIELD -> CallShieldScreen(
                                    settings = shieldSettings,
                                    rules = filterRules,
                                    callLogs = callLogs,
                                    contacts = contacts,
                                    lastSimulationResult = lastSimulationResult,
                                    onUpdateSettings = viewModel::updateShieldSettings,
                                    onSaveFilterRule = viewModel::addOrUpdateFilterRule,
                                    onToggleFilterRule = viewModel::toggleFilterRule,
                                    onDeleteFilterRule = viewModel::deleteFilterRule,
                                    onRunSimulation = viewModel::simulateIncomingCall,
                                    onDispatchAutoReplySms = { number, replyText ->
                                        viewModel.sendOrLogMessage(number, replyText, isAutoReply = true) { targetNum, msgBody ->
                                            launchSystemSmsComposer(context, targetNum, msgBody)
                                        }
                                    },
                                    onCallBackFromLog = { number ->
                                        viewModel.recordOutgoingCall(number) { cleanNum ->
                                            launchSystemDialer(context, cleanNum)
                                        }
                                    },
                                    onDeleteCallLog = viewModel::deleteCallLog
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAccountAndShieldBar(
    userDisplayName: String,
    shieldActive: Boolean,
    onShieldBadgeClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userDisplayName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ClarityDial",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onShieldBadgeClick,
                    color = if (shieldActive) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (shieldActive) EmeraldOnContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (shieldActive) "Shield On" else "Shield Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (shieldActive) EmeraldOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out of Google account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun launchSystemDialer(context: Context, rawPhoneNumber: String) {
    try {
        val clean = rawPhoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (clean.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(clean)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun launchSystemSmsComposer(context: Context, rawPhoneNumber: String, body: String) {
    try {
        val clean = rawPhoneNumber.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(clean)}")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun shareVCardText(context: Context, vcfContent: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_SUBJECT, "ClarityDial Contacts Export (.vcf)")
            putExtra(Intent.EXTRA_TEXT, vcfContent)
        }
        val chooser = Intent.createChooser(sendIntent, "Export Contacts (.vcf)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}
