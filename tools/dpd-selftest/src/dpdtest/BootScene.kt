package dpdtest

import com.egoal.darkestpixeldungeon.scenes.PixelScene

class BootScene : PixelScene() {
    override fun create() {
        super.create()
        HarnessGame.bootReady = true
        Log.info("BootScene ready (game created, hidden window, audio disabled)")
    }
}
