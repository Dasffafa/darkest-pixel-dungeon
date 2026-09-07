package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.levels.Level
import com.egoal.darkestpixeldungeon.scenes.GameScene

/** Context actions for an empty dungeon cell. */
class WndCellActions(private val cell: Int) :
    WndOptions("", "", *actions(cell).map { it.label }.toTypedArray()) {

    override fun onSelect(index: Int) {
        actions(cell).getOrNull(index)?.run?.invoke()
    }
}

private data class CellAction(val label: String, val run: () -> Unit)

private fun actions(cell: Int): List<CellAction> {
    val result = mutableListOf(CellAction("Examine") { GameScene.examineCell(cell) })
    if (cell >= 0 && cell < Dungeon.level.length() && (Level.passable[cell] || Level.avoid[cell])) {
        result += CellAction("Go there") { GameScene.handleCell(cell) }
    }
    return result
}
