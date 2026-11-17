package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.noosa.particles.PixelParticle
import com.watabou.utils.ColorMath
import com.watabou.utils.PointF
import com.watabou.utils.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class MagicBurst private constructor(
    center: PointF,
    private val color: Int,
    private val radius: Float,
    private val duration: Float = 0.22f
) : Group() {

    private val halo = RadialGlow(center, color, radius, duration)
    private val core = RadialGlow(center, 0xFFFFFF, radius * 0.48f, duration * 0.72f)

    init {
        add(halo)
        add(core)

        val sparkCount = Random.IntRange(8, 12)
        for (i in 0 until sparkCount) {
            val spark = recycle(BouncingSpark::class.java) as BouncingSpark
            spark.reset(center.x, center.y, color)
        }
    }

    override fun update() {
        super.update()
        if (!halo.alive && !core.alive && countLiving() == 0) {
            killAndErase()
        }
    }

    override fun draw() {
        Gdx.gl.glBlendFuncSeparate(
            GL20.GL_SRC_ALPHA, GL20.GL_ONE,
            GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA
        )
        super.draw()
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private class RadialGlow(
        private val center: PointF,
        rgb: Int,
        private val targetRadius: Float,
        private val duration: Float
    ) : Image(TextureCache.createRadialGlow(32)) {

        private var timeLeft = duration

        init {
            hardlight(rgb)
            origin.set(width * 0.5f, height * 0.5f)
            updateTransform(1f)
        }

        override fun update() {
            super.update()
            timeLeft -= Game.elapsed
            if (timeLeft <= 0f) {
                killAndErase()
                return
            }
            updateTransform(timeLeft / duration)
        }

        private fun updateTransform(p: Float) {
            val expand = 0.55f + 0.80f * (1f - p)
            val s = (targetRadius * 2f * expand) / max(1f, width)
            scale.set(s)
            x = center.x - origin.x
            y = center.y - origin.y
            alpha((p * p * 0.95f).coerceIn(0f, 1f))
        }
    }

    class BouncingSpark : PixelParticle() {
        private var groundY = 0f
        private var bounced = false

        fun reset(x: Float, y: Float, color: Int) {
            revive()
            this.x = x
            this.y = y
            this.groundY = y + Random.Float(2f, 6f)
            this.bounced = false

            val angle = Random.Float(0f, (PI * 2.0).toFloat())
            val spd = Random.Float(28f, 68f)
            speed.set(cos(angle) * spd, sin(angle) * spd - 16f)
            acc.set(0f, 130f)

            lifespan = Random.Float(0.18f, 0.34f)
            left = lifespan
            size(Random.Float(1.5f, 2.5f))
            color(ColorMath.interpolate(color, 0xFFFFFF, 0.45f))
            am = 1f
        }

        override fun update() {
            super.update()
            if (!bounced && y >= groundY && speed.y > 0f) {
                y = groundY
                speed.y = -speed.y * 0.48f
                speed.x *= 0.72f
                bounced = true
            }
            val p = (left / lifespan).coerceIn(0f, 1f)
            am = p
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun spawn(group: Group?, cell: Int, color: Int, radius: Float = 16f) {
            if (group == null || !Dungeon.level.insideMap(cell)) return
            group.add(MagicBurst(DungeonTilemap.tileCenterToWorld(cell), color, radius))
        }
    }
}
