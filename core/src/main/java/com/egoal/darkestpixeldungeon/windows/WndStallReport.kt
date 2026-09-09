package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.CrashReporting
import com.egoal.darkestpixeldungeon.StallReport
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.utils.GLog

class WndStallReport : WndOptions(
        M.L(WndStallReport::class.java, "title"),
        M.L(WndStallReport::class.java, "message"),
        M.L(WndStallReport::class.java, "report"),
        M.L(WndStallReport::class.java, "cancel")) {

    override fun onSelect(index: Int) {
        if (index != 0) return

        val stall = StallReport.take() ?: return
        CrashReporting.capture(stall)
        GLog.p(M.L(WndStallReport::class.java, "reported"))
    }
}
