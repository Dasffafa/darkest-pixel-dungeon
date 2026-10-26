package com.egoal.darkestpixeldungeon.items.helmets

import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet

class MaskOfLider : Helmet() {
    override val drawId: Int get() = 14
    override val replacesHead: Boolean get() = true
    init {
        image = ItemSpriteSheet.RIDER_MASK
    }

    override fun desc(): String {
        var desc = super.desc()
        if (isIdentified) desc += "\n\n" + Messages.get(this, "effect-desc")

        return desc
    }

    override fun viewAmend(): Int = 1
}