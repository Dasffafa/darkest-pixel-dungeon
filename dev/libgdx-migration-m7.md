# M7: Kotlin Shader and map-filter entry points

## Implemented scope

- `SpriteShaderEffect` attaches a Shader to one `CharSprite`. Its `update`
  callback runs every frame, so animation code can drive uniforms without
  changing `CharSprite.draw()`.
- `HslSpriteEffect` is the first resource-backed example. Shader sources live
  under `assets/shaders` and retain the Radish/Shattered attribution.
- `FilteredMapGroup` captures all dungeon-world layers into a LibGDX
  `FrameBuffer`, applies one `MapFilter`, and composites the result before UI
  elements are drawn.
- With no map filter selected, world layers draw directly and do not pay the
  framebuffer cost.

## Call sites

```kotlin
mob.sprite.shaderEffect = HslSpriteEffect(hue = 20f)
mob.sprite.shaderEffect = null

GameScene.mapFilter(IdentityMapFilter)
GameScene.mapFilter(null)
```

Custom animated effects implement `SpriteShaderEffect.update`. Custom map
filters implement `MapFilter.update` and `prepare` for time-varying uniforms.

## Dice Mage-style death Shader

The Cut death effect follows Radish's two-stage lifetime: it marks the sprite
before the normal actor death path, suppresses the ordinary death animation,
then removes the sprite when the 0.5 second Shader animation completes.

Call it immediately before `super.die(cause)`:

```kotlin
override fun die(cause: Any?) {
    if (shouldUseDiceMageDeathShader()) {
        DiceMageDeathShaders.cut(this)
    }
    super.die(cause)
}
```

The condition remains gameplay-owned. The Shader API deliberately does not
introduce Dice Mage classes, damage-type rules, rewards, or balance changes.

## Cold snow visual Shader

`ColdSnowParticles.Snow(pos)` still owns cell visibility, spawning and
recycling. Particle updates still calculate world-space fall and wind motion.
The dedicated `ColdSnowScript` now performs camera-facing flip, randomized and
depth-based scale, lifetime alpha, and the cold tint on the GPU. This keeps the
effect tied to dungeon cells without turning it into a screen-space filter.
Each flake receives a random tilted flip axis. Resource-backed Noosa shaders
also refresh their mutable camera matrix on every draw, so world-space effects
track camera scrolling and hero-follow movement.

## Runtime acceptance

The Agent did not run Gradle tasks. Android and Desktop testing must verify:

1. `IdentityMapFilter` leaves the map pixel-identical and does not affect UI.
2. The framebuffer result is not vertically inverted on either backend.
3. `HslSpriteEffect` follows sprite animation frames and preserves alpha.
4. Shader state survives pause/resume and scene reconstruction.
5. Clearing either effect restores the original direct rendering path.
