package com.egoal.darkestpixeldungeon.items.weapon.melee

import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet

class CarvedStaff : MeleeWeapon() {
    init {
        image = ItemSpriteSheet.CARVED_STAFF
        tier = 3
    }

    override val maxInscriptions: Int get() = 2

    override fun max(lvl: Int): Int = 5 * tier + lvl * tier

    override fun speedFactor(hero: Hero): Float {
        val ratio = if (enchantment == null) 1f else 0.8f
        return super.speedFactor(hero) * ratio
    }

    override fun proc(dmg: Damage): Damage {
        var dmg = dmg
        val enc = enchantment ?: return super.proc(dmg)

        // trigger once here, then once more inside super.proc; the second trigger
        // must apply its effect without draining the enchantment's duration again
        dmg = enc.proc(this, dmg)
        enc.extraTrigger = true
        try {
            dmg = super.proc(dmg)
        } finally {
            enc.extraTrigger = false
        }
        return dmg
    }

    override fun name(): String {
        val visible = inscriptions.filter { cursedKnown || !it.curse }
        val base = trueName()

        return when {
            visible.isEmpty() -> base
            visible.size == 1 -> visible[0].name(base)
            Messages.has(CarvedStaff::class.java, "dual_name") -> M.L(this, "dual_name",
                    visible[0].name("").trim(), visible[1].name("").trim(), base)
            else -> visible.fold(base) { acc, insc -> insc.name(acc) }
        }
    }
}
