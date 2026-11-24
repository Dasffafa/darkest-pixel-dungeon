package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.levels.lighting.LightSource
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
import kotlin.math.min
import kotlin.math.sin

/**
 * 移植自 Wayward Souls `Gelid Embrace` / `magVazarBolt` + `magVazarExplodeB`：
 * 发光并高频抖动的冰霜球（`miniBoltBall` + `frostBoltGlow`），持续散发小型冰星（`miniBoltSpark` / `frostBoltSpark`）与冷雾（`miniBoltDust`），
 * 并在结束或命中时爆发冰蓝冲击波与冰雾。
 */
class FrostOrbEffect private constructor(
    private var curX: Float,
    private var curY: Float,
    private val targetX: Float,
    private val targetY: Float,
    private val duration: Float,
    private val isHover: Boolean,
    private val onHit: (() -> Unit)?
) : Group() {

    private val startX = curX
    private val startY = curY
    private val mistGroup = Group()
    private val glowGroup = AdditiveGroup()
    private var elapsed = 0f
    private var emitAccum = 0f
    private var burstDone = false

    init {
        add(mistGroup)
        add(glowGroup)
        emitOrbFrame(initial = true)
    }

    override fun update() {
        super.update()
        val dt = Game.elapsed

        if (!burstDone) {
            elapsed += dt
            val t = min(1f, elapsed / duration)
            if (!isHover) {
                curX = startX + (targetX - startX) * t
                curY = startY + (targetY - startY) * t
            }

            emitAccum += dt
            while (emitAccum >= 0.03f) {
                emitAccum -= 0.03f
                emitOrbFrame(initial = false)
            }

            if (t >= 1f) {
                burstDone = true
                triggerBurst(curX, curY)
                onHit?.invoke()
            }
        } else if (mistGroup.countLiving() == 0 && glowGroup.countLiving() == 0) {
            killAndErase()
        }
    }

    private fun emitOrbFrame(initial: Boolean) {
        val jx = curX + Random.Float(-1.5f, 1.5f)
        val jy = curY + Random.Float(-1.5f, 1.5f)

        // frostBoltGlow: 外圈柔和冰蓝光晕
        val glow = glowGroup.recycle(FrostGlowParticle::class.java) as FrostGlowParticle
        glow.reset(jx, jy)

        // miniBoltBall: 发光抖动冰球核心
        val coreCount = if (initial) 3 else 2
        for (i in 0 until coreCount) {
            val ball = glowGroup.recycle(MiniBoltBallParticle::class.java) as MiniBoltBallParticle
            ball.reset(jx, jy)
        }

        // miniBoltSpark / frostBoltSpark: 散发小型冰晶粒子
        val spark = glowGroup.recycle(MiniBoltSparkParticle::class.java) as MiniBoltSparkParticle
        spark.reset(jx, jy)

        // miniBoltDust: 非加色冷灰蓝薄雾
        if (initial || Random.Int(2) == 0) {
            val dust = mistGroup.recycle(MiniBoltDustParticle::class.java) as MiniBoltDustParticle
            dust.reset(jx, jy)
        }
    }

    private fun triggerBurst(x: Float, y: Float) {
        for (i in 0 until 12) {
            val shock = glowGroup.recycle(UndeadEmergeEffect.ShadowShockParticle::class.java) as UndeadEmergeEffect.ShadowShockParticle
            shock.reset(x, y, 0x55CCFF)
        }
        for (i in 0 until 8) {
            val exp = glowGroup.recycle(FrostExplodeParticle::class.java) as FrostExplodeParticle
            exp.reset(x, y)
        }
        for (i in 0 until 10) {
            val spark = glowGroup.recycle(MiniBoltSparkParticle::class.java) as MiniBoltSparkParticle
            spark.reset(x, y, burst = true)
        }
        for (i in 0 until 4) {
            val dust = mistGroup.recycle(MiniBoltDustParticle::class.java) as MiniBoltDustParticle
            dust.reset(x, y)
        }
    }

    private class AdditiveGroup : Group() {
        override fun draw() {
            Gdx.gl.glBlendFuncSeparate(
                GL20.GL_SRC_ALPHA, GL20.GL_ONE,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA
            )
            super.draw()
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
    }

    class FrostGlowParticle : Image(UndeadEmergeEffect.softDiscTexture()) {
        private var left = 0.1f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            left = 0.1f
            x = cx - width / 2f
            y = cy - height / 2f
            hardlight(0x2266CC)
            alpha(0.55f)
            scale.set(16f / max(1f, width))
        }

        override fun update() {
            super.update()
            left -= Game.elapsed
            if (left <= 0f) {
                kill()
                return
            }
            alpha(0.55f * (left / 0.1f))
        }
    }

    class MiniBoltBallParticle : Image(UndeadEmergeEffect.softDiscTexture()) {
        private var lifespan = 0.12f
        private var left = 0.12f
        private var startSize = 8f
        private var endSize = 1.5f
        private var vx = 0f
        private var vy = 0f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            startSize = Random.Float(6f, 9.5f)
            endSize = Random.Float(1.2f, 2.2f)
            lifespan = Random.Float(0.06f, 0.14f)
            left = lifespan
            vx = Random.Float(-28f, 28f)
            vy = Random.Float(-28f, 28f)
            x = cx - width / 2f
            y = cy - height / 2f
            hardlight(0x99EEFF)
            alpha(0.95f)
            scale.set(startSize / max(1f, width))
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }
            x += vx * dt
            y += vy * dt
            val p = 1f - left / lifespan
            val s = (startSize + (endSize - startSize) * p) / max(1f, width)
            scale.set(s)
            hardlight(ColorMath.interpolate(0xBBEFFF, 0x2266EE, p))
            alpha(1f - p * 0.5f)
        }
    }

    class MiniBoltSparkParticle : PixelParticle.Shrinking() {
        fun reset(cx: Float, cy: Float, burst: Boolean = false) {
            revive()
            x = cx + Random.Float(-2f, 2f)
            y = cy + Random.Float(-2f, 2f)
            color(ColorMath.interpolate(0xCCEEFF, 0x3377FF, Random.Float(0f, 0.5f)))
            size(Random.Float(1.5f, 2.8f))
            lifespan = Random.Float(0.35f, 0.9f)
            left = lifespan

            val angle = Random.Float((PI * 2.0).toFloat())
            val spd = if (burst) Random.Float(36f, 72f) else Random.Float(16f, 38f)
            speed.set(cos(angle) * spd, sin(angle) * spd - Random.Float(4f, 14f))
            acc.set(-speed.x * 2.4f, 26f)
        }
    }

    class MiniBoltDustParticle : Image(UndeadEmergeEffect.softDiscTexture()) {
        private var lifespan = 0.6f
        private var left = 0.6f
        private var vx = 0f
        private var vy = 0f
        private var startSize = 4f
        private var endSize = 7f
        private var startAlpha = 0.35f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            val angle = Random.Float((PI * 2.0).toFloat())
            val spd = Random.Float(10f, 24f)
            vx = cos(angle) * spd
            vy = sin(angle) * spd * 0.65f - 4f
            startSize = Random.Float(3.5f, 5.5f)
            endSize = Random.Float(6f, 8.5f)
            startAlpha = Random.Float(0.24f, 0.42f)
            lifespan = Random.Float(0.3f, 0.85f)
            left = lifespan
            x = cx - width / 2f
            y = cy - height / 2f
            hardlight(0x6688AA)
            alpha(startAlpha)
            scale.set(startSize / max(1f, width))
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }
            val damp = Math.pow(0.06, dt.toDouble()).toFloat()
            vx *= damp
            vy *= damp
            x += vx * dt
            y += vy * dt

            val p = 1f - left / lifespan
            scale.set((startSize + (endSize - startSize) * p) / max(1f, width))
            alpha(startAlpha * (1f - p))
        }
    }

    class FrostExplodeParticle : Image(UndeadEmergeEffect.softDiscTexture()) {
        private var lifespan = 0.35f
        private var left = 0.35f
        private var vx = 0f
        private var vy = 0f
        private var startSize = 9f
        private var endSize = 1.5f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            val angle = Random.Float((PI * 2.0).toFloat())
            val spd = Random.Float(14f, 36f)
            vx = cos(angle) * spd
            vy = sin(angle) * spd - Random.Float(6f, 18f)
            startSize = Random.Float(7f, 11f)
            endSize = 1.5f
            lifespan = Random.Float(0.22f, 0.48f)
            left = lifespan
            x = cx - width / 2f
            y = cy - height / 2f
            hardlight(0xAADDFF)
            alpha(0.95f)
            scale.set(startSize / max(1f, width))
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }
            vy += 30f * dt
            x += vx * dt
            y += vy * dt
            val p = 1f - left / lifespan
            scale.set((startSize + (endSize - startSize) * p) / max(1f, width))
            hardlight(ColorMath.interpolate(0xCCEEFF, 0x2277EE, p))
            alpha(1f - p)
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun spawnOrb(cell: Int, duration: Float = 0.9f, onBurst: (() -> Unit)? = null) {
            Game.runOnRenderThread {
                val parent = CellEmitter.get(cell).parent ?: return@runOnRenderThread
                val c = DungeonTilemap.tileCenterToWorld(cell)
                parent.add(FrostOrbEffect(c.x, c.y - 3f, c.x, c.y - 3f, duration, true, onBurst))
                Dungeon.level.addTransientLight(
                    LightSource.fromColor(cell, 0x55CCFF, 3.5f, 0.8f, duration + 0.35f)
                )
            }
        }

        @JvmStatic
        @JvmOverloads
        fun launch(fromCell: Int, toCell: Int, speedTilesPerSec: Float = 12f, onHit: (() -> Unit)? = null) {
            Game.runOnRenderThread {
                val parent = CellEmitter.get(fromCell).parent ?: return@runOnRenderThread
                val s = DungeonTilemap.tileCenterToWorld(fromCell)
                val e = DungeonTilemap.tileCenterToWorld(toCell)
                val dist = PointF.distance(s, e) / DungeonTilemap.SIZE
                val dur = max(0.12f, dist / speedTilesPerSec)
                parent.add(FrostOrbEffect(s.x, s.y - 2f, e.x, e.y - 2f, dur, false) {
                    Dungeon.level.addTransientLight(
                        LightSource.fromColor(toCell, 0x55CCFF, 3.5f, 0.85f, 0.45f)
                    )
                    onHit?.invoke()
                })
            }
        }
    }
}
