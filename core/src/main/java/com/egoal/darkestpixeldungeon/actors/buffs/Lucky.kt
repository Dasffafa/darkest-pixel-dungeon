package com.egoal.darkestpixeldungeon.actors.buffs

import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.ui.BuffIndicator
import com.watabou.utils.Bundle

class Lucky : FlavourBuff() {
    var level = 1

    init {
        type = buffType.POSITIVE
    }

    override fun icon(): Int = BuffIndicator.BLESS

    override fun toString(): String = M.L(this, "name")

    override fun desc(): String = M.L(this, "desc", level, dispTurns())

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(LEVEL, level)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        level = bundle.getInt(LEVEL).coerceAtLeast(1)
    }

    companion object {
        const val MAX_LEVEL = 5
        const val DURATION = 200f

        private const val LEVEL = "level"
    }
}