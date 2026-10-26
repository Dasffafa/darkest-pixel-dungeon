package com.egoal.darkestpixeldungeon.items.helmets

import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet

/**
 * Created by 93942 on 9/24/2018.
 */

class CrownOfDwarf : Helmet() {
    override val drawId: Int get() = 6
    override val replacesHead: Boolean get() = false
    init {
        image = ItemSpriteSheet.DWARF_CROWN
    }

    override val isIdentified: Boolean
        get() = true

    override fun price() = 500
}
