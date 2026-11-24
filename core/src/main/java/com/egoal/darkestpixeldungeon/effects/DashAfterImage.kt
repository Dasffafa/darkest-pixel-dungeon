package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.actors.Char
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.utils.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * 移植自 Wayward Souls `dashBlur` / `tealDash` / `shadoworbDash`：
 * 沿位移路径生成多道带色调半透明残影并逐一淡出。
 */
class DashAfterImage private constructor(
    private val ghost: Image,
    private val delay: Float,
    private val fadeDuration: Float,
    private val startAlpha: Float
) : Group() {

    private var elapsed = 0f

    init {
        ghost.alpha(0f)
        add(ghost)
    }

    override fun update() {
        super.update()
        elapsed += Game.elapsed
        if (elapsed < delay) return

        val t = min(1f, (elapsed - delay) / fadeDuration)
        ghost.alpha(startAlpha * (1f - t))
        if (t >= 1f) {
            killAndErase()
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun trail(
            ch: Char,
            fromCell: Int,
            toCell: Int,
            color: Int = 0x66EEFF,
            steps: Int = 6,
            fadeDuration: Float = 0.28f
        ) {
            if (!Dungeon.visible[fromCell] && !Dungeon.visible[toCell]) return
            Game.runOnRenderThread {
                if (!ch.hasSprite) return@runOnRenderThread
                val src = ch.sprite
                val parent = src.parent ?: return@runOnRenderThread
                val frame = RectF(src.frame())

                val p0 = DungeonTilemap.tileCenterToWorld(fromCell)
                val p1 = DungeonTilemap.tileCenterToWorld(toCell)
                val count = max(2, steps)

                for (i in 0 until count) {
                    val frac = i.toFloat() / count.toFloat()
                    val cx = p0.x + (p1.x - p0.x) * frac
                    val cy = p0.y + (p1.y - p0.y) * frac

                    val img = Image().apply {
                        texture = src.texture
                        frame(frame)
                        flipHorizontal = src.flipHorizontal
                        hardlight(color)
                        x = cx - width() / 2f
                        y = cy + DungeonTilemap.SIZE / 2f - height() - 2f
                    }
                    parent.add(DashAfterImage(img, i * 0.02f, fadeDuration, 0.65f - frac * 0.2f))
                }
            }
        }
    }
}
