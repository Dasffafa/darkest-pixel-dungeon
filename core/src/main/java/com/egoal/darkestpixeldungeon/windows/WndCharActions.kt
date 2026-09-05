package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.mobs.Mob
import com.egoal.darkestpixeldungeon.scenes.GameScene

/** Context actions for a monster or NPC selected from the dungeon view. */
class WndCharActions(private val target: Char, private val cell: Int) :
    WndOptions(target.name, "", *charActions(target, cell).map { it.label }.toTypedArray()) {

    override fun onSelect(index: Int) {
        charActions(target, cell).getOrNull(index)?.run?.invoke()
    }
}

private data class CharAction(val label: String, val run: () -> Unit)

private fun charActions(target: Char, cell: Int): List<CharAction> = listOf(
    CharAction("Attack") { GameScene.handleCell(cell) },
    CharAction("Examine") { if (target is Mob) GameScene.show(WndInfoMob(target)) }
)
