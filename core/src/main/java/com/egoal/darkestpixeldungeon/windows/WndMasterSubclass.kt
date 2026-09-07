package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.egoal.darkestpixeldungeon.actors.hero.HeroSubClass
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.ui.RedButton
import com.egoal.darkestpixeldungeon.ui.Window
import com.watabou.input.Keys
import com.badlogic.gdx.Input

class WndMasterSubclass(heroClass: HeroClass) : Window() {
    private val buttons = ArrayList<RedButton>()
    private var selected = 0
    init {
        val rtm = PixelScene.renderMultiline(6)

        var message = M.L(this, "message")
        for (sc in heroClass.subClasses) message += "\n\n" + sc.desc()
        rtm.text(message, WIDTH.toInt())
        rtm.setPos(0f, 0f)
        add(rtm)

        var y = rtm.bottom() + GAP
        for (sc in heroClass.subClasses) {
            val btn = object : RedButton(sc.title().toUpperCase()) {
                override fun onClick() {
                    hide()
                    HeroSubClass.Choose(Dungeon.hero, sc)
                }
            }
            btn.setRect(0f, y, WIDTH, BTN_HEIGHT)
            add(btn)
            buttons += btn
            if (buttons.size == 1) btn.textColor(TITLE_COLOR)
            y = btn.bottom() + GAP
        }

        val btnCancel = object : RedButton(M.L(this, "cancel")) {
            override fun onClick() {
                hide()
                GameScene.show { WndMessage(M.L(WndMasterSubclass::class.java, "tip")) }
            }
        }
        btnCancel.setRect(0f, y, WIDTH, BTN_HEIGHT)
        add(btnCancel)
        buttons += btnCancel

        resize(WIDTH.toInt(), btnCancel.bottom().toInt())
    }

    override fun onSignal(key: Keys.Key): Boolean {
        if (key.pressed && buttons.isNotEmpty()) {
            when (key.code) {
                Input.Keys.UP -> selected = (selected - 1 + buttons.size) % buttons.size
                Input.Keys.DOWN -> selected = (selected + 1) % buttons.size
                Input.Keys.ENTER -> { buttons[selected].onClick(); return true }
                else -> return super.onSignal(key)
            }
            buttons.forEachIndexed { i, b -> b.textColor(if (i == selected) TITLE_COLOR else 0xFFFFFF) }
            return true
        }
        return super.onSignal(key)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        GameScene.show { WndMessage(M.L(WndMasterSubclass::class.java, "tip")) }
    }

    companion object {
        private const val WIDTH = 120f
        private const val BTN_HEIGHT = 18f
        private const val GAP = 2f

        fun Show(hero: Hero) {
            GameScene.show { WndMasterSubclass(hero.heroClass) }
        }
    }
}
