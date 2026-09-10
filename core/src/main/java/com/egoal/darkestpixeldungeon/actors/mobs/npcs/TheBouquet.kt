package com.egoal.darkestpixeldungeon.actors.mobs.npcs

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.actors.buffs.Chill
import com.egoal.darkestpixeldungeon.levels.VillageLevel
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.plants.Icecap
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.sprites.MobSprite
import com.egoal.darkestpixeldungeon.utils.GLog
import com.egoal.darkestpixeldungeon.windows.WndDialogue
import com.egoal.darkestpixeldungeon.windows.WndMessage
import com.watabou.noosa.Game
import com.watabou.noosa.TextureFilm
import kotlin.math.sin

class TheBouquet : NPC.Unbreakable() {
    init {
        spriteClass = Sprite::class.java

        properties.add(Property.IMMOVABLE)
    }

    override fun interact(): Boolean {
        WndDialogue.Show(
            this, M.L(this, "message"), M.L(this, "touch"), M.L(this, "listen")
        ) { onOptionSelected(it) }

        return false
    }

    private fun onOptionSelected(index: Int) {
        when (index) {
            0 -> touch()
            1 -> GameScene.show { WndMessage(M.L(this, "noword")) }
        }
    }

    private fun touch() {
        if (VillageLevel.snowing) return

        if (Dungeon.isHeroNull) return
        val hero = Dungeon.hero
        Buff.affect(hero, Chill::class.java, CHILL_DURATION)
        Dungeon.level.drop(Icecap.Seed(), hero.pos).sprite.drop()
        GLog.w(M.L(this, "snow"))

        (Dungeon.level as? VillageLevel)?.startSnow()
    }

    class Sprite : MobSprite() {
        init {
            texture(Assets.BOUQUET)

            val frames = TextureFilm(texture, 16, 16)
            idle = Animation(1, true)
            idle.frames(frames, 0)

            die = Animation(20, true)
            die.frames(frames, 0)

            run = idle.clone()
            attack = idle.clone()

            play(idle)

            origin.set(width / 2f, height)
        }

        override fun update() {
            super.update()
            scale.set(1f + BREATH_AMPLITUDE * sin(Game.elapsed * BREATH_SPEED))
        }

        companion object {
            private const val BREATH_AMPLITUDE = 0.035f
            private const val BREATH_SPEED = 1.8f
        }
    }

    companion object {
        private const val CHILL_DURATION = 10f
    }
}
