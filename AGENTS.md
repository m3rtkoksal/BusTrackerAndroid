# AGENTS.md

## Project overview

**BusTracker** (Play Store: **Shuttle Live**) is an Android-only Kotlin/Jetpack Compose app for shuttle drivers and passengers. Shared state lives in Firebase (Auth + Firestore); there is no backend server in this repo.

## Cursor Cloud specific instructions

### One-time VM prerequisites (not in update script)

The Android SDK is installed under `$HOME/Android/Sdk` with:

- `platform-tools`, `build-tools;36.0.0`, `platforms;android-36`, `emulator`
- AVD: `BusTracker_API34` (`system-images;android-34;google_apis;x86_64`)

`~/.bashrc` exports `ANDROID_HOME` and adds SDK tools to `PATH`.

Java 21 (OpenJDK) is the host JDK. Gradle 9.4.1 + AGP 9.2.1 are used via `./gradlew`.

### Update script behavior

The startup update script only ensures `local.properties` exists (gitignored) pointing at the SDK. It does **not** install the Android SDK or start services.

### Build / test / lint

All commands run from the repo root:

| Task | Command | Notes |
|------|---------|-------|
| Debug APK | `./gradlew assembleDebug -x lintDebug` | Produces `app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests | `./gradlew testDebugUnitTest` | JVM unit tests only |
| Lint | `./gradlew lint` | **Fails today** with 7 pre-existing errors (e.g. `MissingPermission` in `MotionActivityService.kt`). Use `-x lintDebug` for build-only checks. |
| Release AAB | `./gradlew bundleRelease` | Requires `keystore.properties` (see `keystore.properties.example`) |

Firebase config is committed in `app/google-services.json`. Optional `MAPS_API_KEY` can go in `local.properties`.

### Running the app

There is no web or desktop target. To run on device/emulator:

```bash
./gradlew installDebug -x lintDebug
adb shell am start -n com.mikatechnology.BusTracker/.MainActivity
```

**Emulator caveat:** This cloud VM has no `/dev/kvm`. The software emulator (`-accel off`) boots and installs APKs, but first Compose startup is very slow (minutes) and may trigger System UI / app ANRs. Prefer a physical device or a KVM-enabled machine for interactive UI work. Build + unit tests are reliable without an emulator.

### Emulator session (optional)

If an emulator is needed, start it in tmux (long-running):

```bash
tmux -f /exec-daemon/tmux.portal.conf new-session -d -s android-emulator
tmux -f /exec-daemon/tmux.portal.conf send-keys -t android-emulator:0.0 \
  'emulator -avd BusTracker_API34 -no-audio -gpu swiftshader_indirect -accel off -no-snapshot' C-m
```

Wait for `adb shell getprop sys.boot_completed` = `1` and `settings get global device_provisioned` = `1` before `adb install`.

### Google Sign-In / Firebase

Live Firebase project: `bustracker-717a3`. Google Sign-In on debug builds requires the debug SHA-1 registered in Firebase/Google Cloud. See `docs/ANDROID_GOOGLE_SIGNIN_FIX.md`.

### Key entry points

- `app/src/main/java/com/mikatechnology/BusTracker/MainActivity.kt` — launcher
- `app/src/main/java/com/mikatechnology/BusTracker/ui/AppRoot.kt` — auth/session routing
- `app/src/main/java/com/mikatechnology/BusTracker/data/repository/ShuttleStore.kt` — Firestore realtime sync
