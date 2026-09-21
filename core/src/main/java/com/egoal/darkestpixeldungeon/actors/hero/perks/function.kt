package com.egoal.darkestpixeldungeon.actors.hero.perks

import com.watabou.noosa.Game
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.Statistics
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.actors.buffs.Hunger
import com.egoal.darkestpixeldungeon.actors.buffs.Recharging
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.egoal.darkestpixeldungeon.actors.hero.HeroSubClass
import com.egoal.darkestpixeldungeon.effects.Flare
import com.egoal.darkestpixeldungeon.effects.Speck
import com.egoal.darkestpixeldungeon.items.Generator
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.food.Food
import com.egoal.darkestpixeldungeon.items.potions.Potion
import com.egoal.darkestpixeldungeon.items.scrolls.ScrollOfRecharging
import com.egoal.darkestpixeldungeon.items.unclassified.DewVial
import com.egoal.darkestpixeldungeon.items.unclassified.Gold
import com.egoal.darkestpixeldungeon.items.unclassified.Rune
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.utils.GLog
import com.watabou.utils.Bundle
import kotlin.math.min
import kotlin.math.round

class BrewEnhancedPotion : Perk() {
    init {
        addTags(Tag.Viability)
    }

    override fun image(): Int = PerkImageSheet.BREW_ENHANCED
    private var nextProb = -1f

    private fun baseStep(hero: Hero = Dungeon.hero): Float {
        val wealth = if (!Dungeon.isHeroNull) hero.wealthBonus() else 0
        return (0.333f * (1f + 0.2f * wealth)).coerceAtLeast(0.01f)
    }

    fun affectPotion(p: Potion, hero: Hero = Dungeon.hero) {
        val step = baseStep(hero)
        if (nextProb < 0f) nextProb = step

        if (p.canBeReinforced()) {
            if (com.watabou.utils.Random.Float() < nextProb) {
                p.reinforce()
                nextProb = step

                Game.runOnRenderThread {
                    GameScene.effect(Flare(7, 32f).color(0xffccdd, true).show(
                            hero.sprite.parent, DungeonTilemap.tileCenterToWorld(hero.pos), 2f))
                }
                return
            }
        }
        nextProb += step
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put("nextprob", nextProb)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        nextProb = if (bundle.contains("nextprob")) bundle.getFloat("nextprob") else -1f
    }
}

// see Hunger
class Dieting : Perk() {
    override fun image(): Int = PerkImageSheet.DIETING
}

// note: this perk can be negative level, i havnt abstract this, but it works for now.
class Discount : Perk(2) {
    override val isNegative: Boolean get() = level < 0

    override fun image(): Int = if (level > 0) PerkImageSheet.DISCOUNT else PerkImageSheet.DISCOUNT_NEG

    fun buyPrice(item: Item): Int = (item.sellPrice() * ratio()).toInt()

    fun ratio(): Float = 1f - 0.25f * level
}

class Drunkard : Perk() {
    override fun image(): Int = PerkImageSheet.WINE_DRUNKARD

    override fun canBeGain(hero: Hero): Boolean = hero.heroClass != HeroClass.EXILE
}

class Ease : Perk() {
    override fun image(): Int = PerkImageSheet.EASE
}

class EfficientPotionOfHealing : Perk(3) {
    init {
        addTags(Tag.Viability)
    }

    override fun image(): Int = PerkImageSheet.POTION_EFF_HEALING
}

class EfficientSearch : Perk() {
    override fun image(): Int = PerkImageSheet.SEARCH_EFFICIENT
}

class ExtraPerkChoice : Perk() {
    override fun image(): Int = PerkImageSheet.EXTRA_CHOICE
}

class ExtraRuneRegularly : TimingPerk(GainRune::class.java) {
    init {
        addTags(Tag.Viability)
    }

    override fun image(): Int = PerkImageSheet.RUNE_EXTRA

    class GainRune : TimingPerk.Timing(Statistics.ClockTime.TimePerDay() / 2f) {
        override fun trigger() {
            val rune = Generator.RUNE.generate() as Rune
            Game.runOnRenderThread {
                GameScene.effect(Flare(7, 32f).color(rune.glowing()?.color ?: 0x66ff66, true).show(
                        Dungeon.hero.sprite.parent, DungeonTilemap.tileCenterToWorld(Dungeon.hero.pos), 2f))
            }

            val dewVial = Dungeon.hero.belongings.getItem(DewVial::class.java)
            if (dewVial == null || dewVial.rune != null) {
                Dungeon.level.drop(rune, Dungeon.hero.pos)
            } else {
                dewVial.rune = rune
                GLog.w(M.L(ExtraRuneRegularly::class.java, "generated", rune.name()))
            }
        }
    }
}

class ExtraStrength : Perk.Additional(3) {
    override fun image(): Int = PerkImageSheet.STRENGTH_EXTRA

    override fun onLose() {
        super.onLose()
        Dungeon.hero.STR -= str()
    }

    override fun onGain() {
        Dungeon.hero.STR += str()
    }

    private fun str(): Int = level

    override fun description(): String = M.L(this, "desc", level)
}

class GoodAppetite : Perk() {
    override fun image(): Int = PerkImageSheet.APPETITE_GOOD

    fun onFoodEaten(hero: Hero, food: Food) {
        when (hero.heroClass) {
            HeroClass.WARRIOR -> {
                if (hero.HP < hero.HT) {
                    hero.HP = min(hero.HP + 5, hero.HT)
                    hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1)
                }
            }
            HeroClass.MAGE -> {
                Buff.affect(hero, Recharging::class.java, 4f)
                ScrollOfRecharging.charge(hero)
            }
            HeroClass.SORCERESS -> {
                hero.buff(Hunger::class.java)!!.satisfy(food.enery * 0.25f)
            }
            else -> {}
        }
    }

    override fun canBeGain(hero: Hero): Boolean =
            hero.heroClass in listOf(HeroClass.WARRIOR, HeroClass.MAGE, HeroClass.SORCERESS)
}

class GreedyMidas : Perk() {
    init {
        addTags(Tag.Viability)
    }

    override fun image(): Int = PerkImageSheet.GREEDY_MIDAS

    fun procGold(gold: Gold, hero: Hero = Dungeon.hero) = gold.apply {
        val wealth = if (!Dungeon.isHeroNull) hero.wealthBonus() else 0
        val bestProb = (0.001f + 0.0033f * wealth).coerceAtLeast(0f)
        val highProb = (0.33f + 0.0333f * wealth).coerceAtLeast(0f)

        val p = com.watabou.utils.Random.Float()
        val multiplier = when {
            p < bestProb -> 10f
            p < bestProb + highProb -> 4.33f
            else -> 1f
        }
        val q = gold.quantity() * multiplier
        quantity(round(q).toInt())
    }
}

class IntendedTransportation : Perk() {
    override fun image(): Int = PerkImageSheet.TRANSPORTATION
}

class Keen : Perk(3) {
    override fun image(): Int = PerkImageSheet.KEEN

    fun baseAwareness(): Float = 0.1f + 0.1f * level
}

class Knowledgeable : Perk(3) {
    override fun image(): Int = PerkImageSheet.KNOWLEDGE

    fun affectItem(item: Item, hero: Hero = Dungeon.hero) {
        if (item.isIdentified) return

        val chance = identifyChance(hero)
        if (com.watabou.utils.Random.Float() < chance) {
            item.identify()
            GLog.w(M.L(this, "identity"))
        } else if (!item.cursedKnown && com.watabou.utils.Random.Float() < chance) {
            item.cursedKnown = true
            GLog.w(M.L(this, "know-curse"))
        }
    }

    private fun identifyChance(hero: Hero = Dungeon.hero): Float {
        val wealth = if (!Dungeon.isHeroNull) hero.wealthBonus() else 0
        return ((0.05f + 0.2f * level) * (1f + 0.2f * wealth)).coerceIn(0f, 1f)
    }
}

class LevelPerception : Perk() {
    override fun image(): Int = PerkImageSheet.LEVEL_PERCEPTION
}

class NightVision : Perk(2) {
    override fun image(): Int = PerkImageSheet.NIGHT_VISION

    override fun canBeGain(hero: Hero): Boolean {
        return hero.subClass == HeroSubClass.MOONRIDER || !hero.heroPerk.has(NightVision::class.java)
    }
}

class QuickLearner : Perk(3) {
    override fun image(): Int = PerkImageSheet.EXP_EXTRA

    private var dexp = 0f

    override fun description(): String = M.L(this, "desc", (ratio() * 100).toInt())

    fun extraExp(exp: Int): Int {
        dexp += exp * ratio()
        val e = dexp.toInt()
        dexp -= e

        return e
    }

    private fun ratio(): Float = 0.05f + level * 0.1f

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put("dexp", dexp)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        dexp = bundle.getFloat("dexp")
    }
}

class Telepath : Perk(2) {
    override fun image(): Int = PerkImageSheet.TELEPATH

    override fun canBeGain(hero: Hero): Boolean {
        return hero.heroClass == HeroClass.HUNTRESS || !hero.heroPerk.has(Telepath::class.java)
    }

    fun range() = 2 * level // 2, 4
}

// see Hero::onKillChar
class FastMoveOnKilling : Perk() {
    override fun image(): Int = PerkImageSheet.FastMoveOnKilling
}