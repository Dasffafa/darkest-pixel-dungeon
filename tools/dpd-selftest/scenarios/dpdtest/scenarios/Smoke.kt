package dpdtest.scenarios

import com.egoal.darkestpixeldungeon.scenes.TitleScene
import com.watabou.noosa.Game
import dpdtest.H
import dpdtest.Scenario

class Smoke : Scenario {
    override fun describe(): String = "Boot into TitleScene and render cleanly without exceptions"

    override fun run(h: H) {
        h.switchScene(TitleScene::class.java)
        h.frames(20)
        h.check(Game.scene() is TitleScene, "TitleScene is active after boot")
    }
}
