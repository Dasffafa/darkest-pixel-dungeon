# LibGDX migration M0 baseline

This record freezes the static migration baseline. The currently installed
Android release is the runtime authority and can be compared directly with
migration builds when each stage is tested.

## Repository baseline

| Item | Baseline |
|---|---|
| Git commit | `e02e67275a20244401ab216dbf880d9d3e0262c7` |
| Git description | `v0.7.2-4-ge02e6727-dirty` |
| Branch | `master` |
| Android version | `versionName 0.7.2`, `versionCode 51` |
| Android SDK configuration | compile 34, target 23, min 23 |
| Android entry point | `com.egoal.darkestpixeldungeon.DarkestPixelDungeon` |
| Current modules | `core` (Android application), `SPD-classes` (Android library) |
| Graphics baseline | OpenGL ES 2.0, RGB565 surface, portrait by default |
| Runtime authority | The user's phone with the current release installed |
| Runtime comparison | Side-by-side checks defined in `libgdx-migration-test-standard.md` |

The baseline commit is authoritative. The working tree was already dirty when
this record was created; unrelated Eclipse metadata, particle work and local
development files are not part of the migration baseline or migration commits.

## Static dependency matrix

The initial scan covered `core/src/main/java` and
`SPD-classes/src/main/java`. It found 134 direct `android.*` import lines in 80
files. Counts are discovery aids, not permanent acceptance criteria; the exit
criteria are the module boundaries in the migration plan.

| Area | Current entry points | Target boundary | Planned stage |
|---|---|---|---|
| Host and lifecycle | `Game`, `DarkestPixelDungeon` inherit/use Android activity, `GLSurfaceView`, display and lifecycle APIs | LibGDX `ApplicationListener` plus Android launcher/platform support | M1-M2 |
| Input | `Touchscreen` consumes `MotionEvent`; `Keys` consumes `KeyEvent` | Platform-neutral Noosa events fed by LibGDX input processors | M2 |
| OpenGL | `glwrap`, `NoosaScript`, `Game`, effects/scenes use `GLES20`/`GL10`; 31 files match GL APIs | `Gdx.gl` and LibGDX buffers while preserving Noosa APIs | M3 |
| Texture and bitmap | `Texture`, `TextureCache`, `BitmapCache`, `SmartTexture`, `Atlas` use Android bitmap/canvas types | LibGDX `Pixmap`, texture and file APIs | M3-M4 |
| Geometry types | Noosa and game code use Android `Rect`, `RectF`, `SparseArray`, `FloatMath` | Java/LibGDX/platform-neutral utility types | M3-M4 |
| Files and saves | 19 files directly use Java files/streams or Android file methods; `Dungeon`, `GamesInProgress`, `Badges`, `Rankings`, `TopExceptionHandler` are critical | Platform-neutral save/data API with unchanged Bundle schema and filenames | M4 |
| Preferences | `Preferences` wraps Android `SharedPreferences` through `Game.instance` | LibGDX Preferences or platform support | M4 |
| Reflection | `Bundle`, `Dungeon`, `Generator`, `Bestiary`, `Mob` are the five initial reflection entry points | LibGDX reflection where platform neutrality requires it | M4 |
| Platform services | URL intents, logs, orientation, immersive mode, vibration, Firebase/Crashlytics and exception UI occur in core/Noosa | `PlatformSupport`; Android implementations remain in Android module | M1, M4 |
| Audio | `Music` uses `MediaPlayer`; `Sample` uses `SoundPool` and Android assets | LibGDX Music/Sound behind the existing Noosa API | M5 |
| Fonts and text input | `RenderedText` and bitmap caches use Android graphics; `InputDialog` uses `AlertDialog`/`EditText` | Dynamic font implementation plus platform text input | M6 |
| Native workaround | `FroyoGLES20Fix.java`, JNI source and armeabi/x86 `.so` files | Remove only after all references are gone | M3 |

Notable leakage inside game code includes Android logging imports that are
unrelated to gameplay. These still count as platform dependencies and must be
removed or routed through a neutral logger before `core` becomes neutral.

## Save compatibility surface

Current saves are Android private files written as Bundles. Preserve exact
filenames, serialized class names and Bundle field names. Backups are part of
the compatibility surface, not disposable implementation details. Save
compatibility will be exercised after M4 using a copy of a real current-version
save; migration tests must never be run against the only copy of user data.

## Runtime comparison policy

No pre-migration screenshot, behavior log or save archive is required. During
stage acceptance, compare the migration build against the current release on
the user's phone using the same language, orientation, font option, immersive
mode, volume and equivalent game state. The comparison cases, tolerances and
failure severity are defined in `dev/libgdx-migration-test-standard.md`.

## Source and license record

The modern reference was inspected read-only at Radish commit
`016cc2f56b9fd2654c6104688dd4bfebe9e076ad`. Its repository license is GPL-3.0,
and the inspected Shader, font and Android/Desktop platform candidates carry
Pixel Dungeon/Shattered Pixel Dungeon GPL notices; Radish-specific
`GlslShaderScript` also carries Radish attribution.

Before copying each file, record its exact source path and commit again because
the reference working tree changes independently. Preserve every existing GPL
header and add Radish attribution where present. This M0 check establishes
license compatibility but does not authorize copying unrelated modern gameplay.

## M0 closure checklist

- [x] Repository, Android configuration and module baseline recorded.
- [x] Static Android/GL/file/reflection/audio dependency matrix recorded.
- [x] Initial Shader/font/platform candidate licenses and source commit checked.
- [x] Current Android installation designated as the runtime authority.
- [x] Post-migration comparison and acceptance standard established.

M0 is complete only when every item above is checked.
