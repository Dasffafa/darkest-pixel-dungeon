package com.egoal.darkestpixeldungeon.windows

import com.watabou.noosa.Game
import com.watabou.utils.PlatformSupport

//todo: this is not a good design, just for very rare & lite use.
object InputDialog {
    fun GetString(title: String, defval: String, onInputed: (String) -> Unit) {
        Game.platform.promptTextInput(title, defval,
                PlatformSupport.TextCallback { accepted, text ->
                    if (accepted) onInputed(text)
                })
    }
}
