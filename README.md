[简体中文](./README_zh.md)

## Darkest Pixel Dungeon(dpd)

a rougelike rpg, with randomly generated levels, items, enemies, and traps. see screenshots below.

based on the source code of [pixel dungeon](https://github.com/watabou/pixel-dungeon) and [shattered pixel dungeon](https://github.com/00-Evan/shattered-pixel-dungeon)

check [darkest pixel dungeon wiki](https://pixeldungeon.fandom.com/wiki/Mod-Darkest_Pixel_Dungeon)

download [latest version](https://github.com/egoal/darkest-pixel-dungeon/releases)

help on [translation](https://www.transifex.com/darkest-pixel-dungeon-localization/)

---
current features & highlights:
* pressure system.
* perk mechanics.
* reworked damage proc.
* new hero class: Sorceress, Exile
* new enemies.
* new artifacts, equipments, and many other items.
* new npc, quests.

background sound track are from (and thanks to) [yet another pixel dungeon](https://github.com/ConsideredHamster/YetAnotherPixelDungeon)

---
develop journal (in chinese) in [dev/dev-notes](./dev/dev-notes)

feel free to contact me, english& chinese is okay.

---
screenshots:

![](dev/screenshots/00.png)
![](dev/screenshots/05.png)

---

## Compilation

### Desktop Gradle Tasks

### Normal run

```bash
./gradlew :desktop:run
```

Launches the desktop build via LWJGL3, automatically injecting version info.

### Debug mode run

```bash
./gradlew :desktop:runDebug
```

Sets the system property:

```
dpd.debug=true
```

Heroes created while this option is on will have debug items enabled.

### Build an executable JAR

```bash
./gradlew :desktop:release
```

Produces a self-contained executable fat JAR with all runtime dependencies:

```
desktop/build/libs/darkest-pixel-dungeon-<version>-desktop.jar
```

Run it with:

```bash
java -jar desktop/build/libs/darkest-pixel-dungeon-<version>-desktop.jar
```

### Build a Windows standalone distribution

```bash
./gradlew :desktop:packrWindows
```

This task will:

1. Download the Windows x64 Temurin JDK 21.
2. Use `jdeps` to analyze the required Java modules.
3. Use `jlink` to create a trimmed Windows JRE.
4. Use Packr to generate a Windows executable.

The output is located at:

```
desktop/build/packr/dist/
```

If the download keeps failing due to network issues, download the JDK manually from [Adoptium Temurin releases](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64) and place the archive at `desktop/build/packr/windows-jdk.zip`. The version the author uses is the Windows JDK 21 zip. Note that no matter what OS you use, you should download the Windows JDK, since this task cross-compiles a Windows distribution.

---

### Android Builds

### Prerequisites

- Android Studio with the Android SDK installed (JDK is bundled or already configured).
- This project is integrated with Google Firebase for crash tracking. The google-services and Firebase plugins are applied unconditionally, so `android/google-services.json` must exist or any build will fail. The file is git-ignored (see `.gitignore`) and must be downloaded from the Firebase console and placed in `android/` yourself.
- Native LibGDX `.so` libraries are copied automatically during the build, no manual step is needed.

### Run the debug APK (default-apk)

1. Open the project root in Android Studio (it is a Gradle project; the Android module is `:android`).
2. Prepare an emulator (AVD) or connect a physical device with USB debugging enabled.
3. Select the `android` run configuration in the top toolbar.
4. Click Run (the green triangle). Android Studio executes `:android:assembleDebug`, signs it with the debug keystore, and installs the app (`com.egoal.darkestpixeldungeon`).
5. The debug APK is written to `android/build/outputs/apk/debug/android-debug.apk`.

### Build a new version with your own signature

No signing config is pre-configured for release builds, so use Android Studio's signing wizard:

1. Menu **Build → Generate Signed App Bundle or APK...**.
2. Choose **Android App Bundle** (for Play Store) or **APK**.
3. Pick your keystore: select an existing `.jks`/`.keystore` and enter the store password, key alias and key password — or click **Create new...** to generate a keystore first.
4. Select the **release** build variant.
5. Finish; the signed artifact is written to:
   - App Bundle: `android/build/outputs/bundle/release/android-release.aab`
   - APK: `android/build/outputs/apk/release/android-release.apk`

Version name/code come from `rootProject.ext.appVersionName` / `rootProject.ext.appVersionCode` in the root `build.gradle` — bump them before publishing a new version.