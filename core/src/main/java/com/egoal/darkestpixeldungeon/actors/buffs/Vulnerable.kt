package com.egoal.darkestpixeldungeon.actors.buffs

import com.egoal.darkestpixeldungeon.actors.Actor
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.ui.BuffIndicator
import com.watabou.utils.Bundle
import kotlin.math.round

class Vulnerable : Buff(), Char.IIncomingDamageProc {

    init {
        type = buffType.NEGATIVE
    }

    data class Entry(
            val ratio: Float,
            val dmgType: Damage.Type? = null,
            val element: Damage.Element? = null,
            var left: Float = 0f
    )

    private val entries = mutableListOf<Entry>()

    fun addEntry(ratio: Float, dmgType: Damage.Type, duration: Float) {
        entries.add(Entry(ratio, dmgType = dmgType, left = duration))
    }

    fun addEntry(ratio: Float, element: Damage.Element, duration: Float) {
        entries.add(Entry(ratio, element = element, left = duration))
    }

    override fun act(): Boolean {
        spend(Actor.TICK)
        entries.forEach { it.left -= Actor.TICK }
        entries.removeAll { it.left <= 0f }
        if (entries.isEmpty()) detach()
        return true
    }

    override fun icon(): Int = BuffIndicator.VULERABLE

    override fun toString(): String = M.L(this, "name")

    override fun desc(): String {
        val lines = mutableListOf<String>()

        val typeEntries = entries.filter { it.dmgType != null }.groupBy { it.dmgType!! }
        for ((dmgType, list) in typeEntries) {
            val maxRatio = list.maxOf { it.ratio }
            val maxLeft = list.maxOf { it.left }
            lines.add(M.L(this, if (maxRatio < 1f) "desc_1" else "desc_0",
                    dmgType.toString(), maxRatio, dispTurns(maxLeft + 1f)))
        }

        val elemEntries = entries.filter { it.element != null }.groupBy { it.element!! }
        for ((elem, list) in elemEntries) {
            val maxRatio = list.maxOf { it.ratio }
            val maxLeft = list.maxOf { it.left }
            lines.add(M.L(this, if (maxRatio < 1f) "desc_elem_1" else "desc_elem_0",
                    elem.textName, maxRatio, dispTurns(maxLeft + 1f)))
        }

        return lines.joinToString("\n")
    }

    override fun procIncommingDamage(damage: Damage) {
        val typeRatio = entries
                .filter { it.dmgType == damage.type }
                .maxOfOrNull { it.ratio }

        val elemRatio = if (damage.add_value > 0 || damage.value > 0) {
            entries
                    .filter { it.element == damage.element }
                    .maxOfOrNull { it.ratio }
        } else null

        if (damage.value > 0) {
            val effectiveValueRatio = when {
                typeRatio != null && elemRatio != null -> maxOf(typeRatio, elemRatio)
                typeRatio != null -> typeRatio
                elemRatio != null -> elemRatio
                else -> null
            }
            if (effectiveValueRatio != null) {
                damage.value = round(damage.value * effectiveValueRatio).toInt()
            }
        }

        if (damage.add_value > 0 && elemRatio != null) {
            damage.add_value = round(damage.add_value * elemRatio).toInt()
        }
    }

    override fun attachTo(target: Char): Boolean {
        if (entries.isEmpty()) {
            return false
        }
        return super.attachTo(target)
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(COUNT, entries.size)
        entries.forEachIndexed { i, e ->
            bundle.put(RATIO + i, e.ratio)
            bundle.put(LEFT + i, e.left)
            if (e.dmgType != null) {
                bundle.put(DAMAGE_TYPE + i, e.dmgType)
            }
            if (e.element != null) {
                bundle.put(ELEMENT + i, e.element)
            }
        }
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        if (bundle.contains(COUNT)) {
            val count = bundle.getInt(COUNT)
            repeat(count) { i ->
                val ratio = bundle.getFloat(RATIO + i)
                val left = bundle.getFloat(LEFT + i)
                val dmgType = if (bundle.contains(DAMAGE_TYPE + i))
                    bundle.getEnum(DAMAGE_TYPE + i, Damage.Type::class.java)
                else null
                val element = if (bundle.contains(ELEMENT + i))
                    bundle.getEnum(ELEMENT + i, Damage.Element::class.java)
                else null

                entries.add(Entry(ratio, dmgType, element, left))
            }
        } else {
            // Discard legacy Vulnerable data to avoid crashes
            entries.clear()
        }
    }

    companion object {
        const val DURATION = 10f

        private const val COUNT = "count"
        private const val RATIO = "ratio_"
        private const val DAMAGE_TYPE = "type_"
        private const val ELEMENT = "elem_"
        private const val LEFT = "left_"

        fun add(target: Char, ratio: Float, dmgType: Damage.Type = Damage.Type.NORMAL, duration: Float): Vulnerable {
            val v = Buff.affect(target, Vulnerable::class.java)
            v.addEntry(ratio, dmgType, duration)
            return v
        }

        fun add(target: Char, ratio: Float, element: Damage.Element, duration: Float): Vulnerable {
            val v = Buff.affect(target, Vulnerable::class.java)
            v.addEntry(ratio, element, duration)
            return v
        }
    }
}

