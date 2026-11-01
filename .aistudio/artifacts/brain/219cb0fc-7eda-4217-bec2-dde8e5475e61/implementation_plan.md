# ClarityDial — Default Phone Dialer, Text Messaging (`sms:`/`smsto:`) Handler, Bluetooth Audio & Call Shield

ClarityDial is a full-featured Android Default Phone Dialer (`ROLE_DIALER`), Call Screening (`ROLE_CALL_SCREENING`), and Text Messaging (`sms:`/`smsto:`) companion backed by **Firebase Authentication (Google Sign-In)** and **Cloud Firestore** persistence. It combines a tactile T9 dialer, an **In-App Active Call UI with Bluetooth & Multi-Route Audio Switching**, an **In-App Text Messaging Hub with `sms:`/`smsto:` Deep-Link Pre-Fill, Incoming Text Simulator & Quick-Reply Templates**, a **Bluetooth Paired Audio Device Manager**, a dedicated **App Settings Hub**, an interactive Call History log, a full-screen importable contacts studio, and an **Automated Call Filtering System & Blocked Calls Log**.

## User Review & Critical Decisions

> [!IMPORTANT]
> The implementation plan has been updated to integrate **Default Text Messaging (`sms:` / `smsto:` Intent Handling, Draft Pre-Fill, Incoming Text Simulator & Optional Carrier Handoff)** alongside **Default Phone Dialer (`ROLE_DIALER` & `ROLE_CALL_SCREENING`)**, **Bluetooth Call Audio Routing**, and **App Settings**. Please review and click **Proceed** to complete the implementation.

- **Confirmed Addition 1 — Google Play Compliant Default Text Messaging (`sms:` / `smsto:`) Handler**:
  - **Zero-Permission `sms:` & `smsto:` Intent Registration**: Registers `MainActivity` for `android.intent.action.SENDTO` and `android.intent.action.VIEW` across `sms`, `smsto`, `mms`, and `mmsto` URI schemes without declaring any prohibited `READ_SMS`, `SEND_SMS`, `WRITE_SMS`, or `RECEIVE_SMS` permissions.
  - **Automatic Recipient & Draft Body Pre-Fill**: Tapping any `sms:` or `smsto:` link in Android (or testing via the App Settings link tester) opens ClarityDial's **Messages** composer directly to that recipient's conversation thread with the draft message text (`sms_body` / `EXTRA_TEXT` / `?body=`) pre-filled in the input bar.
  - **In-App Cloud Messaging with Optional Carrier Handoff**: Users can send messages immediately inside ClarityDial (persisted in real time to Cloud Firestore) and optionally hand off to the device's carrier SMS transport (`launchCarrierSmsOnSend` toggle in **App Settings** and the thread header).
  - **Incoming Text Simulator & Quick-Reply Templates**: Users can simulate incoming text messages from any contact or number in the **Messages** hub (or inside an active conversation) and respond using one-tap **Quick-Reply Templates** or custom messages.
- **Confirmed Addition 2 — Bluetooth Call Audio Routing & Paired Device Manager**:
  - **In-Call Multi-Route Audio Selector (`Bluetooth`, `Speakerphone`, `Earpiece`)** and **Paired Bluetooth Audio Device Manager** (`BluetoothAdapter.bondedDevices` + `AudioManager` communication devices + custom hands-free profiles).
- **Confirmed Addition 3 — Dedicated App Settings Workspace**:
  - Four comprehensive sections: **Default Phone, Call Screening & Text Messaging Handlers**, **Bluetooth & Default Call Audio Preferences**, **Keypad Tones, Haptics & Theme Appearance**, and **Cloud Account Sync & Data Management**.
- **Confirmed Addition 4 — Default Phone App (`ROLE_DIALER`), Call Screening (`ROLE_CALL_SCREENING`) & In-App Active Call UI**:
  - Full-screen **In-App Active Call UI** with live call timer, Bluetooth/Speaker/Earpiece audio routing, Mute, Hold, DTMF keypad, and System Dialer fallback.

---

## 1. Overview & Core Concept

- **What It Does**: Delivers a unified, cloud-synced phone and text messaging suite:
  1. **Default Phone Dialer & In-App Active Call UI (`ROLE_DIALER`)**:
     - Intercepts `tel:` links and `ACTION_DIAL` intents, binds `ClarityInCallService`, and presents a full-screen **In-App Active Call UI** with **Bluetooth / Speaker / Earpiece Audio Switching**, **Mute**, **Hold**, **In-Call DTMF Keypad**, **System Dialer Fallback**, and **End Call**.
  2. **Default Text Messaging Handler (`sms:` / `smsto:`) & Messages Hub**:
     - Intercepts `sms:` and `smsto:` intents, pre-fills the recipient and draft message body in the conversation composer, supports one-tap **Quick-Reply Templates**, an **Incoming Text Simulator**, and configurable **Optional Carrier SMS Handoff**.
  3. **Bluetooth Call Audio & Paired Device Manager**:
     - Discovers paired Bluetooth headsets, earbuds, and car hands-free kits, supports designating a preferred Bluetooth device, and auto-routes active call audio to Bluetooth when connected.
  4. **Comprehensive App Settings Hub**:
     - Manages Default Phone, Call Screening, and Text Messaging handlers, Bluetooth & default call audio output, Keypad DTMF tones & haptic feedback, Daylight/Dark Slate theme, and Cloud Firestore account sync.
  5. **Automated Call Filtering (`Call Shield`), Call History (`Recents`) & Full-Screen Importable Contacts**:
     - Real `ClarityCallScreeningService` + Blocked Calls Log + Interactive Call History + Full-Screen `.vcf`/`.csv` Contact Import Studio.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Handling `sms:` / `smsto:` Links & Composing Texts**:
     - When an external app or link launches `smsto:+14158902314?body=Hello`, ClarityDial opens directly to the **Messages** tab, opens the thread for `(415) 890-2314`, and pre-fills `"Hello"` in the message input field.
     - User taps **Send** to save the message to the Cloud Firestore thread (and if *Carrier SMS Handoff* is enabled, optionally dispatches via the system carrier composer).
     - User can also tap **Simulate Incoming Text** in the Messages header or thread view to receive a realistic incoming message and reply with one-tap **Quick-Reply Templates**.
  2. **Switching Call Audio to Bluetooth During an Active Call**:
     - During an active call in the **In-App Active Call UI**, user switches between **Bluetooth**, **Speaker**, and **Earpiece**, or selects a specific paired Bluetooth headset/car kit.
  3. **Configuring App Settings**:
     - User opens **App Settings** from the top bar gear button (`app_settings_button`) to configure Default Phone (`ROLE_DIALER`), Call Screening (`ROLE_CALL_SCREENING`), and Text Messaging (`sms:`/`smsto:` handler & carrier handoff mode), Bluetooth devices, Keypad tones/haptics/theme, and Cloud Firestore data.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Creator Program Compliant `sms:` / `smsto:` Handler Without Prohibited SMS Permissions**
  - *Chosen Approach*: Declare `ACTION_SENDTO` and `ACTION_VIEW` intent filters for `sms`, `smsto`, `mms`, and `mmsto` schemes on `MainActivity` without declaring prohibited `READ_SMS`, `SEND_SMS`, `WRITE_SMS`, or `RECEIVE_SMS` permissions. Parse both URI query parameters (`?body=`) and Intent extras (`sms_body`, `Intent.EXTRA_TEXT`) to pre-fill the recipient and message draft, and provide an in-app toggle for optional carrier SMS handoff.
  - *Why*: Lets Android route `sms:` and `smsto:` actions directly into ClarityDial's Messages composer while remaining 100% compliant with Google Play Creator Program rules.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│         Android Telecom, Messaging Intents & Bluetooth Audio Layer         │
│  • MainActivity Intent Filters:                                            │
│    - ACTION_DIAL / ACTION_VIEW (scheme="tel")                              │
│    - ACTION_SENDTO / ACTION_VIEW (scheme="sms", "smsto", "mms", "mmsto")   │
│  • ClarityInCallService (InCallService) & ClarityCallScreeningService      │
│  • BluetoothAudioController (BluetoothAdapter + AudioManager + Telecom)    │
│  • DefaultDialerRoleHelper (ROLE_DIALER, ROLE_CALL_SCREENING & SMS parser) │
└──────────────┬───────────────────┬────────────────────┬────────────────────┘
               │                   │                    │
               ▼                   ▼                    ▼
┌────────────────────┐ ┌──────────────────────┐ ┌────────────────────────────┐
│ In-App Active Call │ │ Messages Hub &       │ │ App Settings, Call Shield, │
│ Overlay (Compose)  │ │ SMS Intent Composer  │ │ Keypad, Recents & Contacts │
│ • Bluetooth/Speaker│ │ • Pre-filled Recipient│ │ • Default Phone/SMS Setup  │
│   /Earpiece Router │ │   & Draft Message    │ │ • Paired Bluetooth Manager │
│ • Mute/Hold/DTMF   │ │ • Incoming Simulator │ │ • Tones/Haptics/Dark Slate │
│ • System Fallback  │ │ • Quick Templates    │ │ • VCF Import & Blocked Log │
└─────────┬──────────┘ └───────────┬──────────┘ └──────────────┬─────────────┘
          │                        │                           │
          ▼                        ▼                           ▼
┌────────────────────────────────────────────────────────────────────────────┐
│    ClarityDialViewModel, ActiveCallManager & ClarityFirestoreRepository    │
│  • Syncs Contacts, CallLogs, FilterRules, Messages & App/Shield Settings   │
└────────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping

1. **`DefaultDialerRoleHelper.extractSmsIntentData(intent: Intent?)`**:
   - Extracts the recipient phone number from `sms:` / `smsto:` / `mms:` / `mmsto:` URI `schemeSpecificPart` (stripping any `?body=...` query portion) and extracts the draft body from URI query `?body=`, `intent.getStringExtra("sms_body")`, or `intent.getStringExtra(Intent.EXTRA_TEXT)`.
2. **`ClarityDialViewModel` & `MessagesScreen`**:
   - Maintains `draftMessageBody: StateFlow<String>` so incoming `sms:` / `smsto:` intents (or Quick-Reply Template taps) populate the composer field directly.
   - Adds `simulateIncomingMessage(rawNumber: String, body: String)` (`isOutgoing = false`) so users can test incoming messages in any thread or from the Messages hub.
   - Respects `shieldSettings.carrierSmsHandoffEnabled` (or explicit "Send via Carrier SMS" action) so messages always log to Cloud Firestore and optionally launch external carrier SMS handoff without re-triggering ClarityDial itself.
