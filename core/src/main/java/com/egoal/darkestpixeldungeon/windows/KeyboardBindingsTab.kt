package com.egoal.darkestpixeldungeon.windows

import com.badlogic.gdx.Input
import com.egoal.darkestpixeldungeon.input.DPDAction
import com.egoal.darkestpixeldungeon.input.DPDKeyBindings
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.ui.RedButton
import com.egoal.darkestpixeldungeon.ui.RenderedTextMultiline
import com.egoal.darkestpixeldungeon.ui.ScrollPane
import com.egoal.darkestpixeldungeon.ui.Window
import com.watabou.input.Keys
import com.watabou.noosa.ColorBlock
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.RenderedText
import com.watabou.noosa.ui.Component

class KeyboardBindingsTab(
    private val width: Int,
    private val height: Int
) : Group() {

    private val rows = ArrayList<BindingRow>()
    private val scrollPane: ScrollPane
    private val listTop: Float
    private val listHeight: Float

    init {
        val headerHeight = 12f
        val resetHeight = 16f
        listTop = headerHeight + 1f
        listHeight = height - listTop - resetHeight - 2f

        val colKey1X = width - 54f
        val colKey2X = width - 27f

        val ttlAction = PixelScene.renderText(Messages.get(this, "action"), 7)
        ttlAction.x = 2f
        ttlAction.y = (headerHeight - ttlAction.baseLine()) / 2f
        PixelScene.align(ttlAction)
        add(ttlAction)

        val ttlKey1 = PixelScene.renderText(Messages.get(this, "key1"), 6)
        ttlKey1.x = colKey1X + (26f - ttlKey1.width()) / 2f
        ttlKey1.y = (headerHeight - ttlKey1.baseLine()) / 2f
        PixelScene.align(ttlKey1)
        add(ttlKey1)

        val ttlKey2 = PixelScene.renderText(Messages.get(this, "key2"), 6)
        ttlKey2.x = colKey2X + (26f - ttlKey2.width()) / 2f
        ttlKey2.y = (headerHeight - ttlKey2.baseLine()) / 2f
        PixelScene.align(ttlKey2)
        add(ttlKey2)

        val sep = ColorBlock(width.toFloat(), 1f, 0xff222222.toInt())
        sep.x = 0f
        sep.y = headerHeight
        add(sep)

        val content = Component()
        var y = 0f
        for (action in DPDAction.values()) {
            val row = BindingRow(action, width.toFloat()) { refreshAll() }
            row.setRect(0f, y, width.toFloat(), BindingRow.HEIGHT)
            content.add(row)
            rows.add(row)
            y += BindingRow.HEIGHT
        }
        content.setSize(width.toFloat(), y)

        scrollPane = object : ScrollPane(content) {
            override fun onClick(x: Float, y: Float) {
                for (row in rows) {
                    if (row.onClick(x, y)) break
                }
            }
        }
        add(scrollPane)

        val btnDefaults = object : RedButton(Messages.get(this, "defaults"), 7) {
            override fun onClick() {
                DPDKeyBindings.resetToDefaults()
                refreshAll()
            }
        }
        btnDefaults.setRect(0f, height - resetHeight, width.toFloat(), resetHeight)
        add(btnDefaults)

        refreshAll()
    }

    fun layoutTab() {
        scrollPane.setRect(0f, listTop, width.toFloat(), listHeight)
    }

    private fun refreshAll() {
        for (row in rows) {
            row.refresh()
        }
    }

    private class BindingRow(
        private val action: DPDAction,
        private val rowWidth: Float,
        private val onChanged: () -> Unit
    ) : Component() {

        private lateinit var label: RenderedText
        private lateinit var btnKey1: RedButton
        private lateinit var btnKey2: RedButton
        private lateinit var divider: ColorBlock

        override fun createChildren() {
            super.createChildren()
            label = PixelScene.renderText(6)
            add(label)

            btnKey1 = RedButton("-", 6)
            add(btnKey1)

            btnKey2 = RedButton("-", 6)
            add(btnKey2)

            divider = ColorBlock(1f, 1f, 0xff222222.toInt())
            add(divider)
        }

        init {
            label.text(Messages.get(KeyboardBindingsTab::class.java, "act_${action.name.lowercase()}"))
        }

        override fun layout() {
            super.layout()
            val keyW = 26f
            val keyH = HEIGHT - 3f
            val key2X = x + width - keyW - 1f
            val key1X = key2X - keyW - 1f

            label.x = x + 2f
            label.y = y + (height - label.baseLine()) / 2f
            PixelScene.align(label)

            btnKey1.setRect(key1X, y + 1f, keyW, keyH)
            btnKey2.setRect(key2X, y + 1f, keyW, keyH)

            divider.size(rowWidth, 1f)
            divider.x = x
            divider.y = y + height - 1f
        }

        fun refresh() {
            val keys = DPDKeyBindings.keysForAction(action)
            btnKey1.text(DPDKeyBindings.keyName(keys.getOrNull(0) ?: 0))
            btnKey2.text(DPDKeyBindings.keyName(keys.getOrNull(1) ?: 0))
        }

        fun onClick(px: Float, py: Float): Boolean {
            if (py < y || py >= y + height) return false
            val slot = when {
                px >= btnKey1.left() && px <= btnKey1.right() -> 0
                px >= btnKey2.left() && px <= btnKey2.right() -> 1
                else -> return false
            }
            val actionName = Messages.get(KeyboardBindingsTab::class.java, "act_${action.name.lowercase()}")
            Game.scene()?.add(WndKeyListen(actionName) { keyCode ->
                DPDKeyBindings.bindKey(action, slot, keyCode)
                onChanged()
            })
            return true
        }

        companion object {
            const val HEIGHT = 15f
        }
    }

    private class WndKeyListen(
        actionName: String,
        private val onKeySelected: (Int) -> Unit
    ) : Window() {

        init {
            val w = 108
            val prompt = PixelScene.renderMultiline(
                Messages.get(KeyboardBindingsTab::class.java, "listen_prompt", actionName),
                7
            ) as RenderedTextMultiline
            prompt.maxWidth(w - 8)
            prompt.setPos(4f, 4f)
            add(prompt)

            val hint = PixelScene.renderMultiline(
                Messages.get(KeyboardBindingsTab::class.java, "listen_hint"),
                6
            ) as RenderedTextMultiline
            hint.maxWidth(w - 8)
            hint.hardlight(0xAAAAAA)
            hint.setPos(4f, prompt.bottom() + 6f)
            add(hint)

            resize(w, (hint.bottom() + 6f).toInt())
        }

        override fun onSignal(key: Keys.Key): Boolean {
            if (key.pressed) {
                when (key.code) {
                    Input.Keys.ESCAPE, Keys.BACK -> hide()
                    Input.Keys.BACKSPACE, Input.Keys.FORWARD_DEL -> {
                        onKeySelected(0)
                        hide()
                    }
                    else -> if (key.code > 0) {
                        onKeySelected(key.code)
                        hide()
                    }
                }
            }
            return true
        }
    }
}
