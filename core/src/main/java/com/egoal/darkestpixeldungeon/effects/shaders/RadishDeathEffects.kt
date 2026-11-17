package com.egoal.darkestpixeldungeon.effects.shaders

import com.badlogic.gdx.Gdx
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.glscripts.Script
import com.watabou.noosa.Game
import com.watabou.noosa.NoosaScript
import com.watabou.noosa.particles.Emitter
import com.watabou.noosa.particles.PixelParticle
import com.watabou.utils.Random
import kotlin.math.max
import kotlin.math.min

enum class DeathShaderType {
    CUT,
    BURN,
    ELLIPSE,
    ACID,
    WIPE,
    ALPHA,
    SINGULARITY,
    NOISE,
    LIGHT_DISSOLVE,
    LIGHT_EVAPORATE
}

class CutScript : ResourceNoosaScript("cut") {
    private val uFrame = uniform("uFrame")
    private val uTime = uniform("uTime")
    private val uAlpha = uniform("uAlpha")
    private val uCutLine = uniform("uCutLine")
    private val uCutAlpha = uniform("uCutAlpha")
    private val uCutSide = uniform("uCutSide")

    fun prepare(left: Float, top: Float, width: Float, height: Float, elapsed: Float, progress: Float) {
        uFrame.value4f(left, top, width, height)
        uTime.value1f(elapsed)
        uAlpha.value1f(1f)
        uCutLine.value4f(0.5f, 0.5f, 1f, 1f)
        uCutAlpha.value1f(progress)
        uCutSide.value1f(1f)
    }
}

class BurnScript : ResourceNoosaScript("burn") {
    private val uFrame = uniform("uFrame")
    private val uTime = uniform("uTime")
    private val uAlpha = uniform("uAlpha")
    private val uBounds = uniform("uBounds")
    private val uBurnProgress = uniform("uBurnProgress")
    private val uRandom = uniform("uRandom")

    fun prepare(left: Float, top: Float, width: Float, height: Float, elapsed: Float, progress: Float, random: Float) {
        uFrame.value4f(left, top, width, height)
        uTime.value1f(elapsed)
        uAlpha.value1f(1f)
        uBounds.value4f(0f, 0f, 1f, 1f)
        uBurnProgress.value1f(progress)
        uRandom.value1f(random)
    }
}

class EllipseScript : ResourceNoosaScript("ellipse") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")
    private val uBounds = uniform("uBounds")
    private val uRandom = uniform("uRandom")

    fun prepare(left: Float, top: Float, width: Float, height: Float, progress: Float, random: Float) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(progress)
        uBounds.value4f(0f, 0f, 1f, 1f)
        uRandom.value1f(random)
    }
}

class HslShaderScript : ResourceNoosaScript("hsl") {
    private val hslLocation = uniform("uHsl").location()

    fun prepare(h: Float, s: Float, l: Float) {
        Gdx.gl.glUniform3f(hslLocation, h, s, l)
    }
}

class SingularityScript : ResourceNoosaScript("singularity") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")
    private val uBounds = uniform("uBounds")
    private val uScaleX = uniform("uScaleX")
    private val uScaleY = uniform("uScaleY")

    fun prepare(left: Float, top: Float, width: Float, height: Float, progress: Float) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(progress)
        uBounds.value4f(0f, 0f, 1f, 1f)
        val s = max(0f, 1f - progress)
        uScaleX.value1f(s)
        uScaleY.value1f(s)
    }
}

class AcidScript : ResourceNoosaScript("acid") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")
    private val uRandom = uniform("uRandom")

    fun prepare(left: Float, top: Float, width: Float, height: Float, progress: Float, random: Float) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(progress)
        uRandom.value1f(random)
    }
}

class WipeScript : ResourceNoosaScript("wipe") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")
    private val uBounds = uniform("uBounds")
    private val uDirection = uniform("uDirection")

    fun prepare(left: Float, top: Float, width: Float, height: Float, progress: Float) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(progress)
        uBounds.value4f(0f, 0f, 1f, 1f)
        uDirection.value2f(1f, 0f)
    }
}

class AlphaScript : ResourceNoosaScript("alpha") {
    private val uAlpha = uniform("uAlpha")
    private val uBounds = uniform("uBounds")

    fun prepare(progress: Float) {
        uAlpha.value1f(progress)
        uBounds.value4f(0f, 0f, 1f, 1f)
    }
}

class NoiseScript : ResourceNoosaScript("noise") {
    private val uFrame = uniform("uFrame")
    private val uTime = uniform("uTime")
    private val uAlpha = uniform("uAlpha")

    fun prepare(left: Float, top: Float, width: Float, height: Float, elapsed: Float, progress: Float) {
        uFrame.value4f(left, top, width, height)
        uTime.value1f(elapsed)
        uAlpha.value1f(progress)
    }
}

class LightDissolveScript : ResourceNoosaScript("lightdissolve") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")
    private val uProgress = uniform("uProgress")
    private val uPixelSize = uniform("uPixelSize")
    private val uRandom = uniform("uRandom")
    private val uFlySpeed = uniform("uFlySpeed")
    private val uMoveDir = uniform("uMoveDir")

    fun prepare(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        pxW: Float,
        pxH: Float,
        progress: Float,
        random: Float
    ) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(1f)
        uProgress.value1f(progress)
        uPixelSize.value2f(if (pxW > 0f) 1f / pxW else 1f / 16f, if (pxH > 0f) 1f / pxH else 1f / 16f)
        uRandom.value1f(random)
        uFlySpeed.value1f(1.2f)
        uMoveDir.value2f(0.7071f, -0.7071f)
    }
}

class LightEvaporateScript : ResourceNoosaScript("lightevaporate") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")
    private val uProgress = uniform("uProgress")
    private val uPixelSize = uniform("uPixelSize")
    private val uRandom = uniform("uRandom")
    private val uEmissive = uniform("uEmissive")
    private val uCellScale = uniform("uCellScale")

    fun prepare(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        pxW: Float,
        pxH: Float,
        progress: Float,
        random: Float
    ) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(1f)
        uProgress.value1f(progress)
        uPixelSize.value2f(if (pxW > 0f) 1f / pxW else 1f / 16f, if (pxH > 0f) 1f / pxH else 1f / 16f)
        uRandom.value1f(random)
        uEmissive.value1f(1f + 3f * progress)
        uCellScale.value1f(1f)
    }
}

class WhitePixelEvaporateParticle : PixelParticle() {
    init {
        color(0xFFFFFF)
        acc.set(0f, -18f)
    }

    fun reset(x: Float, y: Float) {
        revive()
        this.x = x
        this.y = y
        speed.set(Random.Float(-4f, 4f), Random.Float(-26f, -12f))
        lifespan = Random.Float(0.45f, 0.78f)
        left = lifespan
        size(1f)
        am = 1f
    }

    override fun update() {
        super.update()
        val p = (left / lifespan).coerceIn(0f, 1f)
        am = p
    }

    companion object {
        @JvmField
        val FACTORY: Emitter.Factory = object : Emitter.Factory() {
            override fun emit(emitter: Emitter, index: Int, x: Float, y: Float) {
                val p = emitter.recycle(WhitePixelEvaporateParticle::class.java) as? WhitePixelEvaporateParticle
                p?.reset(x, y)
            }

            override fun lightMode(): Boolean = true
        }
    }
}

class RadishDeathEffect(
    val type: DeathShaderType,
    private val duration: Float = defaultDuration(type),
    private val driftX: Float = 0f,
    private val driftY: Float = if (type == DeathShaderType.LIGHT_EVAPORATE) -18f else 0f
) : SpriteShaderEffect {

    private var elapsed = 0f
    private var progress = 0f
    private val random = Math.random().toFloat()
    private var evaporateEmitter: Emitter? = null
    private var emitterStarted = false

    override fun update(sprite: CharSprite, elapsed: Float) {
        this.elapsed += elapsed
        progress = min(this.elapsed / max(duration, 0.001f), 1f)

        if (type == DeathShaderType.LIGHT_EVAPORATE) {
            sprite.x += driftX * elapsed
            sprite.y += driftY * elapsed
            updateEvaporateParticles(sprite)
        }

        if (progress >= 1f) {
            evaporateEmitter?.on = false
            evaporateEmitter = null
            sprite.finishShaderDeath(this)
        }
    }

    private fun updateEvaporateParticles(sprite: CharSprite) {
        val parent = sprite.parent ?: return
        if (!emitterStarted) {
            emitterStarted = true
            val emitter = Emitter()
            emitter.pos(sprite.x, sprite.y, sprite.width, sprite.height)
            parent.add(emitter)
            emitter.burst(WhitePixelEvaporateParticle.FACTORY, 28)
            emitter.pour(WhitePixelEvaporateParticle.FACTORY, 0.014f)
            evaporateEmitter = emitter
        } else {
            evaporateEmitter?.pos(sprite.x, sprite.y, sprite.width, sprite.height)
        }
    }

    override fun prepare(sprite: CharSprite): NoosaScript {
        val frame = sprite.frame()
        val tex = sprite.texture
        val pxW = if (tex != null) frame.width() * tex.width else 16f
        val pxH = if (tex != null) frame.height() * tex.height else 16f

        return when (type) {
            DeathShaderType.CUT -> Script.use(CutScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), elapsed, progress)
            }
            DeathShaderType.BURN -> Script.use(BurnScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), elapsed, progress, random)
            }
            DeathShaderType.ELLIPSE -> Script.use(EllipseScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), progress, random)
            }
            DeathShaderType.ACID -> Script.use(AcidScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), progress, random)
            }
            DeathShaderType.WIPE -> Script.use(WipeScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), progress)
            }
            DeathShaderType.ALPHA -> Script.use(AlphaScript::class.java).also {
                it.prepare(progress)
            }
            DeathShaderType.SINGULARITY -> Script.use(SingularityScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), progress)
            }
            DeathShaderType.NOISE -> Script.use(NoiseScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), elapsed, progress)
            }
            DeathShaderType.LIGHT_DISSOLVE -> Script.use(LightDissolveScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), pxW, pxH, progress, random)
            }
            DeathShaderType.LIGHT_EVAPORATE -> Script.use(LightEvaporateScript::class.java).also {
                it.prepare(frame.left, frame.top, frame.width(), frame.height(), pxW, pxH, progress, random)
            }
        }
    }

    companion object {
        fun defaultDuration(type: DeathShaderType): Float = when (type) {
            DeathShaderType.CUT -> 0.5f
            DeathShaderType.BURN -> 0.7f
            DeathShaderType.ACID -> 0.7f
            DeathShaderType.LIGHT_EVAPORATE -> 0.75f
            DeathShaderType.LIGHT_DISSOLVE -> 0.65f
            else -> 0.6f
        }
    }
}

object RadishDeathShaders {
    @JvmStatic
    @JvmOverloads
    fun play(
        target: Char,
        type: DeathShaderType,
        duration: Float = RadishDeathEffect.defaultDuration(type),
        driftX: Float = 0f,
        driftY: Float = if (type == DeathShaderType.LIGHT_EVAPORATE) -18f else 0f
    ): Boolean {
        if (!target.hasSprite || target.sprite.parent == null) return false
        Game.runOnRenderThreadAndWait {
            if (target.sprite.parent != null) {
                target.sprite.beginShaderDeath(RadishDeathEffect(type, duration, driftX, driftY))
            }
        }
        return target.sprite.hasPendingShaderDeath
    }

    @JvmStatic
    @JvmOverloads
    fun lightEvaporate(
        target: Char,
        duration: Float = RadishDeathEffect.defaultDuration(DeathShaderType.LIGHT_EVAPORATE),
        driftX: Float = 0f,
        driftY: Float = -18f
    ): Boolean = play(target, DeathShaderType.LIGHT_EVAPORATE, duration, driftX, driftY)
}
