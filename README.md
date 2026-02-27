# 🔒 Deep Focus

> A digital straitjacket for serious students. Lock in, watch your lecture, and don't escape until you've earned it.

Built by **Aditya Sankar Deb** for Class 12 board and BITSAT exam preparation.

---

## What It Does

Deep Focus locks you into a YouTube lecture until your timer runs out. Once a session starts:

- The **Home and Recents buttons are disabled** (Android Screen Pinning / Lock Task mode)
- **Notification sounds are silenced** (Do Not Disturb — Priority mode, so your lecture audio still plays)
- You **cannot leave the app** until the timer hits zero or you confirm the early exit dialog

It's not a productivity app with streaks and gamification. It's a hard wall between you and distraction.

---

## Features

- **YouTube URL input** — paste any YouTube lecture link
- **Video section selector** — fetches the video duration, then lets you choose exactly which portion to watch using start/end sliders with quick presets (Full, First Half, Second Half)
- **Screen Pinning (Lock Task)** — disables Home, Recents, and back navigation during the session
- **Do Not Disturb** — blocks notification sounds, media audio unaffected
- **Countdown timer** — small overlay in the bottom-left corner, counting down with a circular progress ring
- **Landscape mode** — automatically rotates to landscape when the session starts
- **Early exit** — a low-opacity ✕ button in the top-right corner triggers a confirmation dialog before releasing the lockdown
- **Privacy Policy dialog** — built into the home screen
- **Social links** — LinkedIn and Instagram footer

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| State | ViewModel + StateFlow + Coroutines |
| Navigation | Navigation Compose |
| Video Player | Android WebView (YouTube mobile page) |
| System APIs | NotificationManager (DND), Activity.startLockTask() |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

---

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Load YouTube video in WebView |
| `ACCESS_NOTIFICATION_POLICY` | Enable/restore Do Not Disturb during sessions |

No other permissions. No analytics. No accounts. No data collection of any kind.

---

## Project Structure

```
app/src/main/
├── java/com/aditya/deepfocus/
│   ├── MainActivity.kt               # Entry point + Navigation graph
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt         # URL input, section sliders, branding
│   │   │   └── FocusScreen.kt        # WebView player + timer overlay + lockdown
│   │   ├── viewmodel/
│   │   │   ├── HomeViewModel.kt      # URL validation, video duration fetch, section state
│   │   │   └── FocusViewModel.kt     # Countdown timer (rotation-safe)
│   │   ├── components/
│   │   │   └── SocialIconButton.kt   # Reusable icon button for social links
│   │   └── theme/
│   │       └── Theme.kt              # Dark academic color scheme
├── res/
│   ├── drawable/
│   │   ├── ic_linkedin.xml
│   │   ├── ic_instagram.xml
│   │   └── ic_launcher_foreground.xml
│   └── values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
└── AndroidManifest.xml
```

---

## Building from Source

### Prerequisites

- JDK 17+
- Android SDK (API 35)
- Gradle 8.7

### Steps

```bash
git clone https://github.com/adityasankardeb/deep-focus.git
cd deep-focus
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### CI/CD

This repo includes a GitHub Actions workflow (`.github/workflows/build.yml`) that automatically builds a debug APK on every push to `main`. Download the artifact from the **Actions** tab → latest run → **Artifacts** section.

---

## First Run Setup

1. Install the APK on your Android device
2. Open Deep Focus
3. Tap **Start Deep Work** — you'll be prompted to grant **Do Not Disturb** access
4. Tap **Open Settings**, grant the permission, and return to the app
5. Paste a YouTube URL, load the video info, select your section, and start your session

> **Note:** Screen Pinning is handled automatically by the app. No manual setup needed.

---

## How the Lockdown Works

When a session starts, the app calls `activity.startLockTask()`. This is Android's built-in kiosk/pinning API — it's the same mechanism used by exam and enterprise apps. It physically disables the Home button, Recents button, and status bar navigation at the OS level.

`stopLockTask()` is only called in two situations:
1. The countdown timer reaches `00:00`
2. The user taps the ✕ button and **confirms** they want to end early

DND is set to `INTERRUPTION_FILTER_PRIORITY` (not `FILTER_NONE`), which silences notification sounds and vibrations but leaves media audio — including your lecture — completely unaffected. The previous DND state is captured before the session and exactly restored when it ends.

---

### 📥 Get the App
**[Click Here to Download the Latest APK from Google Drive](https://drive.google.com/file/d/1yTK2SRAv5H6pCzvQNohHcqhq_3KzLQWF/view?usp=drivesdk)**


## Privacy Policy

This app requires Do Not Disturb and Screen Pinning permissions strictly for local focus sessions. These permissions are used only while a session is active and are immediately restored upon completion.

No personal data is collected, stored, or transmitted. No accounts, analytics, or third-party SDKs are used.

---

## Author

**Aditya Sankar Deb**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/aditya-sankar-deb-a276143b1)
[![Instagram](https://img.shields.io/badge/Instagram-E4405F?style=flat&logo=instagram&logoColor=white)](https://instagram.com/adityasankardeb_)

© 2026 All Rights Reserved Aditya Sankar Deb
