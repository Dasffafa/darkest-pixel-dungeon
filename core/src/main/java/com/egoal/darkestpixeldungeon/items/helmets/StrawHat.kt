package com.egoal.darkestpixeldungeon.items.helmets

import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet

class StrawHat : Helmet() {
    init {
        image = ItemSpriteSheet.STRAW_HAT
    }

    override val drawId: Int get() = 0
    override val replacesHead: Boolean get() = false

    override fun desc(): String = super.desc() + "\n\n" + M.L(this, "effect-desc")

    override val isIdentified: Boolean
        get() = true

    override fun random(): Item {
        cursed = false
        return this
    }

    override fun price(): Int = 0
}