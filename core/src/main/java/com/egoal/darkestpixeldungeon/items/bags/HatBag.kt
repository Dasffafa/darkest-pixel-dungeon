package com.egoal.darkestpixeldungeon.items.bags

import com.egoal.darkestpixeldungeon.actors.hero.Belongings
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.helmets.Helmet
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet

class HatBag : Bag() {

    init {
        image = ItemSpriteSheet.POUCH

        size = Belongings.BACKPACK_SIZE
    }

    override fun canHold(item: Item): Boolean = item is Helmet && super.canHold(item)

    override fun price(): Int = 0
}
