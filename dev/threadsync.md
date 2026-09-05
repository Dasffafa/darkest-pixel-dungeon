# Actor / Render Thread Synchronization

## Scope

Darkest Pixel Dungeon has two long-lived execution contexts:

- **Render thread**: the LibGDX application thread. It runs `Game.render()`, updates scenes, and owns the Noosa scene graph and graphics resources.
- **Actor thread**: the gameplay logic worker started by `GameScene`. It runs `Actor.process()` and advances heroes, mobs, buffs, traps, items, and dungeon state.

The central rule is: **game state is owned by the Actor thread; visual state is owned by the Render thread.** Cross-thread work must be one-way and asynchronous whenever possible.

## Use the Actor Thread for

Actor-thread code is responsible for deterministic gameplay and model changes:

- Hero, mob, buff, trap, plant, item, heap, and dungeon logic.
- Movement, combat, damage, status effects, AI, random numbers, and turn scheduling.
- Updating positions, hit points, inventory contents, trap activation state, terrain data, and other serializable model fields.
- Saving or preparing game data, provided the operation does not touch Noosa or LibGDX graphics objects.
- Emitting gameplay messages through `GLog.*`. `GLog` queues UI delivery asynchronously.

Actor-thread code must not construct, measure, or mutate render objects.

## Use the Render Thread for

Render-thread code is responsible for presentation and LibGDX resources:

- Creating, measuring, laying out, showing, or destroying `Window`, `RenderedText`, `RenderedTextMultiline`, sprites, emitters, tweens, scene groups, and map visual objects.
- Accessing `Gdx.gl`, textures, fonts, audio/visual scene resources, and Noosa `Group`/`Gizmo` membership.
- Updating `GameScene`, `GameLog`, fog, map tilemaps, UI panes, status bars, and animations.
- Processing callbacks posted with `Game.runOnRenderThread { ... }`.

The render thread may read stable model data for drawing. It must not perform gameplay mutations that require Actor ordering; post a command to the Actor system instead.

## Cross-Thread Rules

1. **Never synchronously wait for the other thread from gameplay code.** Do not call `runOnRenderThreadAndWait` from Actor code. A render callback must never wait for the Actor thread.
2. **Prefer asynchronous handoff.** Use `Game.runOnRenderThread { ... }` for visual updates. Capture immutable values (usually strings, ids, positions, or colors), not mutable game objects when possible.
3. **Keep callbacks short and exception-safe.** Check that the scene/object is still alive before touching it; scenes can be destroyed before a queued callback executes.
4. **Separate model and view updates.** Change the model immediately on Actor thread, then enqueue only the visual consequence.
5. **Signals are not automatically thread-safe.** A signal listener may construct UI. Dispatch such signals on the Render thread unless every listener is guaranteed to be model-only.
6. **Do not hold locks while posting or waiting.** In particular, never hold an Actor, scene, or collection monitor while calling a render handoff.
7. **Avoid sharing mutable collections.** Actor-owned collections should be copied to immutable snapshots before render use; render-owned collections must only be changed on Render thread.
8. **Do not use sleeps as synchronization.** Use explicit queues, volatile state, or lifecycle callbacks.

## Approved Patterns

### Gameplay event with visual feedback

```java
// Actor thread
hero.takeDamage(amount);
GLog.w("The trap strikes!"); // GLog posts GameLog work asynchronously
Game.runOnRenderThread(() -> {
    if (GameScene.scene != null) {
        // create or update visual objects here
    }
});
```

### Model first, then visual update

```kotlin
// Actor thread
trap.active = false
Game.runOnRenderThread {
    if (trap.hasSprite && trap.sprite.parent != null) {
        trap.sprite.reset(trap)
    }
}
```

### Render-to-Actor command

UI input should enqueue or invoke a gameplay action; the action is resolved by the Actor system so turn ordering remains deterministic. Do not mutate hero or dungeon state directly from a UI callback unless the existing action API explicitly does so.

## Forbidden Patterns

```kotlin
// Forbidden on Actor thread: can deadlock when Render is waiting for Actor.
Game.runOnRenderThreadAndWait { gameLog.addText(text) }

// Forbidden: scene graph mutation from Actor thread.
sprite.visible = true
sprite.parent.add(tweener)
GameScene.show(window)
```

Use asynchronous render handoff instead. The only acceptable use of `runOnRenderThreadAndWait` is code that is proven to already run outside Actor processing and cannot be reached while Render waits for Actor; document that proof next to the call.

## Logging and Error Reporting

- `GLog.*` is safe from Actor code because UI dispatch is asynchronous; keep message formatting/model reads on Actor thread and render construction in `GameLog`.
- The watchdog monitors both Render and Actor progress. A timeout report captures all thread stacks, writes `error.dat`, and reports through `DarkestPixelDungeon.reportException`.
- A timeout is reported once per watchdog lifetime. Do not add retry loops that could produce repeated crash reports.

## Code Review Checklist

- Does this code run from `Actor.process()` or a gameplay callback?
- Does it touch a `Sprite`, `Group`, `Window`, `RenderedText`, emitter, tweener, tilemap, `Gdx.gl`, or other LibGDX object? If yes, move that part to Render thread.
- Does it call `runOnRenderThreadAndWait`, `CountDownLatch.await`, `join`, or wait/notify? Reject unless the call is outside Actor processing and the no-cycle guarantee is documented.
- Are cross-thread values immutable snapshots?
- Can the scene be destroyed before the queued callback runs?
- Is gameplay state changed before visual work is queued?
- Could a Render callback call back into Actor synchronously?

When uncertain, keep the operation on the Actor thread only if it is pure model logic; otherwise enqueue a short asynchronous Render callback.
