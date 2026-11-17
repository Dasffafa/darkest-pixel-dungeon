package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.levels.Level
import com.egoal.darkestpixeldungeon.levels.lighting.LightSource
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.noosa.audio.Sample
import com.watabou.noosa.tweeners.AlphaTweener
import com.watabou.utils.ColorMath
import com.watabou.utils.PointF
import com.watabou.utils.Random
import com.watabou.utils.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class IceShatter private constructor(
    src: CharSprite,
    srcFrame: RectF
) : Group() {

    private class Shard(
        val image: Image,
        var vx: Float,
        var vy: Float,
        var angVel: Float,
        val groundY: Float
    ) {
        var landed = false
    }

    private val shards = ArrayList<Shard>(GRID_COLS * GRID_ROWS)
    private var phase = Phase.FLYING
    private var timer = 0f

    private enum class Phase { FLYING, LINGERING, FADING }

    init {
        val uW = srcFrame.width() / GRID_COLS
        val uH = srcFrame.height() / GRID_ROWS
        val cellW = src.width / GRID_COLS
        val cellH = src.height / GRID_ROWS
        val baseGroundY = src.y + src.height

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val u0 = srcFrame.left + col * uW
                val v0 = srcFrame.top + row * uH
                val img = Image().apply {
                    texture = src.texture
                    frame(RectF(u0, v0, u0 + uW, v0 + uH))
                    flipHorizontal = src.flipHorizontal

                    val drawCol = if (src.flipHorizontal) (GRID_COLS - 1 - col) else col
                    x = src.x + drawCol * cellW
                    y = src.y + row * cellH
                    origin.set(width * 0.5f, height * 0.5f)
                    hardlight(ColorMath.interpolate(0x88EEFF, 0xFFFFFF, Random.Float(0.15f, 0.55f)))
                }
                add(img)

                val angle = Random.Float(0f, (PI * 2.0).toFloat())
                val horizRatio = ((col + 0.5f) / GRID_COLS - 0.5f) * 2f
                val spd = Random.Float(24f, 62f)
                val vx = horizRatio * 48f + cos(angle) * spd * 0.45f
                val vy = -Random.Float(45f, 115f) + sin(angle) * 18f
                val angVel = Random.Float(-360f, 360f)
                val groundY = baseGroundY - img.height() + Random.Float(-2.5f, 2.5f)
                shards.add(Shard(img, vx, vy, angVel, groundY))
            }
        }
    }

    override fun update() {
        super.update()
        val dt = Game.elapsed
        when (phase) {
            Phase.FLYING -> {
                var allLanded = true
                for (shard in shards) {
                    if (shard.landed) continue
                    allLanded = false
                    updateShard(shard, dt)
                }
                if (allLanded) {
                    phase = Phase.LINGERING
                    timer = 0f
                }
            }
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

    private fun updateShard(shard: Shard, dt: Float) {
        shard.vy += GRAVITY * dt

        val img = shard.image
        val nextX = img.x + shard.vx * dt
        if (isSolidWorld(nextX + img.width() * 0.5f, img.y + img.height() * 0.5f)) {
            shard.vx = -shard.vx * WALL_BOUNCE
            shard.angVel = -shard.angVel * 0.5f
        } else {
            img.x = nextX
        }

        val nextY = img.y + shard.vy * dt
        if (shard.vy < 0f && isSolidWorld(img.x + img.width() * 0.5f, nextY + img.height() * 0.5f)) {
            shard.vy = -shard.vy * WALL_BOUNCE
        } else if (nextY >= shard.groundY && shard.vy > 0f) {
            img.y = shard.groundY
            if (shard.vy > 75f) {
                // Small ground bounce before settling
                shard.vy = -shard.vy * 0.25f
                shard.vx *= 0.55f
                shard.angVel *= 0.4f
            } else {
                shard.landed = true
            }
        } else {
            img.y = nextY
        }

        img.angle += shard.angVel * dt
    }

    private fun isSolidWorld(wx: Float, wy: Float): Boolean {
        val level = Dungeon.level
        val tx = (wx / DungeonTilemap.SIZE).toInt()
        val ty = (wy / DungeonTilemap.SIZE).toInt()
        if (tx !in 0 until level.width() || ty !in 0 until level.height()) return true
        return Level.solid[ty * level.width() + tx]
    }

    private fun startFade() {
        val p = parent ?: run {
            killAndErase()
            return
        }
        var remaining = shards.size
        for (shard in shards) {
            val img = shard.image
            p.add(object : AlphaTweener(img, 0f, FADE_TIME) {
                override fun onComplete() {
                    img.killAndErase()
                    if (--remaining <= 0) {
                        this@IceShatter.killAndErase()
                    }
                }
            })
        }
    }

    companion object {
        private const val GRID_COLS = 4
        private const val GRID_ROWS = 4
        private const val GRAVITY = 380f
        private const val WALL_BOUNCE = 0.35f
        private const val LINGER_TIME = 0.85f
        private const val FADE_TIME = 1.0f

        @JvmStatic
        fun shatter(ch: Char?): Boolean {
            if (ch == null || !ch.hasSprite || ch.sprite.parent == null) return false
            Game.runOnRenderThreadAndWait {
                val sprite = ch.sprite
                if (sprite.parent != null) {
                    sprite.suppressDeathAnimation()
                    spawn(sprite, ch.pos)
                }
            }
            return ch.sprite.hasPendingShaderDeath
        }

        @JvmStatic
        fun spawn(sprite: CharSprite?, cell: Int = -1) {
            if (sprite == null || sprite.parent == null) return
            val parent = sprite.parent
            val center: PointF = sprite.center()
            val srcFrame = sprite.frame()

            val effect = IceShatter(sprite, srcFrame)
            sprite.killAndErase()
            parent.add(effect)
            parent.sendToBack(effect)

            Sample.INSTANCE.play(Assets.SND_SHATTER, 0.9f, Random.Float(0.95f, 1.2f))
            Splash.at(center, 0x99EEFF, 14)
            if (cell >= 0 && !Dungeon.isLevelNull) {
                Dungeon.level.addTransientLight(LightSource.frost(cell, 0.25f))
            }
        }
    }
}
