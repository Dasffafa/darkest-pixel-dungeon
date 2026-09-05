/* Darkest Pixel Dungeon Copyright (C) 2018-2026 contributors; GPL-3.0-or-later */
package com.egoal.darkestpixeldungeon.effects.shaders

import com.badlogic.gdx.graphics.glutils.ShaderProgram

/** Full-map post-process filter. UI is rendered after this pass and is unaffected. */
interface MapFilter {
    val fragmentShader: String
    fun update(elapsed: Float) = Unit
    fun prepare(shader: ShaderProgram) = Unit
}

object IdentityMapFilter : MapFilter {
    override val fragmentShader = """
        #ifdef GL_ES
        precision mediump float;
        #endif
        varying vec4 v_color;
        varying vec2 v_texCoords;
        uniform sampler2D u_texture;
        void main() { gl_FragColor = texture2D(u_texture, v_texCoords) * v_color; }
    """.trimIndent()
}
