package com.egoal.darkestpixeldungeon.ui.changelist

import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.ui.RenderedTextMultiline
import com.watabou.noosa.ColorBlock
import com.watabou.noosa.RenderedText
import com.watabou.noosa.ui.Component
import java.util.ArrayList

/**
 * A section of the icon changelist: a heading, an optional paragraph, and the
 * row of [ChangeButton]s that belong to it. The layout follows Shattered Pixel
 * Dungeon's own changelist, where buttons wrap and centre themselves in the row.
 */
class ChangeInfo(titleText: String, private val major: Boolean, bodyText: String?) : Component() {

    private val title: RenderedText = PixelScene.renderText(titleText, if (major) 9 else 6)
    private val line: ColorBlock
    private val body: RenderedTextMultiline?
    private val buttons = ArrayList<ChangeButton>()

    init {
        add(title)

        line = ColorBlock(1f, 1f, if (major) MAJOR_LINE else MINOR_LINE)
        add(line)

        body = if (bodyText.isNullOrEmpty()) null else PixelScene.renderMultiline(bodyText, 6).also { add(it) }
    }

    fun addButton(button: ChangeButton) {
        buttons.add(button)
        add(button)

        button.setSize(ChangeButton.SIZE, ChangeButton.SIZE)
        layout()
    }

    fun onClick(x: Float, y: Float): Boolean {
        for (button in buttons) {
            if (button.onClick(x, y)) return true
        }
        return false
    }

    override fun layout() {
        super.layout()

        var posY = y + 3f
        if (major) posY += 2f

        title.x = x + (width - title.width()) / 2f
        title.y = posY
        PixelScene.align(title)
        posY += title.height() + 2f

        body?.let {
            it.maxWidth(width().toInt())
            it.setPos(x, posY)
            posY += it.height() + 2f
        }

        var posX = x
        var tallest = 0f
        for (button in buttons) {

            if (posX + button.width() >= right()) {
                posX = x
                posY += tallest
                tallest = 0f
            }

            // a row of icons shorter than the section is centred as a whole
            if (posX == x) {
                var offset = width
                for (other in buttons) {
                    offset -= other.width()
                    if (offset <= 0f) {
                        offset += other.width()
                        break
                    }
                }
                posX += offset / 2f
            }

            button.setPos(posX, posY)
            posX += button.width()
            if (tallest < button.height()) tallest = button.height()
        }
        posY += tallest + 2f

        height = posY - y

        if (major) {
            line.size(width(), 1f)
            line.x = x
            line.y = y + 2f
        } else {
            line.size(1f, height())
            line.x = x
            line.y = y
        }
    }

    companion object {
        private const val MAJOR_LINE = 0xFF222222.toInt()
        private const val MINOR_LINE = 0xFF333333.toInt()
    }
}
