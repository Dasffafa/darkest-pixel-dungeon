/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.NoosaScript
import com.watabou.noosa.Visual
import com.watabou.utils.PointF
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class CircleArc(private val nTris: Int, private val rad: Float) : Visual(0f, 0f, 0f, 0f) {

    private var duration = 0f
    private var lifespan = 0f

    //1f is an entire 360 degree sweep
    var sweep = 1f
        set(value) {
            field = value
            dirty = true
        }

    private var dirty = false

    private var lightMode = true

    private val texture: SmartTexture = TextureCache.createSolid(0xFFFFFFFF.toInt())

    private val vertices: FloatBuffer
    private val indices: ShortBuffer

    init {
        vertices = ByteBuffer.allocateDirect((nTris * 2 + 1) * 4 * (java.lang.Float.SIZE / 8))
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        indices = ByteBuffer.allocateDirect(nTris * 3 * (java.lang.Short.SIZE / 8))
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()

        updateTriangles()
    }

    fun color(color: Int, lightMode: Boolean): CircleArc {
        this.lightMode = lightMode
        hardlight(color)

        return this
    }

    fun show(visual: Visual, duration: Float): CircleArc {
        point(visual.center())
        visual.parent.addToBack(this)

        this.lifespan = duration
        this.duration = duration

        return this
    }

    fun show(parent: Group, pos: PointF, duration: Float): CircleArc {
        point(pos)
        parent.add(this)

        this.lifespan = duration
        this.duration = duration

        return this
    }

    private fun updateTriangles() {
        dirty = false
        val v = FloatArray(4)

        indices.position(0)
        vertices.position(0)

        v[0] = 0f
        v[1] = 0f
        v[2] = 0.25f
        v[3] = 0f
        vertices.put(v)

        v[2] = 0.75f
        v[3] = 0f

        //starting position is very top by default, use angle to adjust this.
        val start = 2 * (Math.PI - Math.PI * sweep) - Math.PI / 2.0

        for (i in 0 until nTris) {
            var a = start + i * Math.PI * 2 / nTris * sweep
            v[0] = (cos(a) * rad).toFloat()
            v[1] = (sin(a) * rad).toFloat()
            vertices.put(v)

            a += Math.PI * 2 / nTris * sweep
            v[0] = (cos(a) * rad).toFloat()
            v[1] = (sin(a) * rad).toFloat()
            vertices.put(v)

            indices.put(0.toShort())
            indices.put((1 + i * 2).toShort())
            indices.put((2 + i * 2).toShort())
        }

        indices.position(0)
    }

    override fun update() {
        super.update()

        if (duration > 0) {
            lifespan -= Game.elapsed
            if (lifespan > 0) {
                sweep = lifespan / duration
            } else {
                killAndErase()
            }
        }
    }

    override fun draw() {
        super.draw()

        if (dirty) {
            updateTriangles()
        }

        if (lightMode) {
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            drawArc()
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        } else {
            drawArc()
        }
    }

    private fun drawArc() {
        val script = NoosaScript.get()

        texture.bind()

        script.uModel.valueM4(matrix)
        script.lighting(rm, gm, bm, am, ra, ga, ba, aa)

        script.camera(camera)
        script.drawElements(vertices, indices, nTris * 3)
    }
}
