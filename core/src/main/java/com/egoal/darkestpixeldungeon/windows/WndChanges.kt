package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.ui.ScrollPane
import com.egoal.darkestpixeldungeon.ui.Window
import com.watabou.noosa.Image
import com.watabou.noosa.ui.Component

class WndChanges(icon: Image, title: String, message: String) : Window() {

    init {
        val wndWidth = if (DarkestPixelDungeon.landscape()) WIDTH_L else WIDTH_P

        val titlebar = IconTitle(icon, title)
        titlebar.setRect(0f, 0f, wndWidth.toFloat(), 0f)
        add(titlebar)

        val text = PixelScene.renderMultiline(6)
        text.text(message, wndWidth - MARGIN * 2)
        text.setPos(MARGIN.toFloat(), MARGIN.toFloat())

        val content = Component()
        content.add(text)
        content.setSize(wndWidth.toFloat(), text.bottom() + MARGIN)

        val viewHeight = minOf(content.height(), PixelScene.uiCamera.height * MAX_HEIGHT_RATIO
                - titlebar.height() - GAP)

        resize(wndWidth, (titlebar.bottom() + GAP + viewHeight).toInt())

        val list = ScrollPane(content)
        add(list)
        list.setRect(0f, titlebar.bottom() + GAP, wndWidth.toFloat(), viewHeight)
    }

    companion object {
        private const val WIDTH_P = 120
        private const val WIDTH_L = 144

        private const val MARGIN = 4
        private const val GAP = 2f

        /** the window never grows past this share of the screen */
        private const val MAX_HEIGHT_RATIO = 0.9f
    }
}
