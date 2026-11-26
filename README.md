# CallerX — Open-Source SIP/VoIP & Dual-SIM Dialer

CallerX is a privacy-first, open-source Android dialer combining dual-SIM `TelecomManager` calling, Linphone SIP/VoIP multi-account registration, call recording with Media3 playback, T9 contact search, SMS messaging, and AES-256-GCM encrypted backups.

## Features
- **Dual-SIM + Multi-SIP Calling**: Route calls over SIM1, SIM2, or any registered SIP account (`UDP`, `TCP`, `TLS`, `WSS`) with optional TLS + SRTP enforcement.
- **Smart Dialer & T9 Search**: Fast 3×4 dialpad with voicemail long-press (`1`), speed dials (`2–9`), clipboard copy/paste, and real-time T9 contact matching.
- **In-Call Experience**: Live `MM:SS` timer, Wi-Fi/5G/4G/3G badge, 1–5 bar jitter/packet-loss quality meter, mute, speaker, hold, DTMF keypad, conferencing, transfer, and call recording.
- **Call Recording & Media3 Player**: Foreground `MediaRecorder` service (`VOICE_CALL` with `VOICE_COMMUNICATION` fallback), legal consent prompt before first recording, and ExoPlayer playback (`0.5×–2×` speed).
- **Contacts & SMS**: Tag & note editor, total talk time tracking, `.vcf` vCard sharing, SMS inbox & threaded chat with quick replies.
- **Privacy & Security**: 6-digit salted SHA-256 PIN stored in `EncryptedSharedPreferences`, `BiometricPrompt` unlock, background auto-lock, block list + unknown caller rejection, and PBKDF2 (100,000 iterations) + AES-256-GCM `.callerx` encrypted backups (local, Google Drive `appDataFolder`, or Dropbox).
- **Theming**: 9 Material 3 color palettes, Light/Dark/System mode, and pure `#000000` AMOLED black mode.

## Build Instructions
1. Ensure Android SDK (compileSdk 36 / minSdk 26) and JDK 17+ are installed.
2. Sync Gradle (dependencies include `https://linphone.org/maven_repository` configured in `settings.gradle.kts`).
3. Run `gradle :app:assembleDebug` or build in Android Studio / AI Studio.

## License
CallerX is licensed under the **GNU General Public License v3.0 (GPLv3)** due to integration with the Linphone SDK (`org.linphone:linphone-sdk-android`).

## Legal Disclaimer (Call Recording)
Call recording laws vary by country and jurisdiction (one-party vs. all-party consent). CallerX displays a mandatory consent dialog before recording and requires users to obtain all legally required permissions before recording any call.
