# Production Call Recording & Call History Application for Android

A production-quality Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room**, and **Kotlin Coroutines**. Designed with strict adherence to modern Android privacy boundaries, security controls, and transparent capability reporting.

---

## Technical Investigation & Android Reality Report

### 1. Normal Cellular Calls (PSTN / VoLTE / VoNR)
* **Hardware Line Two-Way Recording (`VOICE_CALL`, `VOICE_DOWNLINK`, `VOICE_UPLINK`)**:
  * **Removed in Android 6 (API 23)** and **hard-locked in Android 9 (API 28)**.
  * Capturing raw downlink audio directly off the telephony modem requires the system-level signature permission `android.permission.CAPTURE_AUDIO_OUTPUT`. Non-system third-party applications receive an immediate `SecurityException` or silent zero-byte streams.
* **Telecom Framework / Default Dialer (`InCallService`)**:
  * An app can register as `InCallService` only if set by the user as the system Default Phone App (`RoleManager.ROLE_DIALER`).
  * However, even default dialer apps on stock AOSP/Pixel builds **do not** gain modem audio tap privileges without OEM system vendor integration (such as Samsung One UI or Xiaomi MIUI native call recorders).
* **Accessibility Protections**:
  * Google Play Developer Program policies and Android 13+ (API 33) Restricted Settings strictly block Accessibility Services from being leveraged to intercept audio streams.
  * This application **does not bypass** accessibility boundaries or privacy safeguards.
* **Permitted Method**:
  * High-fidelity acoustic capture via `AudioSource.MIC` or `AudioSource.VOICE_COMMUNICATION` powered by a visible foreground service (`foregroundServiceType="microphone"`).
  * **Speakerphone Assistance**: When active calls are placed on speakerphone, the microphone acoustically captures both sides of the conversation cleanly. The app provides built-in visual guidance for this workflow.

### 2. WhatsApp Voice Calls (VoIP)
* **`AudioPlaybackCaptureConfiguration` (Android 10+)**:
  * Android 10 introduced `AudioPlaybackCapture`, but the platform strictly limits capture to streams tagged with `USAGE_MEDIA`, `USAGE_GAME`, or `USAGE_UNKNOWN`.
  * Audio streams marked with `USAGE_VOICE_COMMUNICATION` (used by WhatsApp, Zoom, Teams, and VoIP dialers) are **explicitly excluded by Android OS platform policy**. Furthermore, apps must opt into `FLAG_ALLOW_CAPTURE_BY_ALL`, which proprietary VoIP apps do not do for private call streams.
* **Concurrent Audio Capture (Android 10+)**:
  * The Android audio server prioritizes active communications apps over background recorders. If WhatsApp is actively holding the microphone, concurrent attempts by third-party apps will be silenced or receive capture priority contention.
* **Status**: Clearly marked as **RESTRICTED BY OS / CONDITIONAL** in the application dashboard with zero deceptive claims.

### 3. WhatsApp Video Calls (MediaProjection)
* **Screen Video Capture**:
  * Supported via Android's `MediaProjection` API.
  * Enforces **explicit user consent** each session via `MediaProjectionManager.createScreenCaptureIntent()`.
  * Android 14+ (API 34) compliant: Runs within a dedicated foreground service with `android:foregroundServiceType="mediaProjection"`.
* **Audio Track**:
  * Video audio track records microphone acoustic input, picking up local voice and speakerphone audio.
* **Persistent Notification**:
  * Ongoing, non-dismissible notification while video recording is running, providing a one-tap Stop & Save control.

---

## Key Features & Architecture

```
com.callrecorder.app
├── data
│   ├── local
│   │   ├── AppDatabase.kt          (Room database)
│   │   ├── dao/RecordingDao.kt     (CRUD queries, count, duration aggregations)
│   │   └── entity/RecordingEntity.kt
│   └── repository
│       └── RecordingRepository.kt  (Scoped storage operations & Room repository)
├── domain
│   └── model
│       ├── AudioConfig.kt          (Audio source & format options)
│       ├── CallType.kt             (NORMAL_CALL, WHATSAPP_VOICE, WHATSAPP_VIDEO)
│       ├── CompatibilityReport.kt  (Platform capabilities & breakdown)
│       └── RecordingState.kt       (Idle, Recording, Paused, Error)
├── engine
│   ├── AudioPlaybackEngine.kt      (MediaPlayer with seek, progress StateFlow)
│   ├── AudioRecordingEngine.kt     (MediaRecorder with amplitude visualizer)
│   ├── CompatibilityChecker.kt     (Android OS version & OEM capability auditor)
│   └── VideoRecordingEngine.kt     (MediaProjection + VirtualDisplay + MediaRecorder)
├── service
│   ├── CallRecordingService.kt     (Foreground service: type microphone)
│   ├── CallStateReceiver.kt        (Telephony broadcast receiver)
│   ├── RecordingNotificationHelper.kt
│   └── ScreenRecordingService.kt   (Foreground service: type mediaProjection)
└── ui
    ├── components
    │   ├── AudioPlayerSheet.kt     (Persistent bottom playback controller)
    │   ├── CompatibilityCard.kt    (Status chips, technical explanation & guidance)
    │   ├── RecordingItemCard.kt    (Card with play/pause, rename, share, delete)
    │   └── WaveformVisualizer.kt   (Live animated amplitude visualizer)
    ├── dashboard
    │   ├── DashboardScreen.kt      (Bottom navigation host)
    │   ├── NormalCallsSection.kt   (Cellular recording dashboard)
    │   ├── RecordingsSection.kt    (Library with filtering, search, rename, delete)
    │   ├── SettingsSection.kt      (Audio config, storage info, OS evolution matrix)
    │   ├── WhatsAppVideoSection.kt (MediaProjection video consent & recording)
    │   └── WhatsAppVoiceSection.kt (VoIP audio recording & technical limits)
    └── viewmodel
        └── MainViewModel.kt        (StateFlow-driven presentation architecture)
```

---

## 5 Dashboard Sections

1. **Normal Calls**:
   - Live recorder with real-time waveform visualizer driven by microphone amplitude.
   - Optional phone number & contact tagging.
   - Start, Pause, Resume, Stop controls.
   - Speakerphone guidance notification banner.
   - Cellular capability cards (Hardware line vs Acoustic mic vs Call State).
   - Recent cellular recordings preview.

2. **WhatsApp Voice Calls**:
   - Dedicated VoIP audio capture controller.
   - Prominent disclaimer and technical breakdown explaining why `AudioPlaybackCapture` excludes `USAGE_VOICE_COMMUNICATION`.
   - Contact or chat tag assignment.
   - Start/Stop session with persistent foreground notification.

3. **WhatsApp Video Calls**:
   - MediaProjection consent initiation via system prompt.
   - Screen video + audio encoding into standard MP4.
   - Real-time recording duration timer.
   - Persistent notification with quick-stop action.

4. **Recordings**:
   - Room-backed persistent library in app-private scoped storage (`context.filesDir/recordings`).
   - Search bar (filtering by title, contact, phone number).
   - Filter chips: All, Normal Calls, WhatsApp Voice, WhatsApp Video.
   - Total count & total aggregated duration display.
   - Inline audio playback with bottom scrubber sheet.
   - Contextual actions: Rename, Delete (with confirmation dialog), Share (via FileProvider and Android Sharesheet).

5. **Settings & Compatibility**:
   - Audio Source configuration: `MIC` (Microphone default), `VOICE_COMMUNICATION` (Echo-cancelled VoIP), `VOICE_RECOGNITION` (Raw unsuppressed), `DEFAULT`.
   - Audio Format selection: `M4A (AAC High Quality)`, `3GP (Compact Voice)`, `AMR-WB (Speech Optimized)`.
   - App-private storage directory path inspector.
   - Battery-aware behavior documentation (services release wakelocks immediately upon stopping).
   - Android OS Evolution Matrix (breakdown of Android 9, 10, 11, 12, 13, 14, 15 changes).

---

## Testing & Verification

The project includes unit test coverage for the domain model, recording state machine, audio configurations, Room entities, and compatibility engine:

- `CallTypeTest`: Verifies MIME types, file extensions (`m4a`, `mp4`), and fallback lookup.
- `RecordingStateTest`: Verifies transitions between `Idle`, `Recording`, `Paused`, and `Error`.
- `AudioConfigTest`: Validates default parameters, sample rates (44.1 kHz), bitrates (128 kbps), and audio sources.
- `RecordingEntityTest`: Validates Room entity properties, default values, and schema integrity.
- `CompatibilityCheckerTest`: Tests that Android 9+ hardware restrictions, AudioPlaybackCapture VoIP restrictions, and MediaProjection screen video capabilities are flagged accurately.

### Test Execution Results
```
BUILD SUCCESSFUL
51 actionable tasks: 17 executed, 34 up-to-date
All 10 unit test cases executed and passed with 0 failures, 0 errors.
```

### Build Output
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk` (built with `assembleDebug`).
