# Hakan Authenticator

Offline TOTP authenticator for Android, written from scratch. It produces the six- or eight-digit codes that sign-in pages ask for (RFC 6238) and never talks to the network. The UI follows the phone language: English by default, Turkish on a Turkish phone.

## What it does

- Adds an account by scanning its `otpauth://totp/...` QR code (CameraX + ZXing) or by typing the key.
- Shows every code with a countdown; tap copies the code (marked sensitive on Android 13+), long press renames or deletes.
- SHA1, SHA256 and SHA512, 6 or 8 digits, any period from 10 to 300 seconds.
- Exports and restores a password-encrypted backup file.

## Security model

| Layer | How |
|---|---|
| Network | No `INTERNET` permission in the manifest; the app cannot send anything. |
| Storage | Accounts live in one file sealed with AES-256-GCM. The key is generated inside the Android Keystore and never leaves it, so a copied file is useless. |
| App lock | Opening the app needs fingerprint, face or the screen lock (BiometricPrompt). It locks again as soon as it leaves the screen. |
| Screen | `FLAG_SECURE`: no screenshots, blank in the recent-apps preview. |
| Android backup | `allowBackup=false` and extraction rules exclude every domain. |
| Backup file | PBKDF2-HMAC-SHA256 (310,000 iterations) derives an AES-256-GCM key from the password; a wrong password or an edited file fails the GCM tag check. |

## Build

Requirements: JDK 17+, Android SDK with platform 35 (`local.properties` → `sdk.dir`).

```bash
./gradlew testDebugUnitTest   # RFC 6238 / RFC 4226 / RFC 4648 vectors, URI parsing, backup crypto
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

Release signing reads `~/.config/hakan-authenticator/signing.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Without it the release build is signed with the debug key. Updates must be signed with the same key, or Android refuses to install them over the old version.

## Install

Copy the APK to the phone and open it, or `adb install -r app-release.apk`.

## Layout

```
app/src/main/java/dev/hakantopcu/authenticator/
  core/   Base32, Totp (HOTP/TOTP), OtpAuthUri, Account, BackupCrypto   pure Kotlin, unit-tested on the JVM
  store/  VaultStore                                                    Keystore-sealed account file
  ui/     MainActivity (lock), AccountsScreen, AddAccountScreen, ScannerScreen, BackupDialog, Theme
```

## License

Private project. Open-source authenticators (Aegis, 2FAS, andOTP) were read for design ideas only; no code was copied.
