package dpdtest.scenarios

import com.badlogic.gdx.Input
import com.egoal.darkestpixeldungeon.input.DPDAction
import com.egoal.darkestpixeldungeon.input.DPDKeyBindings
import com.egoal.darkestpixeldungeon.scenes.TitleScene
import com.egoal.darkestpixeldungeon.ui.ScrollPane
import com.egoal.darkestpixeldungeon.windows.KeyboardBindingsTab
import com.egoal.darkestpixeldungeon.windows.WndSettings
import com.watabou.noosa.Game
import dpdtest.H
import dpdtest.Scenario

class SettingsKeyboardTabRegression : Scenario {
    override fun describe(): String = "Open WndSettings on desktop, select KeyboardBindingsTab, and verify key rebinding"

    override fun run(h: H) {
        h.switchScene(TitleScene::class.java)

        val wnd = h.onRender {
            val w = WndSettings(true)
            Game.scene().add(w)
            w
        }
        h.frames(5)

        val kbTab = h.onRender {
            wnd.members.filterIsInstance<KeyboardBindingsTab>().firstOrNull()
        }
        h.check(kbTab != null, "KeyboardBindingsTab is present in WndSettings on desktop")

        val scrollCamNotNull = h.onRender {
            val pane = kbTab!!.members.filterIsInstance<ScrollPane>().firstOrNull()
            pane != null && pane.camera() != null
        }
        h.check(scrollCamNotNull, "KeyboardBindingsTab ScrollPane has non-null camera after layout")

        h.onRender {
            val tabsField = wnd.javaClass.superclass.getDeclaredField("tabs")
            tabsField.isAccessible = true
            val tabs = tabsField.get(wnd) as List<*>
            val selectMethod = wnd.javaClass.superclass.getDeclaredMethod("select", Int::class.javaPrimitiveType)
            selectMethod.isAccessible = true
            selectMethod.invoke(wnd, tabs.size - 1)
        }
        h.frames(10)
        h.check(kbTab!!.visible && kbTab.active, "KeyboardBindingsTab becomes visible and active when selected")

        DPDKeyBindings.bindKey(DPDAction.INVENTORY, 0, Input.Keys.K)
        h.check(DPDKeyBindings.actionForKey(Input.Keys.K) == DPDAction.INVENTORY, "Binding K to INVENTORY succeeds")

        DPDKeyBindings.bindKey(DPDAction.EXAMINE, 0, Input.Keys.K)
        h.check(
            DPDKeyBindings.actionForKey(Input.Keys.K) == DPDAction.EXAMINE &&
                !DPDKeyBindings.keysForAction(DPDAction.INVENTORY).contains(Input.Keys.K),
            "Binding K to EXAMINE overwrites previous INVENTORY binding"
        )

        DPDKeyBindings.bindKey(DPDAction.EXAMINE, 0, 0)
        h.check(DPDKeyBindings.actionForKey(Input.Keys.K) == null, "Clearing slot 0 unbinds K")

        DPDKeyBindings.resetToDefaults()
        h.check(DPDKeyBindings.actionForKey(Input.Keys.I) == DPDAction.INVENTORY, "Reset defaults restores I -> INVENTORY")

        h.onRender { wnd.hide() }
        h.frames(5)
    }
}
