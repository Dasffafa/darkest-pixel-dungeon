package com.egoal.darkestpixeldungeon.windows

import com.watabou.noosa.Game
import com.watabou.utils.PlatformSupport

//todo: this is not a good design, just for very rare & lite use.
object InputDialog {
    fun GetStringWithResult(title: String, defval: String, onResult: (Boolean, String) -> Unit) {
        Game.platform.promptTextInput(title, defval,
                PlatformSupport.TextCallback { accepted, text ->
                    onResult(accepted, text)
                })
    }

    fun GetString(title: String, defval: String, onInputed: (String) -> Unit) {
        GetStringWithResult(title, defval) { accepted, text ->
            if (accepted) onInputed(text)
        }
    }
}
