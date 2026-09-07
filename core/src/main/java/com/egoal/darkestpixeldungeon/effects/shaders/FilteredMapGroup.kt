/* Darkest Pixel Dungeon Copyright (C) 2018-2026 contributors; GPL-3.0-or-later */
package com.egoal.darkestpixeldungeon.effects.shaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.watabou.glscripts.Script
import com.watabou.glwrap.Texture
import com.watabou.noosa.Game
import com.watabou.noosa.Group

class FilteredMapGroup : Group() {
    var filter: MapFilter? = null
        set(value) {
            if (field === value) return
            field = value
            shader?.dispose()
            shader = null
            shaderAttempted = false
        }

    private var buffer: FrameBuffer? = null
    private var batch: SpriteBatch? = null
    private var shader: ShaderProgram? = null
    private var shaderAttempted = false

    override fun update() {
        super.update()
        filter?.update(Game.elapsed)
    }

    override fun draw() {
        val activeFilter = filter ?: return super.draw()
        ensureResources(activeFilter)
        val target = buffer ?: return super.draw()
        val renderer = batch ?: return super.draw()

        target.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        super.draw()
        target.end()

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
        renderer.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, Game.width.toFloat(), Game.height.toFloat())
        renderer.shader = shader
        renderer.begin()
        if (shader != null) activeFilter.prepare(renderer.shader)
        renderer.draw(target.colorBufferTexture, 0f, 0f, Game.width.toFloat(), Game.height.toFloat(), 0f, 0f, 1f, 1f)
        renderer.end()
        renderer.shader = null
        Script.clearBinding()
        Texture.clear()
    }

    private fun ensureResources(activeFilter: MapFilter) {
        if (buffer == null || buffer!!.width != Game.width || buffer!!.height != Game.height) {
            buffer?.dispose()
            buffer = FrameBuffer(Pixmap.Format.RGBA8888, Game.width, Game.height, false)
        }
        if (batch == null) batch = SpriteBatch()
        if (!shaderAttempted) {
            shaderAttempted = true
            ShaderProgram.pedantic = false
            val compiled = ShaderProgram(VERTEX_SHADER, activeFilter.fragmentShader)
            if (!compiled.isCompiled) {
                Gdx.app.error("MapFilter", compiled.log)
                compiled.dispose()
            } else shader = compiled
        }
    }

    override fun destroy() {
        shader?.dispose()
        batch?.dispose()
        buffer?.dispose()
        shader = null
        shaderAttempted = false
        batch = null
        buffer = null
        super.destroy()
    }

    companion object {
        private val VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color = a_color;
                v_color.a = v_color.a * (255.0/254.0);
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
        """.trimIndent()
    }
}
