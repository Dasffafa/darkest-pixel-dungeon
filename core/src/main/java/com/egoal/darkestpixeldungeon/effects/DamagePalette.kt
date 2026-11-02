package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.actors.Damage

object DamagePalette {

    // main damage
    const val PHYSICAL = 0xcc0000
    const val PURE_NORMAL = 0xffffff
    const val MAGICAL = 0x3499ff
    const val CRIT = 0xcc0000
    const val PURE_CRIT = 0xffffff

    // elements (matched to the dominant fill colour of the resistance icons)
    const val FIRE = 0xffaa33
    const val ICE = 0xdee8ff
    const val POISON = 0x50ff60
    const val SHADOW = 0x575757
    const val LIGHT = 0xdee8ff
    const val HOLY = 0xffff00

    // debuff / damage-over-time
    const val BLEEDING = 0xcc0000
    const val OOZE = 0x008056

    // special sources
    const val HUNGER = 0xf2e4da

    // positive
    const val HEALING = 0x00ff00

    // fallback (no icon, e.g. mental damage)
    const val DEFAULT = 0x8c8c8c

    fun colorFor(icon: Int): Int = when (icon) {
        FloatingText.PHYS_DMG -> PHYSICAL
        FloatingText.PHYS_DMG_NO_ARMOR -> PURE_NORMAL
        FloatingText.MAGIC_DMG -> MAGICAL
        FloatingText.CRIT -> CRIT
        FloatingText.CRIT_NO_ARMOR -> PURE_CRIT
        FloatingText.BLEEDING -> BLEEDING
        FloatingText.OOZE -> OOZE
        FloatingText.HUNGER -> HUNGER
        FloatingText.HEALING -> HEALING
        else -> DEFAULT
    }

    fun colorFor(element: Damage.Element): Int = when (element) {
        Damage.Element.Fire -> FIRE
        Damage.Element.Poison -> POISON
        Damage.Element.Ice -> ICE
        Damage.Element.Light -> LIGHT
        Damage.Element.Shadow -> SHADOW
        Damage.Element.Holy -> HOLY
    }
}
