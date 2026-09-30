package com.egoal.darkestpixeldungeon.update

import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.watabou.noosa.Game

object Updates {

    private const val CHECK_DELAY = 60 * 60 * 1000L

    private var lastCheck = 0L
    private var checking = false

    @Volatile
    var available: UpdateInfo? = null
        private set

    fun checkForUpdate() {
        if (!DarkestPixelDungeon.updateCheck()) return

        val now = System.currentTimeMillis()
        if (checking || now - lastCheck < CHECK_DELAY) return
        checking = true

        UpdateChecker.fetch { info ->
            checking = false
            lastCheck = System.currentTimeMillis()
            if (info != null && info.versionCode > Game.versionCode &&
                    info.versionCode > DarkestPixelDungeon.updateIgnored()) {
                available = info
            }
        }
    }

    fun clear() {
        available = null
    }

    fun ignore() {
        val info = available ?: return
        DarkestPixelDungeon.updateIgnored(info.versionCode)
        available = null
    }
}
