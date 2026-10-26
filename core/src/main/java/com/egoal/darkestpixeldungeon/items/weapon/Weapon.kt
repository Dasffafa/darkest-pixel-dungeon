/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.egoal.darkestpixeldungeon.items.weapon

import com.egoal.darkestpixeldungeon.Badges
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.perks.EnchantmentExtraDamage
import com.egoal.darkestpixeldungeon.actors.hero.perks.ExtraStrengthPower
import com.egoal.darkestpixeldungeon.items.Catalog
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.KindOfWeapon
import com.egoal.darkestpixeldungeon.items.rings.Ring
import com.egoal.darkestpixeldungeon.items.rings.RingOfFuror
import com.egoal.darkestpixeldungeon.items.rings.RingOfSharpshooting
import com.egoal.darkestpixeldungeon.items.weapon.curses.Wayward
import com.egoal.darkestpixeldungeon.items.weapon.inscriptions.Projecting
import com.egoal.darkestpixeldungeon.items.weapon.missiles.MissileWeapon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.utils.GLog
import com.watabou.utils.Bundle
import com.watabou.utils.Random
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

abstract class Weapon : KindOfWeapon() {
    var ACC = 1f  // Accuracy modifier
    var DLY = 1f  // Speed modifier
    var RCH = 1    // Reach modifier (only applies to melee hits)

    var imbue = Imbue.NONE

    private var hitsToKnow = HITS_TO_KNOW

    /** Inscriptions held by this weapon, in the order they were acquired. */
    val inscriptions: MutableList<Inscription> = ArrayList()

    /** How many inscriptions this weapon can hold at once. */
    open val maxInscriptions: Int get() = 1

    /** The most recently acquired inscription. Assigning replaces every inscription. */
    var inscription: Inscription?
        get() = inscriptions.lastOrNull()
        set(value) {
            inscriptions.clear()
            if (value != null) inscriptions.add(value)
        }

    /** Enchantments held by this weapon, in the order they were acquired. */
    val enchantments: MutableList<Enchantment> = ArrayList()

    /** How many enchantments this weapon can hold at once. */
    open val maxEnchantments: Int get() = 1

    /** The most recently acquired enchantment. Assigning replaces every enchantment. */
    var enchantment: Enchantment?
        get() = enchantments.lastOrNull()
        set(value) {
            enchantments.clear()
            if (value != null) enchantments.add(value)
        }

    enum class Imbue(private val damageFactor: Float, private val delayFactor: Float, private val strFix: Int) {
        NONE(1.0f, 1.00f, 0),
        LIGHT(0.7f, 0.67f, -1),
        HEAVY(1.5f, 1.67f, 1);

        fun strFix(): Int {
            return strFix
        }

        fun damageFactor(dmg: Int): Int = round(dmg * damageFactor).toInt()

        fun delayFactor(dly: Float): Float = dly * delayFactor
    }

    override fun proc(dmg: Damage): Damage {
        var dmg = dmg
        for (insc in inscriptions) dmg = insc.proc(this, dmg)
        // snapshot: an enchantment may expire and remove itself while it procs
        for (enc in ArrayList(enchantments)) {
            dmg = enc.proc(this, dmg)
            if (dmg.from is Hero) {
                Dungeon.hero.heroPerk.get(EnchantmentExtraDamage::class.java)?.procDamage(dmg)
            }
        }

        if (!levelKnown) {
            if (--hitsToKnow <= 0) {
                levelKnown = true
                GLog.i(Messages.get(Weapon::class.java, "identify"))
                Badges.validateItemLevelAquired(this)
            }
        }

        return dmg
    }

    override fun onFirstSeen(hero: Hero) {
        // missiles carry no level, only melee weapons benefit
        if (this !is MissileWeapon && !isIdentified) rollRareQuality(hero)
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(UNFAMILIRIARITY, hitsToKnow)
        bundle.put(INSCRIPTIONS, inscriptions)
        bundle.put(ENCHANTMENTS, enchantments)
        bundle.put(IMBUE, imbue)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        hitsToKnow = bundle.getInt(UNFAMILIRIARITY)

        inscriptions.clear()
        for (b in bundle.getCollection(INSCRIPTIONS)) (b as? Inscription)?.let { inscriptions.add(it) }
        // saves from before multi-inscription support carry a single entry
        if (inscriptions.isEmpty() && bundle.contains(INSCRIPTION))
            (bundle.get(INSCRIPTION) as? Inscription)?.let { inscriptions.add(it) }

        enchantments.clear()
        for (b in bundle.getCollection(ENCHANTMENTS)) (b as? Enchantment)?.let { enchantments.add(it) }
        // saves from before multi-enchantment support carry a single entry
        if (enchantments.isEmpty() && bundle.contains(ENCHANTMENT))
            (bundle.get(ENCHANTMENT) as? Enchantment)?.let { enchantments.add(it) }

        imbue = bundle.getEnum(IMBUE, Imbue::class.java)
    }

    override fun accuracyFactor(hero: Hero, target: Char): Float {
        var encumbrance = STRReq() - hero.STR()

        if (isInscribed(Wayward::class.java))
            encumbrance = max(3, encumbrance + 3)

        var acc = ACC

        if (this is MissileWeapon) {
            val bonus = Ring.getBonus(hero, RingOfSharpshooting.Aim::class.java)
            acc *= 1.1f.pow(bonus)
        }

        return if (encumbrance > 0) acc / 1.5f.pow(encumbrance) else acc
    }

    override fun speedFactor(hero: Hero): Float {
        val encumbrance = STRReq() - hero.STR()

        var dly = imbue.delayFactor(DLY)

        val bonus = Ring.getBonus(hero, RingOfFuror.Furor::class.java)

        dly = 0.25f + (dly - 0.25f) * 0.8f.pow(bonus)

        return if (encumbrance > 0) dly * 1.2f.pow(encumbrance) else dly
    }

    override fun reachFactor(hero: Hero): Int = if (isInscribed(Projecting::class.java)) RCH + 1 else RCH

    override fun giveDamage(hero: Hero, target: Char): Damage {
        val dmg = super.giveDamage(hero, target)

        // extra damage
        val exStr = hero.STR() - STRReq()
        if (exStr > 0) {
            dmg.value += Random.Int(1, exStr)
            hero.heroPerk.get(ExtraStrengthPower::class.java)?.affectDamage(dmg, exStr)
        }

        dmg.value = imbue.damageFactor(dmg.value)
        return dmg
    }

    fun STRReq(): Int = STRReq(level())

    abstract fun STRReq(lvl: Int): Int

    open fun upgrade(inscribe: Boolean): Item {
        if (inscribe && !hasGoodInscription()) {
            this.inscribe()
        } else if (!inscribe && inscriptions.isNotEmpty() && Random.Float() > 0.9f.pow(level())) {
            inscriptions.removeAt(inscriptions.size - 1)
        }

        return super.upgrade()
    }

    override fun name(): String {
        val visible = inscriptions.filter { cursedKnown || !it.curse }
        return if (visible.isEmpty()) super.name()
        else visible.fold(super.name()) { acc, insc -> insc.name(acc) }
    }

    override fun random(): Item {
        val roll = Random.Float()
        if (roll < 0.3f) {
            //30% chance to be level 0 and cursed
            inscribe(Inscription.randomNegative())
            cursed = true
            return this
        } else if (roll < 0.75f) {
            //45% chance to be level 0
        } else if (roll < 0.95f) {
            //15% chance to be +1
            upgrade(1)
        } else {
            //5% chance to be +2
            upgrade(2)
        }

        //if not cursed, 10% chance to be enchanted (7% overall)
        if (Random.Int(10) == 0)
            inscribe()

        return this
    }

    open fun inscribe(insc: Inscription?): Weapon {
        if (insc == null) {
            inscriptions.clear()
        } else {
            // once full, the earliest acquisition makes room for the new one
            if (inscriptions.size >= maxInscriptions) inscriptions.removeAt(0)
            inscriptions.add(insc)

            // only reveal once the hero knows about it, generation applies curses too
            if (cursedKnown) Catalog.SetSeen(insc.javaClass)
        }

        return this
    }

    open fun inscribe(): Weapon {
        val held = inscriptions.map { it.javaClass }
        var new = Inscription.randomPositive()
        while (new.javaClass in held) new = Inscription.randomPositive()

        return inscribe(new)
    }

    open fun isInscribed(type: Class<out Inscription>): Boolean = inscriptions.any { it.javaClass == type }

    fun hasGoodInscription(): Boolean = inscriptions.any { !it.curse }
    open fun hasCurseInscription(): Boolean = inscriptions.any { it.curse }

    open fun clearCurseInscription(): Boolean {
        if (!hasCurseInscription()) return false
        inscriptions.removeAll { it.curse }
        return true
    }

    open fun enchant(type: Class<out Enchantment>, duration: Float): Weapon {
        // prolong the same enchantment if present, otherwise take a slot
        val existing = enchantments.firstOrNull { it.javaClass == type }
        if (existing != null) {
            existing.left = max(existing.left, duration)
        } else {
            // once full, the earliest acquisition makes room for the new one
            if (enchantments.size >= maxEnchantments) enchantments.removeAt(0)
            enchantments.add(type.newInstance().apply { left = duration })
        }

        if (cursedKnown) Catalog.SetSeen(type)
        GLog.w(M.L(Weapon::class.java, "on_enchanted", name(), enchantments.first { it.javaClass == type }.name()))

        updateQuickslot()
        return this
    }

    open fun hasEnchant(type: Class<out Enchantment>): Boolean = enchantments.any { it.javaClass == type }

    /** Removes a single enchantment, e.g. when it runs out of duration. */
    fun removeEnchantment(enc: Enchantment) {
        enchantments.remove(enc)
    }

    override fun identify(): Item {
        enchantments.forEach { Catalog.SetSeen(it.javaClass) }
        inscriptions.forEach { Catalog.SetSeen(it.javaClass) }
        return super.identify()
    }

    override fun glowing(): ItemSprite.Glowing? = enchantment?.glowing()

    companion object {
        private const val HITS_TO_KNOW = 30

        private const val UNFAMILIRIARITY = "unfamiliarity"
        private const val INSCRIPTION = "inscription"
        private const val INSCRIPTIONS = "inscriptions"
        private const val ENCHANTMENT = "enchantment"
        private const val ENCHANTMENTS = "enchantments"
        private const val IMBUE = "imbue"
    }
}
