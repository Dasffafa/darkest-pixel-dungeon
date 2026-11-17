package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.effects.shaders.ResourceNoosaScript
import com.watabou.glscripts.Script
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.glwrap.Matrix
import com.watabou.glwrap.Quad
import com.watabou.glwrap.Vertexbuffer
import com.watabou.noosa.Camera
import com.watabou.noosa.Game
import com.watabou.utils.Random
import com.watabou.utils.RectF
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class SceneCutScript : ResourceNoosaScript("scenecut") {
    private val uFrame = uniform("uFrame")
    private val uSize = uniform("uSize")
    private val uPad = uniform("uPad")
    private val uLine = uniform("uLine")
    private val uSlide = uniform("uSlide")
    private val uFall = uniform("uFall")
    private val uSilhouette = uniform("uSilhouette")
    private val uFade = uniform("uFade")
    private val uCrack = uniform("uCrack")
    private val uGlow = uniform("uGlow")
    private val uSeed = uniform("uSeed")
    private val uGlowOnly = uniform("uGlowOnly")

    fun setParams(
        uv: RectF,
        w: Float,
        h: Float,
        pad: Float,
        line: FloatArray,
        slide: Float,
        fall: Float,
        crack: Float,
        fade: Float,
        seed: Float
    ) {
        uFrame.value4f(uv.left, uv.top, uv.width(), uv.height())
        uSize.value2f(w, h)
        uPad.value1f(pad)
        uLine.value4f(line[0], line[1], line[2], line[3])
        uSlide.value1f(slide)
        uFall.value1f(fall)
        uSilhouette.value1f(0f)
        uFade.value1f(fade)
        uCrack.value1f(crack)
        uGlow.value1f(0f)
        uGlowOnly.value1f(0f)
        uSeed.value1f(seed)
    }
}

object SceneCutEffect {
    private const val CLIP_TIME = KillBoatSlashEffect.TEAR_TIME
    private const val TEAR_HOLD = 0.45f
    private const val CUT_TIME = CLIP_TIME + TEAR_HOLD

    private const val SLIDE_TILES = 0.5f
    private const val SLIDE_DELAY = 0.02f
    private const val SLIDE_TIME = 0.35f
    private const val FALL_FRAC = 0.5f
    private const val FALL_DELAY = SLIDE_DELAY + SLIDE_TIME
    private const val FALL_TIME = 0.28f
    private const val FADE_DELAY = FALL_DELAY + FALL_TIME
    private const val FADE_TIME = 0.14f

    private const val CUT_ANGLE_DEG = 30f
    private const val CLIP_FRAME = 32
    private const val CLIP_FPS = 10f
    private const val CLIP_SCALE_X = 1.85f
    private const val CLIP_SCALE_Y = 0.95f

    private var armed = false
    private var active = false
    private var elapsed = 0f
    private var seed = 0f

    private var pendingVictim: Char? = null
    private var tex: SmartTexture? = null
    private var frame: RectF? = null
    private val rect = FloatArray(4)
    private val line = FloatArray(4)
    private val identity = FloatArray(16).apply { Matrix.setIdentity(this) }

    private var tearTex: SmartTexture? = null
    private var quad: Vertexbuffer? = null
    private var quadBuffer: FloatBuffer? = null

    @JvmStatic
    fun trigger(victim: Char?): Boolean {
        if (victim == null || !victim.hasSprite || victim.sprite.parent == null) return false
        if (armed || active || Dungeon.isLevelNull) return false
        if (!Dungeon.visible[victim.pos]) return false

        victim.sprite.suppressDeathAnimation()
        pendingVictim = victim
        elapsed = 0f
        seed = Random.Float(0f, 100f)
        armed = true
        return true
    }

    @JvmStatic
    fun postDraw() {
        if (armed) {
            armed = false
            val victim = pendingVictim
            pendingVictim = null
            if (!capture(victim)) return
            active = true
            elapsed = 0f
        }
        if (!active) return

        elapsed += Game.elapsed
        if (elapsed >= CUT_TIME + FADE_DELAY + FADE_TIME) {
            finish()
            return
        }
        render(elapsed)
    }

    private fun capture(victim: Char?): Boolean {
        if (victim == null || !victim.hasSprite) return false
        val s = victim.sprite
        if (s.texture == null || s.parent == null) return false

        tex = s.texture
        frame = s.frame()
        rect[0] = s.x
        rect[1] = s.y
        rect[2] = s.width()
        rect[3] = s.height()
        if (rect[2] <= 0f || rect[3] <= 0f) return false

        aim()
        s.visible = false
        s.killAndErase()
        return true
    }

    private fun aim() {
        val rad = Math.toRadians(CUT_ANGLE_DEG.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val len = sqrt(dx * dx + dy * dy)
        line[0] = spriteWidthPx() * 0.5f
        line[1] = spriteHeightPx() * 0.5f
        line[2] = dx / len
        line[3] = dy / len
    }

    private fun spriteWidthPx(): Float {
        val f = frame ?: return 16f
        val t = tex ?: return 16f
        return f.width() * t.width
    }

    private fun spriteHeightPx(): Float {
        val f = frame ?: return 16f
        val t = tex ?: return 16f
        return f.height() * t.height
    }

    private fun padPx(): Float = max(SLIDE_TILES * DungeonTilemap.SIZE, FALL_FRAC * spriteHeightPx()) + 10f

    private fun render(now: Float) {
        val tTex = tex ?: return finish()
        val tFrame = frame ?: return finish()

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight())

        val t = now - CUT_TIME
        val crack = if (t >= 0f) 1f else 0f
        val slide = if (t > 0f) clamp01((t - SLIDE_DELAY) / SLIDE_TIME) * SLIDE_TILES * DungeonTilemap.SIZE else 0f
        val fallT = if (t > 0f) clamp01((t - FALL_DELAY) / FALL_TIME) else 0f
        val fall = fallT * fallT * spriteHeightPx() * FALL_FRAC
        val fade = if (t > 0f) 1f - clamp01((t - FADE_DELAY) / FADE_TIME) else 1f

        val pad = padPx()
        beginPass(tTex, tFrame, spriteWidthPx(), spriteHeightPx(), pad, slide, fall, crack, fade)
        drawQuad(rect[0] - pad, rect[1] - pad, rect[2] + pad * 2f, rect[3] + pad * 2f)

        if (now < CLIP_TIME) {
            val f = min(7, (now * CLIP_FPS).toInt())
            val clip = getTearTex()
            val w = CLIP_FRAME * CLIP_SCALE_X
            val h = CLIP_FRAME * CLIP_SCALE_Y
            val cx = rect[0] + rect[2] * 0.5f
            val cy = rect[1] + rect[3] * 0.5f
            beginPass(clip, clipFrame(clip, f), CLIP_FRAME.toFloat(), CLIP_FRAME.toFloat(), 0f, 0f, 0f, 0f, 1f)
            drawQuad(cx - w * 0.5f, cy - h * 0.5f, w, h)
        }

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun getTearTex(): SmartTexture {
        if (tearTex == null) {
            tearTex = TextureCache.get(Assets.KILL_BOAT_TEAR)
        }
        return tearTex!!
    }

    private fun clipFrame(texture: SmartTexture, i: Int): RectF {
        val fw = CLIP_FRAME / texture.width.toFloat()
        val left = i * fw
        return RectF(left, 0f, left + fw, 1f)
    }

    private fun beginPass(
        texture: SmartTexture,
        uv: RectF,
        w: Float,
        h: Float,
        pad: Float,
        slide: Float,
        fall: Float,
        crack: Float,
        fade: Float
    ) {
        val s = Script.use(SceneCutScript::class.java)
        s.resetCamera()
        s.camera(Camera.main)
        s.uModel.valueM4(identity)
        s.setParams(uv, w, h, pad, line, slide, fall, crack, fade, seed)
        texture.bind()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawQuad(x: Float, y: Float, w: Float, h: Float) {
        val buf = quadBuffer ?: Quad.create().also { quadBuffer = it }
        buf.position(0)
        buf.put(
            floatArrayOf(
                x, y, 0f, 0f,
                x + w, y, 1f, 0f,
                x + w, y + h, 1f, 1f,
                x, y + h, 0f, 1f
            )
        )
        buf.position(0)

        val vb = quad ?: Vertexbuffer(buf).also { quad = it }
        if (vb === quad) vb.updateVertices(buf)

        val s = Script.use(SceneCutScript::class.java)
        s.drawQuad(vb)
    }

    private fun finish() {
        active = false
        armed = false
        elapsed = 0f
        pendingVictim = null
        tex = null
        frame = null
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    @JvmStatic
    fun reset() {
        finish()
        quad = null
        quadBuffer = null
        tearTex = null
    }

    private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
}
