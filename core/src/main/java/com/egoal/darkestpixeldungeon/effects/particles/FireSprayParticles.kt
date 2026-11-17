package com.egoal.darkestpixeldungeon.effects.particles

import com.watabou.noosa.particles.Emitter
import com.watabou.noosa.particles.PixelParticle
import com.watabou.utils.ColorMath
import com.watabou.utils.Random
import kotlin.math.PI
import kotlin.math.sin

class FireSprayParticle : PixelParticle() {

    private var baseSize = 5f

    init {
        acc.set(0f, -38f)
    }

    fun reset(x: Float, y: Float, vx: Float = Random.Float(-16f, 16f), vy: Float = Random.Float(-18f, 12f)) {
        revive()
        this.x = x
        this.y = y
        speed.set(vx, vy)
        lifespan = Random.Float(0.28f, 0.52f)
        left = lifespan
        baseSize = Random.Float(4.5f, 7.5f)
        size(baseSize * 0.5f)
        color(COLOR_HOT)
        am = 0.95f
    }

    override fun update() {
        super.update()
        val p = (left / lifespan).coerceIn(0f, 1f)
        // Expand quickly then shrink smoothly
        val sizeCurve = 0.25f + 0.85f * sin(PI * p).toFloat()
        size(baseSize * sizeCurve)

        val c = when {
            p > 0.72f -> ColorMath.interpolate(COLOR_YELLOW, COLOR_HOT, (p - 0.72f) / 0.28f)
            p > 0.35f -> ColorMath.interpolate(COLOR_ORANGE, COLOR_YELLOW, (p - 0.35f) / 0.37f)
            else -> ColorMath.interpolate(COLOR_RED, COLOR_ORANGE, p / 0.35f)
        }
        color(c)
        am = (p * 1.15f).coerceAtMost(0.95f)
    }

    companion object {
        private const val COLOR_HOT = 0xFFF4B8
        private const val COLOR_YELLOW = 0xFFCC33
        private const val COLOR_ORANGE = 0xFF6E14
        private const val COLOR_RED = 0xB82008

        @JvmField
        val FACTORY: Emitter.Factory = object : Emitter.Factory() {
            override fun emit(emitter: Emitter, index: Int, x: Float, y: Float) {
                val p = emitter.recycle(FireSprayParticle::class.java) as? FireSprayParticle
                p?.reset(x, y)
            }

            override fun lightMode(): Boolean = true
        }
    }
}

class FireSparkParticle : PixelParticle.Shrinking() {

    init {
        color(0xFFE066)
        acc.set(0f, 48f)
    }

    fun reset(x: Float, y: Float) {
        revive()
        this.x = x
        this.y = y
        speed.set(Random.Float(-36f, 36f), Random.Float(-42f, 18f))
        lifespan = Random.Float(0.22f, 0.44f)
        left = lifespan
        size = Random.Float(1.6f, 2.6f)
        am = 1f
    }

    override fun update() {
        super.update()
        val p = (left / lifespan).coerceIn(0f, 1f)
        color(ColorMath.interpolate(0xFF5511, 0xFFE877, p))
        am = p
    }

    companion object {
        @JvmField
        val FACTORY: Emitter.Factory = object : Emitter.Factory() {
            override fun emit(emitter: Emitter, index: Int, x: Float, y: Float) {
                val p = emitter.recycle(FireSparkParticle::class.java) as? FireSparkParticle
                p?.reset(x, y)
            }

            override fun lightMode(): Boolean = true
        }
    }
}

class FireSmokeParticle : PixelParticle() {

    private var startSize = 3.5f
    private var endSize = 8.5f

    init {
        color(0x2C2420)
        acc.set(0f, -12f)
    }

    fun reset(x: Float, y: Float) {
        revive()
        this.x = x
        this.y = y
        speed.set(Random.Float(-10f, 10f), Random.Float(-26f, -14f))
        lifespan = Random.Float(0.55f, 0.90f)
        left = lifespan
        startSize = Random.Float(3f, 4.5f)
        endSize = startSize + Random.Float(4f, 6f)
        size(startSize)
        am = 0.42f
    }

    override fun update() {
        super.update()
        val p = (left / lifespan).coerceIn(0f, 1f)
        size(startSize + (endSize - startSize) * (1f - p))
        am = 0.42f * p
    }

    companion object {
        @JvmField
        val FACTORY: Emitter.Factory = object : Emitter.Factory() {
            override fun emit(emitter: Emitter, index: Int, x: Float, y: Float) {
                val p = emitter.recycle(FireSmokeParticle::class.java) as? FireSmokeParticle
                p?.reset(x, y)
            }

            override fun lightMode(): Boolean = false
        }
    }
}
