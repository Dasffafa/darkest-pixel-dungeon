package com.egoal.darkestpixeldungeon.effects

object DamagePalette {

    // main damage
    const val PHYSICAL = 0xcc0000
    const val PURE_NORMAL = 0xffffff
    const val MAGICAL = 0x3499ff
    const val CRIT = 0xcc0000
    const val PURE_CRIT = 0xffffff

    // elements
    const val FIRE = 0xffaa33
    const val ICE = 0x5674b9
    const val POISON = 0x993399
    const val SHADOW = 0xffffff
    const val LIGHT = 0x2ce8f5
    const val HOLY = 0xffff00

    // special sources
    const val HUNGER = 0xff0000

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
        FloatingText.ELEM_FIRE -> FIRE
        FloatingText.ELEM_ICE -> ICE
        FloatingText.ELEM_POISON -> POISON
        FloatingText.ELEM_SHADOW -> SHADOW
        FloatingText.ELEM_LIGHT -> LIGHT
        FloatingText.ELEM_HOLY -> HOLY
        FloatingText.HUNGER -> HUNGER
        FloatingText.HEALING -> HEALING
        else -> DEFAULT
    }
}
