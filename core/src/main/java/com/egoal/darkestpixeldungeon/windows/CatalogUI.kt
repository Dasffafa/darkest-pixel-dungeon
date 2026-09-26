package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.ui.Icons
import com.egoal.darkestpixeldungeon.ui.Window
import com.watabou.noosa.Game

/**
 * Entry point of the catalog. The catalog is a compendium of everything the
 * hero has met, so it is opened from the title screen and warns about spoilers
 * until the player confirms the warning once.
 */
object CatalogUI {

    fun open() {
        if (!DarkestPixelDungeon.catalogSpoilerWarning()) {
            show(WndCatalogs())
            return
        }

        val wnd = WndOptions.CreateConfirm(Icons.WARNING.get(),
                M.L(CatalogUI::class.java, "spoiler_title"),
                M.L(CatalogUI::class.java, "spoiler_message")) {
            DarkestPixelDungeon.catalogSpoilerWarning(false)
            show(WndCatalogs())
        }

        Game.scene().addToFront(wnd)
    }

    /**
     * Shows a window on top of whatever scene is running: windows opened from
     * the title screen cannot go through GameScene.
     */
    fun show(wnd: Window) {
        val scene = Game.scene()
        if (scene is GameScene) GameScene.show { wnd } else scene.addToFront(wnd)
    }
}
