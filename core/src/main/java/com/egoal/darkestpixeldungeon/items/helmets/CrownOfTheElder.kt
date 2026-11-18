package com.egoal.darkestpixeldungeon.items.helmets

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.buffs.Amok
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.actors.buffs.Corruption
import com.egoal.darkestpixeldungeon.actors.buffs.Terror
import com.egoal.darkestpixeldungeon.actors.mobs.Mob
import com.egoal.darkestpixeldungeon.effects.CellEmitter
import com.egoal.darkestpixeldungeon.effects.particles.ShadowParticle
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.utils.Bundle
import com.watabou.utils.Random
import kotlin.math.round

class CrownOfTheElder : Helmet() {
    override val drawId: Int get() = 19
    override val replacesHead: Boolean get() = false

    init {
        image = ItemSpriteSheet.ELDER_CROWN
    }

    override fun random(): Item = apply {
        cursed = Random.Float() < 0.7f
    }

    override fun procGivenDamage(dmg: Damage) {
        val target = dmg.to as? Char ?: return
        if (!target.properties().contains(Char.Property.UNDEAD)) return

        val multiplier = if (cursed) 1.5f else 1.3f
        dmg.value = round(dmg.value * multiplier).toInt()
        if (dmg.add_value > 0) {
            dmg.add_value = round(dmg.add_value * multiplier).toInt()
        }

        if (target is Mob && target.isAlive && target.camp != Char.Camp.HERO) {
            applyUndeadDominion(target, dmg.from as? Char)
        }
    }

    private fun applyUndeadDominion(target: Mob, attacker: Char?) {
        val dread = Buff.affect(target, UndeadDread::class.java)
        val baseGain = if (cursed) PRESSURE_GAIN_CURSED else PRESSURE_GAIN_NORMAL
        val gain = (baseGain + Random.Float(-PRESSURE_JITTER, PRESSURE_JITTER)).coerceAtLeast(1f)
        if (dread.addPressure(gain) && canConvert(target)) {
            target.buff(Terror::class.java)?.detach()
            target.buff(Amok::class.java)?.detach()
            dread.detach()
            Buff.affect(target, Corruption::class.java)
            CellEmitter.center(target.pos).burst(ShadowParticle.CURSE, 6)
            target.sprite.showStatus(CharSprite.WARNING, M.L(this, "plea_fight"))
            return
        }

        val hasNearbyOthers = cursed && Dungeon.level.mobs.any {
            it !== target && it.isAlive && Dungeon.level.distance(it.pos, target.pos) <= AMOK_CHECK_RADIUS
        }

        if (hasNearbyOthers) {
            Buff.prolong(target, Amok::class.java, AMOK_DURATION)
            if (target.buff(Amok::class.java) != null) {
                val key = if (Random.Int(2) == 0) "plea_fight" else "plea_garbled"
                target.sprite.showStatus(CharSprite.WARNING, M.L(this, key))
            }
            return
        }

        val sourceId = attacker?.id() ?: Dungeon.hero.id()
        Buff.affect(target, Terror::class.java, Terror.DURATION).objectid = sourceId
        if (target.buff(Terror::class.java) != null) {
            val key = when (Random.Int(3)) {
                0 -> "plea_punish"
                1 -> "plea_forgive"
                else -> "plea_garbled"
            }
            target.sprite.showStatus(CharSprite.WARNING, M.L(this, key))
        }
    }

    private fun canConvert(target: Mob): Boolean =
        !target.properties().contains(Char.Property.BOSS) &&
            !target.properties().contains(Char.Property.MINIBOSS) &&
            !target.immunizedBuffs().contains(Corruption::class.java)

    /** Returns elemental resistance modifier for [element]; negative values increase vulnerability. */
    fun elementalResistanceAmend(element: Damage.Element): Float = when (element) {
        Damage.Element.Shadow -> if (cursed) 0.35f else 0.25f
        Damage.Element.Light -> if (cursed) -0.45f else -0.25f
        else -> 0f
    }

    override fun price(): Int = 500

    companion object {
        private const val AMOK_CHECK_RADIUS = 4
        private const val AMOK_DURATION = 4f
        private const val PRESSURE_GAIN_NORMAL = 25f
        private const val PRESSURE_GAIN_CURSED = 33f
        private const val PRESSURE_JITTER = 10f
        private const val MAX_UNDEAD_PRESSURE = 100f
    }

    class UndeadDread : Buff() {
        private var pressure = 0f

        /** Adds [amount] pressure and returns true once the conversion threshold is reached. */
        fun addPressure(amount: Float): Boolean {
            pressure = (pressure + amount).coerceAtMost(MAX_UNDEAD_PRESSURE)
            return pressure >= MAX_UNDEAD_PRESSURE
        }

        override fun storeInBundle(bundle: Bundle) {
            super.storeInBundle(bundle)
            bundle.put(PRESSURE_KEY, pressure)
        }

        override fun restoreFromBundle(bundle: Bundle) {
            super.restoreFromBundle(bundle)
            pressure = bundle.getFloat(PRESSURE_KEY)
        }

        companion object {
            private const val PRESSURE_KEY = "undead_pressure"
        }
    }
}
