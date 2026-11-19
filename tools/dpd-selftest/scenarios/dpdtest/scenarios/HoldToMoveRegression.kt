package dpdtest.scenarios

import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.scenes.TitleScene
import dpdtest.H
import dpdtest.Scenario

class HoldToMoveRegression : Scenario {
    override fun describe(): String = "Verify movementHoldSensitivity preference range and persistence"

    override fun run(h: H) {
        h.switchScene(TitleScene::class.java)

        val original = DarkestPixelDungeon.movementHoldSensitivity()
        h.check(original in 0..4, "Default movementHoldSensitivity is within 0..4")

        DarkestPixelDungeon.movementHoldSensitivity(1)
        h.check(DarkestPixelDungeon.movementHoldSensitivity() == 1, "movementHoldSensitivity persists value 1")

        DarkestPixelDungeon.movementHoldSensitivity(4)
        h.check(DarkestPixelDungeon.movementHoldSensitivity() == 4, "movementHoldSensitivity persists value 4")

        DarkestPixelDungeon.movementHoldSensitivity(original)
    }
}
