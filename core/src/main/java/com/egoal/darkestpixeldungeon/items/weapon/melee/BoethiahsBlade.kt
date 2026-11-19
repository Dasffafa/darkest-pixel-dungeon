package com.egoal.darkestpixeldungeon.items.weapon.melee

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Actor
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.effects.CellEmitter
import com.egoal.darkestpixeldungeon.effects.Speck
import com.egoal.darkestpixeldungeon.effects.Wound
import com.egoal.darkestpixeldungeon.effects.particles.ShadowParticle
import com.egoal.darkestpixeldungeon.items.EquipableItem
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.armor.Armor
import com.egoal.darkestpixeldungeon.items.weapon.Inscription
import com.egoal.darkestpixeldungeon.items.weapon.Weapon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.egoal.darkestpixeldungeon.utils.GLog
import com.egoal.darkestpixeldungeon.windows.WndBag
import com.egoal.darkestpixeldungeon.windows.WndOptions
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min

class BoethiahsBlade : MeleeWeapon() {
    enum class SacrificeMode {
        NONE,
        GREED,
        VIOLENT,
        EBONY
    }

    var sacrificeMode: SacrificeMode = SacrificeMode.NONE
        private set

    var enchantPoints: Int = 0
        private set

    private var sacrificedTier: Int = 3
    private var sacrificedLevel: Int = 0
    private var goldDrainAcc: Float = 0f
    private var ebonyTickAcc: Float = 0f

    init {
        image = ItemSpriteSheet.BOETHIAHS_BLADE
        tier = 3
    }

    val isGreedActive: Boolean get() = sacrificeMode == SacrificeMode.GREED && enchantPoints > 0
    val isViolentActive: Boolean get() = sacrificeMode == SacrificeMode.VIOLENT && enchantPoints > 0
    val isEbonyActive: Boolean get() = sacrificeMode == SacrificeMode.EBONY && enchantPoints > 0

    private fun effectiveTier(): Int = when {
        isGreedActive -> 3 + Dungeon.gold / 1000
        isViolentActive -> max(3, sacrificedTier)
        else -> 3
    }

    override fun level(): Int = super.level() + if (isViolentActive) sacrificedLevel else 0

    override fun visiblyUpgraded(): Int = if (levelKnown) level() else 0

    override fun min(lvl: Int): Int = effectiveTier() + lvl

    override fun max(lvl: Int): Int = 5 * (effectiveTier() + 1) + lvl * (effectiveTier() + 1)

    override fun STRReq(lvl: Int): Int {
        val l = max(0, lvl)
        return imbue.strFix() + 8 + effectiveTier() * 2 - (l + 1) / 2 - 1
    }

    override fun glowing(): ItemSprite.Glowing? = when {
        isGreedActive -> GLOW_GREED
        isViolentActive -> GLOW_VIOLENT
        isEbonyActive -> GLOW_EBONY
        else -> super.glowing()
    }

    override fun actions(hero: Hero): ArrayList<String> {
        val actions = super.actions(hero)
        if (sacrificeMode == SacrificeMode.NONE || enchantPoints <= 0) {
            actions.add(AC_SACRIFICE)
        }
        return actions
    }

    override fun proc(dmg: Damage): Damage {
        val attacker = dmg.from as Char
        val defender = dmg.to as Char

        if (isViolentActive) {
            dmg.value = (dmg.value * 1.30f).toInt()
        }

        val shadowDmg = if (isGreedActive) {
            max(1, (dmg.value * 0.10f).toInt())
        } else {
            Random.Int(2, max(3, dmg.value / 4))
        }
        dmg.setAdditionalDamage(Damage.Element.Shadow, shadowDmg)

        if (dmg.isFeatured(Damage.Feature.CRITICAL))
            dmg.value += (2 + level()) * effectiveTier()

        val dht = if (cursed) {
            level() * 3 / 2 + 1
        } else level() + 1

        val ht = defender.HT
        defender.HT = max(1, defender.HT - dht)
        defender.HP = min(defender.HP, defender.HT)

        val drained = ht - defender.HT
        if (drained > 0) {
            when {
                isGreedActive -> {
                    Dungeon.gold += drained
                    if (attacker is Hero) {
                        attacker.sprite.showStatus(CharSprite.NEUTRAL, "+$drained")
                    }
                }
                isEbonyActive -> attacker.SHLD += drained * 2
                else -> attacker.SHLD += drained
            }
        }

        if (sacrificeMode != SacrificeMode.NONE && enchantPoints > 0) {
            enchantPoints--
            if (enchantPoints <= 0) {
                clearSacrifice()
                GLog.w(M.L(this, "enchant_expired"))
            }
            updateQuickslot()
        }

        return super.proc(dmg)
    }

    fun onHeroSpend(hero: Hero, time: Float) {
        if (sacrificeMode == SacrificeMode.NONE || enchantPoints <= 0) return

        if (isGreedActive) {
            goldDrainAcc += time
            if (goldDrainAcc >= 5f) {
                val ticks = (goldDrainAcc / 5f).toInt()
                goldDrainAcc -= ticks * 5f
                if (Dungeon.gold > 0) {
                    Dungeon.gold = max(0, Dungeon.gold - ticks)
                }
            }
        } else if (isEbonyActive) {
            ebonyTickAcc += time
            if (ebonyTickAcc >= 1f) {
                ebonyTickAcc -= 1f
                val auraDmg = 1 + sacrificedTier + sacrificedLevel / 2
                for (offset in PathFinder.NEIGHBOURS8) {
                    val pos = hero.pos + offset
                    val ch = Actor.findChar(pos)
                    if (ch != null && ch.isAlive && ch !== hero && ch.camp != hero.camp) {
                        ch.takeDamage(
                            Damage(hero, ch, Damage.Type.MAGICAL)
                                .setAdditionalDamage(Damage.Element.Shadow, auraDmg)
                        )
                        if (Dungeon.visible[pos]) {
                            CellEmitter.get(pos).burst(ShadowParticle.UP, 2)
                        }
                    }
                }
            }
        }
    }

    override fun doUnequip(hero: Hero, collect: Boolean, single: Boolean): Boolean {
        GLog.n(M.L(this, "unequip_first"))
        return false
    }

    override fun execute(hero: Hero, action: String) {
        when (action) {
            AC_UNEQUIP -> {
                WndOptions.Confirm(ItemSprite(this), name, M.L(this, "unequip_warn", name)) {
                    unequip(hero)
                }
            }
            AC_SACRIFICE -> {
                if (sacrificeMode != SacrificeMode.NONE && enchantPoints > 0) {
                    GLog.w(M.L(this, "already_enchanted"))
                    return
                }
                WndOptions.Show(
                    ItemSprite(this),
                    M.L(this, "sacrifice_title"),
                    M.L(this, "sacrifice_prompt"),
                    M.L(this, "sacrifice_gold"),
                    M.L(this, "sacrifice_weapon"),
                    M.L(this, "sacrifice_armor")
                ) { idx ->
                    when (idx) {
                        0 -> sacrificeGold(hero)
                        1 -> GameScene.selectItem(weaponSelector, WndBag.Mode.WEAPON, M.L(this, "select_weapon"))
                        2 -> GameScene.selectItem(armorSelector, WndBag.Mode.ARMOR, M.L(this, "select_armor"))
                    }
                }
            }
            else -> super.execute(hero, action)
        }
    }

    private fun sacrificeGold(hero: Hero) {
        if (Dungeon.gold < GOLD_SACRIFICE_COST) {
            GLog.w(M.L(this, "not_enough_gold", GOLD_SACRIFICE_COST))
            return
        }
        Dungeon.gold -= GOLD_SACRIFICE_COST
        sacrificeMode = SacrificeMode.GREED
        enchantPoints = 30
        sacrificedTier = 3
        sacrificedLevel = 0
        goldDrainAcc = 0f

        hero.sprite.emitter().burst(Speck.factory(Speck.COIN), 10)
        Sample.INSTANCE.play(Assets.SND_GOLD)
        hero.spendAndNext(1f)
        updateQuickslot()
        GLog.p(M.L(this, "enchant_greed"))
    }

    private val weaponSelector = WndBag.Listener { item ->
        if (item == null) return@Listener
        val hero = Dungeon.hero
        if (item === this@BoethiahsBlade || item !is MeleeWeapon) {
            GLog.w(M.L(this@BoethiahsBlade, "invalid_sacrifice"))
            return@Listener
        }
        if (item.isEquipped(hero) && item.cursed) {
            GLog.w(M.L(EquipableItem::class.java, "unequip_cursed"))
            return@Listener
        }
        if (item.isEquipped(hero)) item.doUnequip(hero, false)
        else item.detach(hero.belongings.backpack)

        val rawTier = max(1, item.tier)
        val rawLevel = max(0, item.level())
        sacrificeMode = SacrificeMode.VIOLENT
        sacrificedTier = max(3, rawTier)
        sacrificedLevel = rawLevel
        enchantPoints = 10 * rawTier + 20 * rawLevel

        hero.sprite.emitter().burst(ShadowParticle.CURSE, 10)
        Sample.INSTANCE.play(Assets.SND_CURSED)
        hero.spendAndNext(1f)
        updateQuickslot()
        GLog.p(M.L(this@BoethiahsBlade, "enchant_violent", enchantPoints))
    }

    private val armorSelector = WndBag.Listener { item ->
        if (item == null) return@Listener
        val hero = Dungeon.hero
        if (item !is Armor) {
            GLog.w(M.L(this@BoethiahsBlade, "invalid_sacrifice"))
            return@Listener
        }
        if (item.isEquipped(hero) && item.cursed) {
            GLog.w(M.L(EquipableItem::class.java, "unequip_cursed"))
            return@Listener
        }
        if (item.isEquipped(hero)) item.doUnequip(hero, false)
        else item.detach(hero.belongings.backpack)

        val rawTier = max(1, item.tier)
        val rawLevel = max(0, item.level())
        sacrificeMode = SacrificeMode.EBONY
        sacrificedTier = rawTier
        sacrificedLevel = rawLevel
        enchantPoints = 15 * rawTier + 20 * rawLevel
        ebonyTickAcc = 0f

        hero.sprite.emitter().burst(ShadowParticle.UP, 10)
        Sample.INSTANCE.play(Assets.SND_CURSED)
        hero.spendAndNext(1f)
        updateQuickslot()
        GLog.p(M.L(this@BoethiahsBlade, "enchant_ebony", enchantPoints))
    }

    private fun clearSacrifice() {
        sacrificeMode = SacrificeMode.NONE
        enchantPoints = 0
        sacrificedTier = 3
        sacrificedLevel = 0
        goldDrainAcc = 0f
        ebonyTickAcc = 0f
    }

    override fun info(): String {
        var info = desc()
        val t = effectiveTier()

        if (levelKnown) {
            info += "\n\n" + M.L(MeleeWeapon::class.java, "stats_known", t,
                imbue.damageFactor(min()), imbue.damageFactor(max()), STRReq())
            if (STRReq() > Dungeon.hero.STR()) {
                info += " " + M.L(Weapon::class.java, "too_heavy")
            } else if (Dungeon.hero.STR() > STRReq()) {
                info += " " + M.L(Weapon::class.java, "excess_str", Dungeon.hero.STR() - STRReq())
            }
        } else {
            info += "\n\n" + M.L(MeleeWeapon::class.java, "stats_unknown", t,
                min(0), max(0), STRReq(0))
        }

        val statsDesc = M.L(this, "stats_desc")
        if (statsDesc.isNotEmpty()) info += "\n\n" + statsDesc

        if (sacrificeMode != SacrificeMode.NONE && enchantPoints > 0) {
            val modeKey = when (sacrificeMode) {
                SacrificeMode.GREED -> "mode_greed_desc"
                SacrificeMode.VIOLENT -> "mode_violent_desc"
                SacrificeMode.EBONY -> "mode_ebony_desc"
                SacrificeMode.NONE -> ""
            }
            if (modeKey.isNotEmpty()) {
                info += "\n\n" + M.L(this, modeKey, enchantPoints)
            }
        }

        for (insc in inscriptions) {
            if (cursedKnown || !insc.curse) {
                info += "\n\n" + M.L(Weapon::class.java, "inscribed", insc.name())
                info += " " + M.L(insc, "desc")
            }
        }
        for (enc in enchantments) {
            info += "\n\n" + M.L(Weapon::class.java, "enchanted", enc.name())
            info += " " + enc.desc()
        }
        if (cursed && isEquipped(Dungeon.hero)) {
            info += "\n\n" + M.L(Weapon::class.java, "cursed_worn")
        } else if (cursedKnown && cursed) {
            info += "\n\n" + M.L(Weapon::class.java, "cursed")
        }

        return info
    }

    override fun doPickUp(hero: Hero): Boolean {
        GLog.n(M.L(this, "cannot_pickup"))
        return false
    }

    private fun unequip(hero: Hero) {
        if (cursed) {
            GLog.w(M.L(EquipableItem::class.java, "unequip_cursed"))
            return
        }

        hero.spendAndNext(time2equip(hero))

        onDetach()
        Dungeon.quickslot.clearItem(this)
        updateQuickslot()
        Dungeon.level.drop(this, hero.pos)

        hero.belongings.weapon = null

        val equipToCurse = hero.belongings.equippedItems().filter { it != null && !it.cursed }
        if (equipToCurse.isEmpty()) {
            hero.regeneration -= 0.2f
            Wound.hit(hero)
        } else {
            val item = equipToCurse.random() as Item
            item.cursed = true
            item.cursedKnown = true

            if (item is Weapon) item.inscribe(Inscription.randomNegative())
            else if (item is Armor) item.glyph = Armor.Glyph.randomCurse()

            CellEmitter.get(hero.pos).burst(ShadowParticle.CURSE, 5)
            Sample.INSTANCE.play(Assets.SND_BURNING)
        }
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(SACRIFICE_MODE, sacrificeMode.name)
        bundle.put(ENCHANT_POINTS, enchantPoints)
        bundle.put(SACRIFICED_TIER, sacrificedTier)
        bundle.put(SACRIFICED_LEVEL, sacrificedLevel)
        bundle.put(GOLD_DRAIN_ACC, goldDrainAcc)
        bundle.put(EBONY_TICK_ACC, ebonyTickAcc)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        sacrificeMode = try {
            SacrificeMode.valueOf(bundle.getString(SACRIFICE_MODE))
        } catch (_: Exception) {
            SacrificeMode.NONE
        }
        enchantPoints = bundle.getInt(ENCHANT_POINTS)
        sacrificedTier = bundle.getInt(SACRIFICED_TIER)
        sacrificedLevel = bundle.getInt(SACRIFICED_LEVEL)
        goldDrainAcc = bundle.getFloat(GOLD_DRAIN_ACC)
        ebonyTickAcc = bundle.getFloat(EBONY_TICK_ACC)
    }

    companion object {
        private const val AC_SACRIFICE = "sacrifice"
        private const val GOLD_SACRIFICE_COST = 500

        private const val SACRIFICE_MODE = "sacrifice_mode"
        private const val ENCHANT_POINTS = "enchant_points"
        private const val SACRIFICED_TIER = "sacrificed_tier"
        private const val SACRIFICED_LEVEL = "sacrificed_level"
        private const val GOLD_DRAIN_ACC = "gold_drain_acc"
        private const val EBONY_TICK_ACC = "ebony_tick_acc"

        private val GLOW_GREED = ItemSprite.Glowing(0xFFD700)
        private val GLOW_VIOLENT = ItemSprite.Glowing(0xFF3333)
        private val GLOW_EBONY = ItemSprite.Glowing(0x3B234A)
    }
}
