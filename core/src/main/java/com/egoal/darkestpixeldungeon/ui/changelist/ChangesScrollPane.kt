package com.egoal.darkestpixeldungeon.ui.changelist

import com.egoal.darkestpixeldungeon.ui.ScrollPane
import com.watabou.noosa.ui.Component
import java.util.ArrayList

/**
 * The changelist's scroll pane. A tap that is not a drag is handed to the
 * sections, whose icon buttons open their own windows.
 */
class ChangesScrollPane(content: Component) : ScrollPane(content) {

    private val infos = ArrayList<ChangeInfo>()

    fun addInfo(info: ChangeInfo) {
        infos.add(info)
    }

    override fun onClick(x: Float, y: Float) {
        for (info in infos) {
            if (info.onClick(x, y)) return
        }
    }
}
