package com.egoal.darkestpixeldungeon.items.unclassified

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Badges
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.Statistics
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.effects.Speck
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.Random

class GoldenEffigy(
    private var mobName: String = "",
    private var goldValue: Int = 10
) : Item() {

    init {
        image = ItemSpriteSheet.GOLD
        stackable = false
    }

    override fun name(): String =
        if (mobName.isNotEmpty()) M.L(this, "name_of", mobName) else M.L(this, "name")

    override fun desc(): String = M.L(this, "desc", goldValue)

    override fun glowing(): ItemSprite.Glowing = GOLDEN_GLOW

    override fun doPickUp(hero: Hero): Boolean {
        Dungeon.gold += goldValue
        Statistics.GoldCollected += goldValue
        Badges.validateGoldCollected()

        GameScene.pickUp(this)
        hero.sprite.showStatus(CharSprite.NEUTRAL, "+$goldValue")
        hero.sprite.emitter().burst(Speck.factory(Speck.COIN), 6)
        Sample.INSTANCE.play(Assets.SND_GOLD, 1f, 1f, Random.Float(0.9f, 1.1f))
        hero.spendAndNext(TIME_TO_PICK_UP)
        return true
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(MOB_NAME, mobName)
        bundle.put(GOLD_VALUE, goldValue)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        mobName = bundle.getString(MOB_NAME)
        goldValue = bundle.getInt(GOLD_VALUE)
    }

    companion object {
        private val GOLDEN_GLOW = ItemSprite.Glowing(0xFFD700)
        private const val MOB_NAME = "mob_name"
        private const val GOLD_VALUE = "gold_value"
    }
}
