package com.egoal.darkestpixeldungeon.items.helmets

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.utils.Random

class HelmetBarbarian : Helmet() {
    override val drawId: Int get() = 9
    override val replacesHead: Boolean get() = true
    init {
        image = ItemSpriteSheet.HELMET_BARBARIAN
    }

    override fun desc(): String {
        var desc = super.desc()
        if (isIdentified) {
            desc += "\n\n" + Messages.get(this, "effect-desc")
            if (cursed)
                desc += "\n\n" + Messages.get(Helmet::class.java, "cursed_desc")
        }

        return desc
    }

    override fun procGivenDamage(dmg: Damage) {
        if (dmg.from != null && dmg.from is Char && (dmg.from as Char).HP < (dmg.from as Char).HT / 2)
            dmg.value = dmg.value * 5 / 4
    }
}