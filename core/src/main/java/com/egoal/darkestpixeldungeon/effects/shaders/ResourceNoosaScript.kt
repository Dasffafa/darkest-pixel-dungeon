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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.egoal.darkestpixeldungeon.effects.shaders

import com.badlogic.gdx.Gdx
import com.watabou.noosa.Camera
import com.watabou.noosa.NoosaScript

/** Loads a Noosa-compatible vertex/fragment pair from assets/shaders. */
abstract class ResourceNoosaScript protected constructor(name: String) : NoosaScript(loadSource(name)) {
    override fun camera(camera: Camera?) {
        // Camera matrices are mutable; reference equality cannot detect scrolling.
        resetCamera()
        super.camera(camera)
    }

    companion object {
        private fun loadSource(name: String): String {
            val vertex = Gdx.files.internal("shaders/$name.vert")
            val fragment = Gdx.files.internal("shaders/$name.frag")
            require(vertex.exists()) { "Vertex shader not found: ${vertex.path()}" }
            require(fragment.exists()) { "Fragment shader not found: ${fragment.path()}" }
            return vertex.readString() + "//\n" + fragment.readString()
        }
    }
}
