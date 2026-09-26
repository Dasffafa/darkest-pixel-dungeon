package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.ui.CheckBox

/**
 * Game settings option controlling whether the catalog asks the player to
 * confirm the spoiler warning before it opens. Confirming the warning switches
 * this off, and it can be turned back on here at any time.
 */
class CatalogSpoilerOption : CheckBox(M.L(CatalogSpoilerOption::class.java, "text")) {

    init {
        checked(DarkestPixelDungeon.catalogSpoilerWarning())
    }

    override fun onClick() {
        super.onClick()
        DarkestPixelDungeon.catalogSpoilerWarning(checked())
    }
}
