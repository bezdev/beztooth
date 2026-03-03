# Gradle Modernization Plan

## Context
Project uses Gradle 9.0 wrapper but AGP 7.2.2 + legacy `android.support` libraries. Gradle 9.0 removed `jcenter()` and is incompatible with AGP 7.x. Full migration needed.

## Target Versions
- **Gradle**: 8.9 (downgrade from 9.0 — AGP 8.7.x not yet certified for Gradle 9.0)
- **AGP**: 8.7.3
- **compileSdk / targetSdk**: 34
- **minSdk**: 26 (unchanged)
- **AndroidX** (replaces `android.support`)

## Steps

### 1. Gradle wrapper — downgrade to 8.9
- `gradle-wrapper.properties`: change distribution URL to `gradle-8.9-bin.zip`

### 2. `gradle.properties` — enable AndroidX
Add:
```
android.useAndroidX=true
android.enableJetifier=true
```

### 3. `settings.gradle` — add plugin management
Convert to:
```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
include ':app'
```

### 4. Root `build.gradle` — modernize
- Remove `buildscript` block and `allprojects` block
- Use plugin DSL:
```groovy
plugins {
    id 'com.android.application' version '8.7.3' apply false
}
```

### 5. `app/build.gradle` — update
- Add `namespace 'com.beztooth'`
- Update `compileSdk 34`, `targetSdk 34`
- Replace dependencies:
  - `com.android.support:appcompat-v7:26.1.0` → `androidx.appcompat:appcompat:1.6.1`
  - `com.android.support.constraint:constraint-layout:1.0.2` → `androidx.constraintlayout:constraintlayout:2.1.4`
  - `com.android.support.test:runner` → `androidx.test:runner:1.5.2`
  - `com.android.support.test.espresso:espresso-core` → `androidx.test.espresso:espresso-core:3.5.1`
- Update test runner: `androidx.test.runner.AndroidJUnitRunner`
- Update proguard ref: `proguard-android-optimize.txt`

### 6. Remove `package` from manifests
- Remove `package="com.beztooth"` from `app/src/main/AndroidManifest.xml` (replaced by `namespace`)

### 7. Migrate Java imports (9 files)
| Old | New |
|-----|-----|
| `android.support.v7.app.AppCompatActivity` | `androidx.appcompat.app.AppCompatActivity` |
| `android.support.v4.content.LocalBroadcastManager` | `androidx.localbroadcastmanager.content.LocalBroadcastManager` |
| `android.support.v7.widget.Toolbar` | `androidx.appcompat.widget.Toolbar` |
| `android.support.v7.widget.AppCompatButton` | `androidx.appcompat.widget.AppCompatButton` |

Files: `BluetoothActivity.java`, `ConnectionManager.java`, `ThermometerActivity.java`, `SyncClockActivity.java`, `KimchiActivity.java`, `GarageDoorActivity.java`, `DeviceActivity.java`, `DevicesActivity.java`, `BezButton.java`

### 8. Migrate XML layouts (8 files)
Replace `android.support.v7.widget.Toolbar` → `androidx.appcompat.widget.Toolbar` in all layout XMLs.

### 9. Update BLE permissions for SDK 31+
In main `AndroidManifest.xml`, add:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```
(The old BLUETOOTH/BLUETOOTH_ADMIN permissions are still needed for minSdk < 31.)

### 10. Build & verify
```bash
./gradlew assembleFullDebug
```

## Files Modified
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle.properties`
- `settings.gradle`
- `build.gradle`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- 8 layout XML files in `app/src/main/res/layout/`
- 9 Java source files

## Unresolved Questions
- Runtime permission handling for `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` (SDK 31+) may need code changes in `BluetoothActivity.java` — will flag during implementation if needed.
