package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.levels.lighting.LightSource
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Game
import com.watabou.noosa.NoosaScript
import com.watabou.noosa.Visual
import com.watabou.noosa.audio.Sample
import com.watabou.utils.PointF
import com.watabou.utils.Random
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

open class Beam private constructor(
    start: PointF,
    end: PointF,
    private val style: Style,
    private val duration: Float
) : Visual(0f, 0f, 0f, 0f) {

    data class Style(
        val outerColor: Int,
        val midColor: Int,
        val coreColor: Int,
        val baseWidth: Float,
        val jitterAmp: Float,
        val lightColor: Int
    )

    private var timeLeft = duration
    private var jitterTimer = 0f

    private val startPt = PointF(start)
    private val endPt = PointF(end)
    private val dirX: Float
    private val dirY: Float
    private val perpX: Float
    private val perpY: Float
    private val length: Float

    private val segments: Int
    private val jitterOffsets: FloatArray

    private val vertices: FloatBuffer
    private val indices: ShortBuffer
    private val texture: SmartTexture = TextureCache.createSolid(0xFFFFFFFF.toInt())

    init {
        val dx = end.x - start.x
        val dy = end.y - start.y
        length = max(1f, sqrt(dx * dx + dy * dy))
        dirX = dx / length
        dirY = dy / length
        perpX = -dirY
        perpY = dirX

        segments = max(6, min(36, ceil(length / 8f).toInt()))
        jitterOffsets = FloatArray(segments + 1)
        regenerateJitter()

        val vertexCount = (segments + 1) * 2
        vertices = ByteBuffer.allocateDirect(vertexCount * 4 * (java.lang.Float.SIZE / 8))
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val indexCount = segments * 6
        indices = ByteBuffer.allocateDirect(indexCount * (java.lang.Short.SIZE / 8))
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()

        for (i in 0 until segments) {
            val v0 = (i * 2).toShort()
            val v1 = (i * 2 + 1).toShort()
            val v2 = ((i + 1) * 2).toShort()
            val v3 = ((i + 1) * 2 + 1).toShort()
            indices.put(v0)
            indices.put(v1)
            indices.put(v2)
            indices.put(v1)
            indices.put(v3)
            indices.put(v2)
        }
        indices.position(0)

        Sample.INSTANCE.play(Assets.SND_RAY)
        emitTransientLights()
    }

    private fun emitTransientLights() {
        val level = Dungeon.level
        val samples = if (length > DungeonTilemap.SIZE * 3f) 3 else 2
        val lightDur = max(0.18f, duration * 0.6f)
        for (i in 0 until samples) {
            val t = if (samples == 1) 0.5f else i.toFloat() / (samples - 1)
            val wx = startPt.x + (endPt.x - startPt.x) * t
            val wy = startPt.y + (endPt.y - startPt.y) * t
            val tileX = (wx / DungeonTilemap.SIZE).toInt()
            val tileY = (wy / DungeonTilemap.SIZE).toInt()
            if (tileX in 0 until level.width() && tileY in 0 until level.height()) {
                level.addTransientLight(
                    LightSource.fromColor(
                        cell = tileX + tileY * level.width(),
                        rgb = style.lightColor,
                        radius = 3.2f,
                        intensity = 0.85f,
                        duration = lightDur
                    )
                )
            }
        }
    }

    private fun regenerateJitter() {
        jitterOffsets[0] = 0f
        jitterOffsets[segments] = 0f
        for (i in 1 until segments) {
            val t = i.toFloat() / segments
            val envelope = sin(PI * t).toFloat()
            jitterOffsets[i] = Random.Float(-1f, 1f) * style.jitterAmp * envelope
        }
    }

    override fun update() {
        super.update()

        jitterTimer += Game.elapsed
        if (jitterTimer >= JITTER_INTERVAL) {
            jitterTimer -= JITTER_INTERVAL
            regenerateJitter()
        }

        timeLeft -= Game.elapsed
        if (timeLeft <= 0f) {
            killAndErase()
        }
    }

    override fun draw() {
        super.draw()
        val progress = max(0f, timeLeft / duration)
        if (progress <= 0f) return

        val script = NoosaScript.get()
        texture.bind()
        script.uModel.valueM4(matrix)
        script.camera(camera)

        // Separate blending prevents alpha saturation/graying when multiple additive layers overlap
        Gdx.gl.glBlendFuncSeparate(
            GL20.GL_SRC_ALPHA, GL20.GL_ONE,
            GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA
        )

        drawStrokeLayer(script, style.outerColor, 0.30f * progress, style.baseWidth * 2.6f * progress)
        drawStrokeLayer(script, style.midColor, 0.58f * progress, style.baseWidth * 1.45f * progress)
        drawStrokeLayer(script, style.coreColor, 0.95f * progress, style.baseWidth * 0.55f * progress)

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawStrokeLayer(
        script: NoosaScript,
        color: Int,
        alpha: Float,
        maxHalfWidth: Float
    ) {
        if (maxHalfWidth <= 0.02f || alpha <= 0.01f) return

        vertices.position(0)
        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val taper = 0.25f + 0.75f * sin(PI * t).toFloat()
            val halfW = maxHalfWidth * taper

            val cx = startPt.x + dirX * (length * t) + perpX * jitterOffsets[i]
            val cy = startPt.y + dirY * (length * t) + perpY * jitterOffsets[i]

            vertices.put(cx + perpX * halfW)
            vertices.put(cy + perpY * halfW)
            vertices.put(0.5f)
            vertices.put(0.5f)

            vertices.put(cx - perpX * halfW)
            vertices.put(cy - perpY * halfW)
            vertices.put(0.5f)
            vertices.put(0.5f)
        }
        vertices.position(0)

        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        script.lighting(r, g, b, alpha, 0f, 0f, 0f, 0f)
        script.drawElements(vertices, indices, segments * 6)
    }

    class DeathRay(s: PointF, e: PointF) : Beam(
        s, e,
        Style(
            outerColor = 0xAA22FF,
            midColor = 0xEE66FF,
            coreColor = 0xFFFFFF,
            baseWidth = 3.5f,
            jitterAmp = 3.5f,
            lightColor = 0xCC55FF
        ),
        0.5f
    )

    class LightRay(s: PointF, e: PointF) : Beam(
        s, e,
        Style(
            outerColor = 0x44BBFF,
            midColor = 0xAAEEFF,
            coreColor = 0xFFFFFF,
            baseWidth = 3.5f,
            jitterAmp = 2.5f,
            lightColor = 0x88DDFF
        ),
        1.0f
    )

    class HealthRay(s: PointF, e: PointF) : Beam(
        s, e,
        Style(
            outerColor = 0x22DD66,
            midColor = 0x88FFAA,
            coreColor = 0xFFFFFF,
            baseWidth = 3.0f,
            jitterAmp = 2.0f,
            lightColor = 0x44FF88
        ),
        0.75f
    )

    class ThickDeathRay(s: PointF, e: PointF) : Beam(
        s, e,
        Style(
            outerColor = 0x9911EE,
            midColor = 0xDD55FF,
            coreColor = 0xFFFFFF,
            baseWidth = 6.5f,
            jitterAmp = 5.0f,
            lightColor = 0xBB44FF
        ),
        0.5f
    )

    class DarkRay(s: PointF, e: PointF) : Beam(
        s, e,
        Style(
            outerColor = 0x551188,
            midColor = 0x9933CC,
            coreColor = 0xEECCFF,
            baseWidth = 4.5f,
            jitterAmp = 4.0f,
            lightColor = 0x7722AA
        ),
        2.0f
    )

    companion object {
        private const val JITTER_INTERVAL = 1f / 22f
    }
}
