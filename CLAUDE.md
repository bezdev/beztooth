# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK (all flavors)
./gradlew assembleFullDebug      # Build specific flavor debug APK
./gradlew installFullDebug       # Build and install on connected device
./gradlew clean                  # Clean build artifacts
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests on device
```

Flavors: `full`, `thermometer`, `syncclock`, `garagedoor`, `counter`

## Architecture

Android BLE device control app with product flavors for different use cases.

**Core pattern**: Bound service (`ConnectionManager`) + `LocalBroadcastManager` events.

- **`ConnectionManager`** (bound Service) — central BLE manager handling scanning, connections, and GATT operations. Activities bind to it and receive events via local broadcasts.
- **`DeviceShadow`** — in-memory model of a connected device's GATT tree (services → characteristics → descriptors).
- **`BluetoothActivity`** (abstract) — base activity handling BLE permissions, Bluetooth enable prompts, and service binding. All feature activities extend this.

**UI layer** (`com.beztooth.UI`):
- `Beztooth` — main launcher (full flavor), shows feature grid
- `DevicesActivity` — scans/lists BLE devices
- `DeviceActivity` — detailed GATT service/characteristic view
- Feature activities: `ThermometerActivity`, `SyncClockActivity`, `GarageDoorActivity`, `KimchiActivity`, `CounterActivity`
- Custom components: `BezButton`, `BezContainer`, `BezAnimation`

**Product flavors** share `main/` source but each has its own `AndroidManifest.xml` declaring launcher activity. The `full` flavor includes all features.

**`Constants.java`** — maps standard Bluetooth GATT UUIDs (services, characteristics) to human-readable names, defines device MAC addresses, and characteristic read types (STRING, HEX, INTEGER, TIME, etc.).

## Key Details

- Min/Target/Compile SDK: 26 (Android 8.0)
- Uses legacy `android.support` libraries (not AndroidX)
- Threading: `Handler` + `ReentrantLock` for BLE callback synchronization
- Gradle plugin: `com.android.tools.build:gradle:7.2.2`
