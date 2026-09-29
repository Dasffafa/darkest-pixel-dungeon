package com.egoal.darkestpixeldungeon.ui.changelist

import com.egoal.darkestpixeldungeon.scenes.ChangesScene
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.watabou.noosa.Image
import com.watabou.noosa.ui.Component

/**
 * One entry of the icon changelist: an icon that opens its own window with the
 * full text of the change. Not a [com.watabou.noosa.ui.Button] on purpose, as a
 * button would swallow the drag that scrolls the list.
 */
class ChangeButton(private val icon: Image, private val title: String, private val text: String) : Component() {

    init {
        add(icon)
        setSize(SIZE, SIZE)
    }

    fun onClick(x: Float, y: Float): Boolean {
        if (inside(x, y)) {
            ChangesScene.showChangeInfo(icon, title, text)
            return true
        }
        return false
    }

    override fun layout() {
        super.layout()

        icon.x = x + (width - icon.width()) / 2f
        icon.y = y + (height - icon.height()) / 2f
        PixelScene.align(icon)
    }

    companion object {
        const val SIZE = 16f
    }
}
