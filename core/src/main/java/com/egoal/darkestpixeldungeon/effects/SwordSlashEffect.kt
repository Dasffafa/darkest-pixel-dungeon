package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.actors.Char
import com.watabou.noosa.Group
import com.watabou.noosa.MovieClip
import com.watabou.noosa.TextureFilm
import com.watabou.utils.PointF
import kotlin.math.atan2

class SwordSlashEffect : MovieClip(Assets.SWORD_SLASH) {

    init {
        if (film == null) {
            film = TextureFilm(texture, 64, 47)
        }
        if (slashAnim == null) {
            slashAnim = Animation(24, false).apply {
                frames(film, 0, 1, 2, 3, 4, 5, 6, 7, 8)
            }
        }
        play(slashAnim)
        scale.set(0.6f)
        origin.set(width / 2f, height / 2f)
        listener = Listener { killAndErase() }
    }

    companion object {
        private var film: TextureFilm? = null
        private var slashAnim: Animation? = null

        @JvmStatic
        fun showOn(attacker: Char?, defender: Char?) {
            if (attacker == null || defender == null || !attacker.hasSprite || !defender.hasSprite) return
            val parent = defender.sprite.parent ?: return
            val from = attacker.sprite.center()
            val to = defender.sprite.center()
            val angleDeg = Math.toDegrees(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())).toFloat()
            preview(parent, to, 0.6f, angleDeg)
        }

        @JvmStatic
        @JvmOverloads
        fun preview(parent: Group?, center: PointF?, scaleFactor: Float = 0.6f, angleDeg: Float = 0f) {
            if (parent == null || center == null) return
            val effect = SwordSlashEffect().apply {
                scale.set(scaleFactor)
                origin.set(width / 2f, height / 2f)
                x = center.x - width / 2f
                y = center.y - height / 2f
                angle = angleDeg
            }
            parent.add(effect)
        }
    }
}
