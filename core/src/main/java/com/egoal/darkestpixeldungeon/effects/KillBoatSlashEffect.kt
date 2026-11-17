package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.actors.Char
import com.watabou.noosa.Group
import com.watabou.noosa.MovieClip
import com.watabou.noosa.TextureFilm
import com.watabou.utils.PointF

class KillBoatSlashEffect @JvmOverloads constructor(tear: Boolean = false) :
    MovieClip(if (tear) Assets.KILL_BOAT_TEAR else Assets.KILL_BOAT_SLASH) {

    init {
        if (tear) {
            if (tearFilm == null) {
                tearFilm = TextureFilm(texture, FRAME, FRAME)
                tearAnim = Animation(FPS, false).apply {
                    frames(tearFilm, 0, 1, 2, 3, 4, 5, 6, 7)
                }
            }
            play(tearAnim)
        } else {
            if (slashFilm == null) {
                slashFilm = TextureFilm(texture, FRAME, FRAME)
                slashAnim = Animation(FPS, false).apply {
                    frames(slashFilm, 0, 1, 2, 3, 4, 5, 6)
                }
            }
            play(slashAnim)
        }

        scale.set(SCALE_X, SCALE_Y)
        origin.set(width / 2f, height / 2f)
        listener = Listener { killAndErase() }
    }

    companion object {
        @JvmField
        var enabled = true

        private const val FRAME = 32
        private const val FPS = 10
        private const val SCALE_X = 1.9f
        private const val SCALE_Y = 1.15f

        const val SLASH_TIME = 7f / FPS
        const val TEAR_TIME = 8f / FPS
        const val KILL_CLIP_TIME = SLASH_TIME + TEAR_TIME

        private var slashFilm: TextureFilm? = null
        private var tearFilm: TextureFilm? = null
        private var slashAnim: Animation? = null
        private var tearAnim: Animation? = null

        @JvmStatic
        fun showOn(target: Char?, tear: Boolean) {
            if (!enabled || target == null || !target.hasSprite) return
            val s = target.sprite
            val parent = s.parent ?: return
            preview(parent, s.center(), tear)
        }

        @JvmStatic
        fun preview(parent: Group?, center: PointF?, tear: Boolean) {
            if (parent == null || center == null) return
            val e = KillBoatSlashEffect(tear).apply {
                scale.set(SCALE_X, SCALE_Y)
                origin.set(width / 2f, height / 2f)
                x = center.x - width / 2f
                y = center.y - height / 2f
            }
            parent.add(e)
        }
    }
}
