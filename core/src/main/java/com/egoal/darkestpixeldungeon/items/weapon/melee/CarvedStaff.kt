package com.egoal.darkestpixeldungeon.items.weapon.melee

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.utils.Random

class CarvedStaff : MeleeWeapon() {
    init {
        image = ItemSpriteSheet.CARVED_STAFF
        tier = 3
    }

    override val maxEnchantments: Int get() = 2

    override fun max(lvl: Int): Int = 5 * tier + lvl * tier

    override fun speedFactor(hero: Hero): Float {
        val ratio = if (enchantment == null) 1f else 0.8f
        return super.speedFactor(hero) * ratio
    }

    override fun giveDamage(hero: Hero, target: Char): Damage {
        val dmg = super.giveDamage(hero, target)

        // holding two enchantments at once also adds a little attack power
        if (enchantments.size >= 2) dmg.value += Random.IntRange(1, level().coerceAtLeast(0) + 1)

        return dmg
    }

    override fun proc(dmg: Damage): Damage {
        var dmg = dmg

        // trigger every enchantment once, then a second time inside super.proc;
        // the repeat triggers must not drain duration again
        for (enc in ArrayList(enchantments)) dmg = enc.proc(this, dmg)

        val repeat = ArrayList(enchantments)
        for (enc in repeat) enc.extraTrigger = true
        try {
            dmg = super.proc(dmg)
        } finally {
            for (enc in repeat) enc.extraTrigger = false
        }
        return dmg
    }
}
