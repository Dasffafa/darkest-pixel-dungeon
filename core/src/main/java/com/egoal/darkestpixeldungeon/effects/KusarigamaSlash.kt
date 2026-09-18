package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.effects.particles.SparkParticle
import com.watabou.noosa.MovieClip
import com.watabou.noosa.TextureFilm
import com.watabou.utils.PointF

class KusarigamaSlash : MovieClip(), MovieClip.Listener {
    private val anim = Animation(24, false)

    init {
        listener = this
        texture("miscs/kusarigama_slash.png")
        anim.frames(TextureFilm(texture, 48, 48), 0, 1, 2, 3, 4)
        origin.set(24f, 24f)
    }

    override fun onComplete(anim: Animation) {
        kill()
    }

    fun reset(targetCenter: PointF, attackAngle: Float) {
        revive()
        x = targetCenter.x - origin.x
        y = targetCenter.y - origin.y
        angle = attackAngle
        play(anim)
    }

    companion object {
        fun slash(attacker: Char, defender: Char) {
            val parent = defender.sprite.parent ?: return
            val slash = parent.recycle(KusarigamaSlash::class.java) as KusarigamaSlash
            parent.bringToFront(slash)

            val from = attacker.sprite.center()
            val to = defender.sprite.center()
            val attackAngle = PointF.angle(from, to)

            slash.reset(to, attackAngle)

            // Tangent to the arc: perpendicular to the line from attacker to defender
            val tangent = attackAngle + (Math.PI / 2).toFloat()
            val blood = defender.sprite.blood()
            if (blood != 0) {
                Splash.at(to, tangent, (Math.PI / 4).toFloat(), 1.3f, blood, 18)
            } else {
                CellEmitter.center(defender.pos).burst(SparkParticle.FACTORY, 10)
            }
        }
    }
}
