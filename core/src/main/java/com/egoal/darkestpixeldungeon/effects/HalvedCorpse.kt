package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.levels.Level
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.noosa.particles.Emitter
import com.watabou.noosa.particles.PixelParticle
import com.watabou.noosa.tweeners.AlphaTweener
import com.watabou.utils.PointF
import com.watabou.utils.Random
import com.watabou.utils.RectF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class HalvedCorpse private constructor(
    src: CharSprite,
    srcFrame: RectF,
    cutFrac: Float,
    angleDeg: Float,
    speed: Float
) : Group() {

    private val upper: Image
    private val lower: Image
    private val cutY: Float

    private var vx: Float
    private var vy: Float
    private var angVel: Float
    private var phase = Phase.FLYING
    private var timer = 0f
    private var fadingStarted = false

    private enum class Phase { FLYING, LINGERING, FADING }

    init {
        val cutV = srcFrame.top + srcFrame.height() * cutFrac
        cutY = src.y + src.height * cutFrac

        upper = Image().apply {
            texture = src.texture
            frame(RectF(srcFrame.left, srcFrame.top, srcFrame.right, cutV))
            flipHorizontal = src.flipHorizontal
            x = src.x
            y = src.y
            origin.set(width / 2f, height)
        }

        lower = Image().apply {
            texture = src.texture
            frame(RectF(srcFrame.left, cutV, srcFrame.right, srcFrame.bottom))
            flipHorizontal = src.flipHorizontal
            x = src.x
            y = src.y + upper.height()
        }

        add(lower)
        add(upper)

        val rad = Math.toRadians(angleDeg.toDouble())
        vx = (speed * cos(rad)).toFloat()
        vy = (-speed * sin(rad)).toFloat()
        val maxAngle = Random.Float(35f, 65f) * if (Random.Int(2) == 0) 1f else -1f
        angVel = maxAngle / 0.45f
    }

    override fun update() {
        super.update()
        val dt = Game.elapsed
        when (phase) {
            Phase.FLYING -> updateFlight(dt)
            Phase.LINGERING -> {
                timer += dt
                if (timer >= LINGER_TIME) {
                    phase = Phase.FADING
                    startFade()
                }
            }
            Phase.FADING -> Unit
        }
    }

    private fun updateFlight(dt: Float) {
        vy += GRAVITY * dt

        val nextX = upper.x + vx * dt
        val centerX = nextX + upper.width * 0.5f
        val centerY = upper.y + upper.height * 0.5f
        if (isSolidWorld(centerX, centerY)) {
            vx = -vx * WALL_BOUNCE_X
            if (vy < 0f) vy *= 0.5f
            angVel = -angVel * 0.5f
        } else {
            upper.x = nextX
        }

        val nextY = upper.y + vy * dt
        val nextCenterX = upper.x + upper.width * 0.5f
        val nextCenterY = nextY + upper.height * 0.5f
        if (vy < 0f && isSolidWorld(nextCenterX, nextCenterY)) {
            vy = -vy * WALL_BOUNCE_Y
        } else if (nextY >= cutY && vy > 0f) {
            upper.y = cutY
            phase = Phase.LINGERING
            timer = 0f
            return
        } else {
            upper.y = nextY
        }

        upper.angle += angVel * dt
    }

    private fun isSolidWorld(wx: Float, wy: Float): Boolean {
        val level = Dungeon.level
        val tx = (wx / DungeonTilemap.SIZE).toInt()
        val ty = (wy / DungeonTilemap.SIZE).toInt()
        if (tx !in 0 until level.width() || ty !in 0 until level.height()) return true
        return Level.solid[ty * level.width() + tx]
    }

    private fun startFade() {
        if (fadingStarted) return
        fadingStarted = true
        val p = parent ?: run {
            killAndErase()
            return
        }
        for (img in arrayOf(upper, lower)) {
            p.add(object : AlphaTweener(img, 0f, FADE_TIME) {
                override fun onComplete() {
                    img.killAndErase()
                    this@HalvedCorpse.killAndErase()
                }
            })
        }
    }

    private class CutBloodFactory(private val color: Int) : Emitter.Factory() {
        override fun emit(emitter: Emitter, index: Int, x: Float, y: Float) {
            val p = emitter.recycle(PixelParticle.Shrinking::class.java) as? PixelParticle ?: return
            p.reset(x, y, color, Random.Float(1f, 2f), Random.Float(0.5f, 0.9f))
            p.speed.set(Random.Float(-12f, 12f), Random.Float(-8f, 8f))
            p.acc.set(0f, 100f)
        }
    }

    companion object {
        private const val GRAVITY = 360f
        private const val WALL_BOUNCE_X = 0.35f
        private const val WALL_BOUNCE_Y = 0.30f
        private const val LINGER_TIME = 0.7f
        private const val FADE_TIME = 1.2f

        @JvmStatic
        @JvmOverloads
        fun bisect(
            ch: Char?,
            angleDeg: Float = if (Random.Int(2) == 0) 45f else 135f,
            speed: Float = 90f,
            cutFrac: Float = 0.5f
        ) {
            val sprite = ch?.sprite ?: return
            spawn(sprite, angleDeg, speed, cutFrac)
        }

        @JvmStatic
        @JvmOverloads
        fun spawn(
            sprite: CharSprite?,
            angleDeg: Float = if (Random.Int(2) == 0) 45f else 135f,
            speed: Float = 90f,
            cutFrac: Float = 0.5f
        ) {
            if (sprite == null || sprite.parent == null) return
            val parent = sprite.parent
            val srcFrame = sprite.frame()
            val sw = sprite.width
            val clampedFrac = max(0.1f, min(0.9f, cutFrac))
            val cutY = sprite.y + sprite.height * clampedFrac
            val bloodColor = sprite.blood()
            val sx = sprite.x

            sprite.killAndErase()

            val corpse = HalvedCorpse(sprite, srcFrame, clampedFrac, angleDeg, speed)
            parent.add(corpse)
            parent.sendToBack(corpse)

            val cutCenter = PointF(sx + sw / 2f, cutY)
            Splash.at(cutCenter, bloodColor, 14)

            val emitter = Emitter()
            emitter.pos(sx, cutY - 1f, sw, 2f)
            emitter.start(CutBloodFactory(bloodColor), 0.04f, max(1, (sw / 2f).toInt()))
            parent.add(emitter)
        }
    }
}
