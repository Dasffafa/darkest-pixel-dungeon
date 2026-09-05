/*
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.egoal.darkestpixeldungeon.effects.shaders

import com.badlogic.gdx.Gdx
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.glscripts.Script
import com.watabou.noosa.NoosaScript

class HslSpriteEffect(var hue: Float = 0f, var saturation: Float = 0f, var lightness: Float = 0f) : SpriteShaderEffect {
    override fun prepare(sprite: CharSprite): NoosaScript =
        Script.use(HslSpriteScript::class.java).also { it.setHsl(hue, saturation, lightness) }
}

class HslSpriteScript : ResourceNoosaScript("hsl-sprite") {
    private val hslLocation = uniform("uHsl").location()
    fun setHsl(hue: Float, saturation: Float, lightness: Float) {
        Gdx.gl.glUniform3f(hslLocation, hue, saturation, lightness)
    }
}
