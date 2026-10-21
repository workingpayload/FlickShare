# FlickShare

[![Latest commit](https://img.shields.io/github/last-commit/workingpayload/FlickShare)](https://github.com/workingpayload/FlickShare/commits)

> Send files, links, and text from an Android phone to a compatible desktop receiver over your local network, using a screen gesture or Quick Throw.

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [Configuration](#configuration)
- [Project structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Background

FlickShare is a native Kotlin Android app built with Jetpack Compose and Material 3. Stage a payload, choose a desktop, and send it without uploading it to a cloud storage service.

Version **1.0.0** includes:

- Multi-file selection through Android's document picker and support for Android's Share Sheet.
- Shared text and links packaged as UTF-8 `.txt` files.
- On-screen drag/fling, Quick Throw, and optional motion-based triggers.
- Local receiver discovery, manual IP/hostname entry, and a remembered desktop.
- Streaming multipart uploads with progress, transfer speed, and completion details.
- Optional compass calibration for direction metadata understood by compatible receivers.
- A liquid-glass interface with Throw, Deck, and Devices tabs, six accent colors, and an AMOLED theme.

**This repository contains the Android sender only.** A compatible desktop receiver must be installed and running separately; no desktop server or server startup command is included here. See [v1.0.0 release notes](relese.md).

## Install

### Requirements

- Android **8.0 or later** (API 26+) on the phone.
- A compatible FlickShare desktop receiver reachable from the phone on the same local network.
- Receiver access through the desktop firewall, normally on TCP port **8765**.
- On Android API 37+, permission for local-network access when prompted.

Motion sensors are optional: Quick Throw and on-screen gestures do not require them.

### Install the existing APK

The repository includes `app\release\app-release.apk`, whose adjacent output metadata identifies version `1.0.0`, version code `1`, and application ID `com.flickshare.app`.

Copy the APK to your phone, open it, and allow installation from that source if Android prompts you. Only install APKs from a source you trust.

Alternatively, with Android SDK Platform-Tools installed and an authorized USB-debugging device connected:

```powershell
adb install -r .\app\release\app-release.apk
```

### Build from source

Install Android Studio, Android SDK Platform **37**, and a JDK compatible with Android Gradle Plugin **9.3.3** and Gradle **9.8.1**. The project targets Java 17 bytecode; the JDK running Gradle must also meet the build tools' requirements.

```powershell
git clone https://github.com/workingpayload/FlickShare.git
Set-Location FlickShare
.\gradlew.bat :app:assembleDebug
```

Open the project in Android Studio to configure your local SDK location. Check `local.properties` before building: its `sdk.dir` must point to the SDK on your machine, not another developer's installation.

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`. Install it on a connected device with:

```powershell
.\gradlew.bat :app:installDebug
```

To compile the release variant:

```powershell
.\gradlew.bat :app:assembleRelease
```

The Gradle configuration does not define release signing. Configure your own signing credentials outside source control before distributing a newly built release APK; do not assume it matches the signing identity of the existing APK.

## Usage

### Send files from the app

1. Start the compatible desktop receiver and connect the phone and desktop to the same local network.
2. Open FlickShare and grant local-network access if prompted.
3. In **Devices**, select a discovered desktop, or choose **Manual IP** and enter its address and port.
4. Tap **Pick Files to Throw** or the **+** action to select one or more files.
5. Use **Quick Throw**, or fling the staged card on screen.
6. Watch the upload progress and the completion summary. Check the receiver for the delivered files.

The **Deck** tab lists staged files, lets you add or clear the payload, and provides **Launch All**. The selected receiver is remembered between app launches.

### Send a link or a batch from another app

In a browser, use **Share** and choose **FlickShare** to stage a link as a text file. In a file manager or gallery, select multiple items and share them to FlickShare. Review the staged payload, then use **Quick Throw** to send it.

### Optional gestures and direction

While holding the staged card, a wrist-flick gesture can trigger a transfer on devices with motion sensors. **Hands-free Flick** and **Tap to Share** are separate opt-in settings in Devices; both are off by default. Motion triggers are armed only on the Throw tab with a staged payload and no active upload or setup dialog.

Keep the phone securely in your hand; "throw" refers to sending the payload, not releasing the device. Quick Throw is the no-motion alternative.

On phones with a rotation-vector sensor, connect a desktop, point the phone toward it, and select **Point at PC** under **Throw Direction**. Calibration is saved by receiver IP address. Without usable calibration, the app uses gesture direction when available. Desktop-side directional effects require receiver support.

## Configuration

No application environment variables, account, or API key are required.

| Setting | Default | Where to change it |
|---------|---------|--------------------|
| Receiver address | Automatically selected or restored from saved preferences | Devices / Manual IP |
| Receiver port | `8765` for automatic discovery | Devices / Manual IP for another port |
| Accent | Coral | Devices; Coral, Teal, Azure, Lavender, Mint, or Amber |
| AMOLED dark theme | Off | Devices |
| Hands-free Flick | Off | Devices; requires a motion sensor |
| Tap to Share | Off | Devices; requires an accelerometer |
| Throw direction calibration | Not configured | Devices / Throw Direction |
| Android SDK location | Machine-specific | `sdk.dir` in `local.properties` |

For command-line builds, `JAVA_HOME` can select the JDK and `ANDROID_HOME` can identify the SDK. They are build-tool configuration, not runtime app settings.

### Receiver compatibility and network privacy

Despite the `MdnsDiscoveryManager` class name, the current implementation discovers receivers by probing HTTP `/info` endpoints on a local IPv4 `/24` range, not by browsing mDNS services. Automatic scans use port `8765`; use manual entry for other ports or addresses outside that range.

| Interface | What the Android client expects or sends |
|-----------|------------------------------------------|
| `GET /info` | HTTP 200 with a JSON object containing `"status": "ok"`; optional `hostname` and `port` identify the receiver |
| `POST /upload` | `multipart/form-data` with one or more parts named `files`; a successful HTTP status marks the transfer successful |
| `X-FlickShare-Client` | `Android/1.0` |
| `X-FlickShare-Direction` | Optional angle in degrees |
| `X-FlickShare-Direction-Source` | Optional direction-source identifier |

File content streams in 64 KiB chunks rather than loading each entire file into memory. Shared text is held in memory.

**Transfers use unencrypted HTTP, and the Android client sends no authentication credentials.** Use only a trusted local network. Do not expose the receiver directly to the internet. A remembered or manually selected desktop can appear connected before its background reachability check completes.

## Project structure

```text
app\
  build.gradle.kts                 Android app settings and dependencies
  src\main\AndroidManifest.xml     Permissions, launcher, and share targets
  src\main\java\com\flickshare\app\
    MainActivity.kt                Lifecycle, share intents, staging, uploads
    discovery\                     Receiver discovery and saved endpoint
    gesture\                       Flick/bump detection and direction mapping
    network\                       Multipart file streaming and progress
    ui\                            Compose screens, themes, glass components
  src\main\res\                    Fonts, icons, strings, network policy
  src\test\                       Gesture and direction unit tests
  release\                        Existing release APK and output metadata
gradle\wrapper\                    Gradle wrapper configuration
build.gradle.kts                   Plugin versions
settings.gradle.kts                Repositories and the single :app module
```

The build uses Kotlin's Compose plugin `2.3.20`, Compose BOM `2026.02.01`, coroutines, OkHttp, Haze, and Kyant Backdrop. Compile and target SDK are both `37`.

## Troubleshooting

| Symptom | What to check |
|---------|---------------|
| No desktop found | Receiver is running; both devices can reach each other; guest Wi-Fi/client isolation and firewall rules are not blocking LAN traffic |
| Discovery misses a receiver | Use Manual IP; automatic discovery only scans a local IPv4 `/24` range on port `8765` |
| Remembered device fails to receive | Rescan or update its IP and port; saved selection is not proof the receiver is online |
| Local-network permission error | Allow local-network/Nearby devices access in Android settings, then retry |
| Motion controls unavailable | Use Quick Throw; sensor availability varies by device |
| Direction calibration unavailable or inaccurate | Check sensor availability and recalibrate; an IP change requires new calibration |
| SDK/build setup fails | Verify Platform 37, `local.properties`, and a JDK compatible with the pinned Gradle and Android plugin versions |
| APK update reports a signature conflict | Use an APK signed with the installed app's key; uninstalling first removes app-local settings and data |

## Contributing

Fork the repository, create a focused branch, and include tests for behavior changes. Existing JUnit 4 tests cover flick detection, bump detection, and throw-angle calculations.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

For UI, networking, permissions, or sensor changes, also exercise the affected flow on a device with a compatible receiver. Open a pull request against the repository's default branch. Do not commit signing keys, credentials, machine-specific configuration, or generated build output.

## License

No license file is present in this repository at the time of writing. No SPDX license identifier is declared here; do not assume an open-source license or redistribution permission.
