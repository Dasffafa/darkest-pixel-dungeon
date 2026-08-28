# M1: first LibGDX Android vertical slice

## Source baseline

- Historical baseline: Shattered Pixel Dungeon `f10be84a` (`v0.7.4a`).
- Primary migration commit: `2a523f2e` (`v0.7.4b`).
- Source history is read from the local Radish repository only because that
  repository retains the original Shattered commits. No current Radish engine
  implementation is used in M1.
- DPD gameplay, assets, scene classes and save behavior remain authoritative.

## Implemented mapping

| DPD area | Historical mapping | M1 result |
|---|---|---|
| Gradle dependencies | Shattered `build.gradle`, `SPD-classes/build.gradle` | LibGDX 1.11.0 Android backend/core and Android native classifiers added; the historical version is not retained as a runtime dependency |
| Host lifecycle | `com.watabou.noosa.Game` | `AndroidApplication` + `ApplicationListener`; create/render/resize/pause/resume/dispose mapped |
| Input | `InputHandler`, `Keys`, `Touchscreen` | LibGDX input events queued and dispatched during the Noosa update step |
| GL wrappers | `glwrap/*` | Program, Shader, uniforms, attributes, buffers, textures and framebuffers use `Gdx.gl` |
| Matrix | `glwrap/Matrix` | Android matrix multiplication replaced by the historical Java implementation |
| Textures | `Texture`, `SmartTexture`, `TextureCache`, `BitmapCache` | Static assets use LibGDX Pixmap and internal files |
| Dynamic Android text | `RenderedText` | Android Canvas retained; historical ARGB Bitmap to RGBA Pixmap conversion used for upload |
| Audio | `Music`, `Sample` | LibGDX Music/Sound behind the existing Noosa APIs |
| DPD GL effects | DPD authoritative behavior | Existing blend/draw calls redirected to `Gdx.gl` without changing effect logic |

## Intentional DPD-preserving differences

- The first Shattered LibGDX commit remains the migration and behavior
  reference, while DPD targets LibGDX 1.11.0. Compatibility adjustments must
  preserve the historical behavior rather than pinning its decade-old runtime.
- DPD keeps its existing update-before-draw frame order. The first Shattered
  commit drew before calculating and applying the next update step.
- DPD keeps its existing scene `pause()`/`resume()` hooks and explicitly
  pauses/resumes audio until later lifecycle fixes are evaluated.
- Android private file APIs, preferences, intents, dynamic font generation and
  platform services remain Android-specific in M1. Their historical migration
  belongs to later stages.
- The obsolete Froyo GLES workaround remains present but unreferenced. Removal
  is deferred until the first Android build and rendering test pass.

## Static verification

- `git diff --check` passes.
- Direct `android.opengl.GLES20`, `GL10` and `GLUtils` calls were removed from
  active rendering code.
- A direct `javac` check of all `SPD-classes` Java sources against the locally
  cached Android and LibGDX 1.11 APIs reached only the existing Kotlin
  `GameMath` dependency. The environment has no standalone `kotlinc`, so a full
  mixed Java/Kotlin compile was not performed.
- No Gradle task was run by the Agent.

## Desktop migration note

The LWJGL3 Desktop module now has an initial launcher under `desktop/`, based
on historical commit `7670bc14`. It is not marked compilable yet: `Game` still
extends Android `AndroidApplication`, and `core` still contains Android
Activity/file-service calls. Completing the historical host split is required
before the Desktop build gate can pass.

## User build gate

Run the normal debug build and return the first complete error block if it
fails. Do not fix multiple later errors before the first one is understood.

```text
./gradlew :android:assembleDebug
./gradlew :desktop:classes
```

The module split follows the platform boundary introduced by Shattered
`2aaf18b0` and the Desktop launcher introduced by `7670bc14`. Text rendering
uses the FreeType-backed approach from `b985ed3b`, adjusted for LibGDX 1.11.0.
The Agent has only performed static checks; both commands remain user build
gates under the repository instructions.

## Desktop build and launch result

Verified on 2026-08-28 with LibGDX 1.11.0, LWJGL3 and Java 21:

- `:desktop:classes` completed with `BUILD SUCCESSFUL`.
- `:desktop:run` created the game window, loaded `data/database.cdb`, compiled
  the Noosa shaders and entered `WelcomeScene`.
- Desktop-specific fixes included Java-compatible Han regex syntax and removal
  of GLES precision qualifiers before compiling shaders on Desktop.
- OpenAL real-time-priority and Java 8 target warnings remain non-fatal.

## Desktop actor-thread texture parameter fix

Desktop S03 NPC interaction exposed a P0 native crash when actor-thread UI
construction called `SmartTexture.filter()` without a current OpenGL context.
DPD intentionally retains the historical actor thread, and window construction
can originate from that thread throughout NPC, item and level interactions.

The platform-neutral texture compatibility layer now records filter and wrap
changes made outside the render thread. The pending parameters are applied when
the texture is next bound on the render thread. This preserves DPD's existing
actor and callback ordering while keeping all OpenGL calls on the thread that
owns the LWJGL3 context. The fix is classified as `historical-migration`; it
does not alter gameplay behavior.

Static inspection covered all actor, item and level window entry points and the
shared `Window` -> `ShadowBox` -> `SmartTexture.filter()` construction path.
Runtime acceptance remains required for dialogue and quest windows on Desktop
and Android, including at least CatEgoal and Alchemist interactions.

After a successful install, execute Android test cases S01-S03 from
`libgdx-migration-test-standard.md`:

1. Cold launch to title/welcome; compare first frame, title layout and music.
2. Create a character and enter a fixed dungeon.
3. Move, pick up an item, open a door and complete one combat.
4. Confirm tap/drag/back behavior and the expected click/combat audio.
5. Compare systematic blur, pixel offsets, blend state and missing textures
   against the current App on the same phone.

Record device, settings, pass/fail cases, first stack trace or reproduction and
severity. M1 cannot close or be committed until this gate passes.
