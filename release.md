# FlickShare v1.0.0

Initial Android client release notes. These notes describe the checked-in implementation and APK metadata; they do not imply that a GitHub Release has been published.

## [1.0.0] - 2026-10-22

### Added

- **File sharing:** Stage one or more files using Android's document picker, or receive files through the single-item and multi-item Android Share Sheet.
- **Text sharing:** Package shared links and notes as UTF-8 `.txt` files for upload to the desktop.
- **Transfer controls:** Send staged files using an on-screen card fling, Quick Throw, or Launch All from the Deck tab.
- **Motion gestures:** Support held-card wrist flicks, optional Hands-free Flick, and optional Tap to Share double-bump detection on devices with compatible sensors. Hands-free modes are off by default.
- **Receiver discovery:** Probe local IPv4 HTTP `/info` endpoints, select discovered receivers, remember the chosen desktop, and offer rescan, forget, and manual address/port controls.
- **Streaming uploads:** Send multiple `files` parts in one HTTP multipart request, reading file streams in 64 KiB chunks. Support unknown-length streams without buffering entire files.
- **Transfer feedback:** Show upload progress, transfer speed, completion details, error messages, and haptic feedback for launch actions.
- **Direction metadata:** Derive direction from a fling or wrist motion, or use optional compass calibration saved by receiver IP address. Send direction headers for receivers that support directional effects.
- **Interface:** Provide Throw, Deck, and Devices tabs with animated liquid-glass components, six accent presets, and an optional AMOLED dark theme.
- **Platform integration:** Declare local-network access for API 37+, accept shared content through a single-top activity, and support devices without motion sensors through touch-based controls.
- **Unit tests:** Include focused JUnit tests for flick triggering, bump discrimination, and throw-direction calculations.

## Release details

| Field | Value |
|-------|-------|
| Version name | `1.0.0` |
| Version code | `1` |
| Application ID | `com.flickshare.app` |
| Minimum Android version | Android 8.0 / API 26 |
| Compile / target SDK | API 37 |
| Existing APK | `app\release\app-release.apk` |
| APK metadata | `app\release\output-metadata.json` |
| Default receiver port | TCP `8765` |
| Transfer interface | HTTP `POST /upload`, multipart field `files` |

## Installation and first transfer

Install the existing APK on an Android API 26+ device, or build and install the debug variant from source:

```powershell
.\gradlew.bat :app:installDebug
```

Start a compatible desktop receiver separately and connect both devices to the same local network. Open FlickShare, allow local-network access when requested, and select a desktop in **Devices**. Use **Manual IP** if discovery does not find it. Stage a file and select **Quick Throw**.

See [readme.md](readme.md) for prerequisites, build instructions, configuration, and troubleshooting.

## Compatibility and limitations

- This folder supplies the Android sender, not the desktop receiver. Desktop save locations and visual effects depend on the receiver implementation.
- Transfers use cleartext HTTP; the Android client supplies no authentication credentials. Use a trusted LAN and do not expose the receiver publicly.
- Automatic discovery probes a local IPv4 `/24` range on port `8765`. It does not currently browse mDNS, despite the discovery manager's name. Other reachable addresses and ports need manual entry.
- A saved or manually entered receiver may be displayed as connected before background verification finishes. The desktop must actually be reachable for an upload to succeed.
- Gesture controls depend on hardware and are active only while the app is ready to send from the Throw tab. Quick Throw remains the no-motion alternative; keep the phone securely in hand.
- Direction calibration requires a rotation-vector sensor. It is keyed by IP address and may need to be repeated after an address change.
- The app is a foreground sender; it does not implement a background transfer service or resumable upload protocol.
- Percentage progress is approximate when a content provider does not supply file sizes.
- Release signing is not configured in the Gradle scripts. Newly built release APKs need appropriate signing before distribution.

## Release scope

The repository history available for these notes contains one initial commit, `377e2c9`, with no version tags or earlier release baseline. The entries above describe initial functionality, not fixes or upgrades from a previous published version. No license file is included in the repository.
