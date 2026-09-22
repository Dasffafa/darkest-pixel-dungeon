package com.egoal.darkestpixeldungeon.actors.buffs

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.ui.BuffIndicator
import com.watabou.utils.Bundle
import kotlin.math.max

class ElementBroken : FlavourBuff() {

    // stores the resistance reduction amount for each element (e.g. 0.25f for -25% resistance)
    private val reductions = FloatArray(Damage.Element.values().size) { 0f }

    init {
        type = buffType.NEGATIVE
    }

    fun add(element: Damage.Element, ratio: Float): ElementBroken {
        val reduction = max(0f, ratio - 1f)
        reductions[element.ordinal] = max(reductions[element.ordinal], reduction)
        return this
    }

    fun resistanceReduction(element: Damage.Element): Float = reductions[element.ordinal]

    override fun icon(): Int = BuffIndicator.ELEMENT_BROKEN

    override fun toString(): String = M.L(this, "name")

    override fun desc(): String {
        val elestr = reductions.indices
                .filter { reductions[it] > 0f }
                .joinToString { Damage.Element.values()[it].textName }
        return M.L(this, "desc", elestr, dispTurns())
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put("reductions", reductions)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        if (bundle.contains("reductions")) {
            bundle.getFloatArray("reductions")?.copyInto(reductions)
        } else if (bundle.contains("ratio")) {
            // backward compatibility for old saves that stored ratios instead of reductions
            val oldRatios = bundle.getFloatArray("ratio")
            if (oldRatios != null) {
                for (i in oldRatios.indices) {
                    if (i < reductions.size) {
                        reductions[i] = max(0f, oldRatios[i] - 1f)
                    }
                }
            }
        }
    }
}