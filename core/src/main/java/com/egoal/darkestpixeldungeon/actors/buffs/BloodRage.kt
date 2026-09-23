package com.egoal.darkestpixeldungeon.actors.buffs

import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.ui.BuffIndicator
import com.watabou.utils.Bundle
import kotlin.math.max

/**
 * Granted to a Mask of Madness wearer after a single hit for more than 40% of
 * max HP. Each attack made while active carries extra shadow damage equal to
 * 1/5 of the damage that triggered it.
 */
class BloodRage : FlavourBuff() {
    private var value = 0

    init {
        type = buffType.POSITIVE
    }

    /** Keeps the higher bonus when re-triggered. */
    fun setValue(v: Int) {
        value = max(value, v)
    }

    /** Extra shadow damage added to each attack. */
    fun bonus(): Int = value

    override fun icon(): Int = BuffIndicator.RAGE

    override fun toString(): String = Messages.get(this, "name")

    override fun heroMessage(): String? = Messages.get(this, "heromsg")

    override fun desc(): String = Messages.get(this, "desc", value, dispTurns())

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(VALUE, value)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        value = bundle.getInt(VALUE)
    }

    companion object {
        private const val VALUE = "value"
    }
}
