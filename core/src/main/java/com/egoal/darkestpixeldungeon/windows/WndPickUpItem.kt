package com.egoal.darkestpixeldungeon.windows

import com.badlogic.gdx.Input
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.items.Heap
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.ui.RedButton
import com.egoal.darkestpixeldungeon.ui.Window
import com.egoal.darkestpixeldungeon.utils.GLog
import com.watabou.input.Keys

class WndPickUpItem(private val heap: Heap, private val hero: Hero) : Window() {
    private val buttons = ArrayList<RedButton>()
    private var selected = 0

    init { rebuild() }

    private fun rebuild() {
        if (!alive || !exists || members == null) return
        // Group.remove() only detaches a button; it does not destroy the button's
        // TouchArea, so explicitly destroy every button that is being rebuilt.
        buttons.forEach {
            remove(it)
            it.destroy()
        }
        buttons.clear()
        if (heap.empty()) { hide(); return }

        var y = MARGIN
        heap.items.toList().forEach { item ->
            val button = object : RedButton(item.toString()) {
                override fun onClick() { pickUp(item) }
            }
            button.setRect(MARGIN, y, WIDTH - MARGIN * 2, BUTTON_HEIGHT)
            buttons += button
            add(button)
            y += BUTTON_HEIGHT + MARGIN
        }
        selected = 0
        updateSelection()
        resize(WIDTH.toInt(), y.toInt())
    }

    private fun pickUp(item: Item) {
        // A teleport or scene transition can destroy this window while the click
        // callback is still unwinding. Do not mutate a destroyed window or pick
        // up from a heap the hero has already left behind.
        if (!alive || !exists || parent == null || members == null || hero.pos != heap.pos ||
            Dungeon.level.heaps.get(heap.pos) !== heap) return
        if (!heap.items.contains(item) || !item.doPickUp(hero)) {
            hero.ready()
            return
        }
        heap.pickUp(item)
        GLog.i(Messages.get(hero, "you_now_have", item.name()))
        hero.ready()
        if (heap.empty()) {
            hide()
            return
        }
        rebuild()
    }

    override fun onBackPressed() {
        // Closing the picker cancels the pending pickup action and lets the hero act again.
        super.onBackPressed()
        hero.ready()
    }

    private fun updateSelection() {
        buttons.forEachIndexed { index, button ->
            button.textColor(if (index == selected) TITLE_COLOR else 0xFFFFFF)
        }
    }

    override fun onSignal(key: Keys.Key): Boolean {
        if (key.pressed && buttons.isNotEmpty()) {
            when (key.code) {
                Input.Keys.UP -> selected = (selected - 1 + buttons.size) % buttons.size
                Input.Keys.DOWN -> selected = (selected + 1) % buttons.size
                Input.Keys.ENTER -> { pickUp(heap.items.getOrNull(selected) ?: return true); return true }
                else -> return super.onSignal(key)
            }
            updateSelection()
            return true
        }
        return super.onSignal(key)
    }

    companion object {
        private const val WIDTH = 120f
        private const val MARGIN = 2f
        private const val BUTTON_HEIGHT = 20f
    }
}
