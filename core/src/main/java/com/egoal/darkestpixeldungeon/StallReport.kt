package com.egoal.darkestpixeldungeon

object StallReport {

    @Volatile
    private var pending: Throwable? = null

    val hasPending: Boolean
        get() = pending != null

    fun record(t: Throwable) {
        pending = t
    }

    fun take(): Throwable? {
        val t = pending
        pending = null
        return t
    }
}
