/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.egoal.darkestpixeldungeon

import com.badlogic.gdx.Gdx
import com.watabou.noosa.Game
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.protocol.SentryId

typealias SentryOptionsConfigurer = (SentryOptions) -> Unit

object CrashReporting {

    private const val GRANTED = "granted"
    private const val DENIED = "denied"

    @Volatile
    private var initialized = false

    @Volatile
    var lastEventId: SentryId? = null
        private set

    var platformInit: ((SentryOptionsConfigurer) -> Unit)? = null

    var platformFeedback: ((SentryId?) -> Unit)? = null

    val available: Boolean
        get() = SentryConfig.DSN.isNotBlank()

    private val decision: String?
        get() = Preferences.INSTANCE.getString(Preferences.KEY_CRASH_REPORT, null)

    val isConsented: Boolean
        get() = decision == GRANTED

    val isDecided: Boolean
        get() = decision != null

    val shouldPrompt: Boolean
        get() = available && !isDecided

    val feedbackAvailable: Boolean
        get() = initialized && platformFeedback != null

    fun setConsent(granted: Boolean) {
        Preferences.INSTANCE.put(Preferences.KEY_CRASH_REPORT, if (granted) GRANTED else DENIED)
        if (granted) init()
    }

    fun initIfConsented() {
        if (available && isConsented && !initialized) init()
    }

    fun showFeedback(eventId: SentryId?) {
        try {
            platformFeedback?.invoke(eventId)
        } catch (e: Throwable) {
            Gdx.app?.error("dpd", "Sentry feedback failed", e)
        }
    }

    fun capture(t: Throwable) {
        if (!initialized) return
        try {
            Sentry.captureException(t)
        } catch (e: Throwable) {
            Gdx.app?.error("dpd", "Sentry capture failed", e)
        }
    }

    private fun applyOptions(options: SentryOptions) {
        options.dsn = SentryConfig.DSN
        val version = Game.version
        options.release = if (version.isNullOrEmpty())
            "com.egoal.darkestpixeldungeon"
        else
            "com.egoal.darkestpixeldungeon@" + version
        options.dist = Game.versionCode.toString()
        options.environment = "release"
        options.tracesSampleRate = 0.0
        options.profilesSampleRate = 0.0
        options.isSendDefaultPii = false
        options.isEnableUncaughtExceptionHandler = true
        options.isAttachThreads = true
        options.isEnableAutoSessionTracking = true
        options.isEnableUserInteractionBreadcrumbs = true
        options.isDebug = DarkestPixelDungeon.debug()
        options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
            lastEventId = event.eventId
            event
        }
    }

    private fun init() {
        if (initialized) return
        try {
            val hook = platformInit
            if (hook != null) hook { applyOptions(it) } else Sentry.init { applyOptions(it) }
            initialized = true
        } catch (e: Throwable) {
            Gdx.app?.error("dpd", "Sentry init failed", e)
        }
    }
}
