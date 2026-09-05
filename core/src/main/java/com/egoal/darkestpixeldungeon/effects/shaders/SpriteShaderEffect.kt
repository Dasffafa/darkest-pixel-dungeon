/*
 * Darkest Pixel Dungeon Copyright (C) 2018-2026 contributors
 * GPL-3.0-or-later
 */
package com.egoal.darkestpixeldungeon.effects.shaders

import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.noosa.NoosaScript

/** A shader attached to one character sprite. Parameters may be animated in [update]. */
interface SpriteShaderEffect {
    fun update(sprite: CharSprite, elapsed: Float) = Unit
    fun prepare(sprite: CharSprite): NoosaScript
}
