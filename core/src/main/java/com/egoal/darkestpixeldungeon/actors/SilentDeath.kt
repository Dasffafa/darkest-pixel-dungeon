package com.egoal.darkestpixeldungeon.actors

interface SilentDeath

object DeathSilence {
    fun isSilent(cause: Any?): Boolean {
        val cls: Class<*> = when (cause) {
            null -> return false
            is Class<*> -> cause
            else -> cause.javaClass
        }
        return SilentDeath::class.java.isAssignableFrom(cls)
    }
}