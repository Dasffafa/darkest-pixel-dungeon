/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Radish Pixel Dungeon
 * Copyright (C) 2024-2026 Radish Pixel Dungeon Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.egoal.darkestpixeldungeon.effects.shaders

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.glscripts.Script
import com.watabou.noosa.Game
import com.watabou.noosa.NoosaScript
import kotlin.math.max
import kotlin.math.min

/** Slice-and-Dice-style death effect adapted from Radish Pixel Dungeon. */
class CutDeathEffect(private val duration: Float = DEFAULT_DURATION) : SpriteShaderEffect {
    private var elapsed = 0f
    private var progress = 0f

    override fun update(sprite: CharSprite, elapsed: Float) {
        this.elapsed += elapsed
        progress = min(this.elapsed / max(duration, 0.001f), 1f)
        if (progress >= 1f) sprite.finishShaderDeath(this)
    }

    override fun prepare(sprite: CharSprite): NoosaScript =
        Script.use(CutDeathScript::class.java).also { shader ->
            val frame = sprite.frame()
            shader.prepare(frame.left, frame.top, frame.width(), frame.height(), progress)
        }

    companion object {
        const val DEFAULT_DURATION = 0.5f
    }
}

class CutDeathScript : ResourceNoosaScript("cut-death") {
    private val frame = uniform("uFrame")
    private val cutLine = uniform("uCutLine")
    private val cutProgress = uniform("uCutProgress")
    private val cutSide = uniform("uCutSide")

    fun prepare(left: Float, top: Float, width: Float, height: Float, progress: Float) {
        frame.value4f(left, top, width, height)
        cutLine.value4f(0.5f, 0.5f, 1f, 1f)
        cutProgress.value1f(progress)
        cutSide.value1f(1f)
    }
}

object DeathShaders {
    /** Must be called before the mob's normal `super.die(cause)` path. */
    @JvmStatic
    @JvmOverloads
    fun cut(target: Char, duration: Float = CutDeathEffect.DEFAULT_DURATION): Boolean {
        if (!target.hasSprite || target.sprite.parent == null) return false
        Game.runOnRenderThreadAndWait {
            if (target.sprite.parent != null) target.sprite.beginShaderDeath(CutDeathEffect(duration))
        }
        return target.sprite.hasPendingShaderDeath
    }
}
