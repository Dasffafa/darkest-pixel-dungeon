package com.egoal.darkestpixeldungeon.items.bags

import com.egoal.darkestpixeldungeon.actors.hero.Belongings
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.unclassified.ParticleTesterItem
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet

class ParticlePlayerBag : Bag() {

    init {
        image = ItemSpriteSheet.POUCH
        size = 35
    }

    override fun canHold(item: Item): Boolean = item is ParticleTesterItem && super.canHold(item)

    override fun price(): Int = 0
}
