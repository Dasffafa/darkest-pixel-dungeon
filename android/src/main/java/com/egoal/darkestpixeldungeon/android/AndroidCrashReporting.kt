/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * Darkest Pixel Dungeon
 * Copyright (C) 2018-2026 Egoal
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
package com.egoal.darkestpixeldungeon.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.egoal.darkestpixeldungeon.CrashReporting
import io.sentry.Sentry
import io.sentry.SentryFeedbackOptions
import io.sentry.android.core.SentryAndroid

object AndroidCrashReporting {

    @JvmStatic
    fun install(context: Context) {
        CrashReporting.platformInit = { configurer ->
            SentryAndroid.init(context) { options -> configurer(options) }
        }
        CrashReporting.platformFeedback = { eventId ->
            Handler(Looper.getMainLooper()).post {
                val configurer = SentryFeedbackOptions.OptionsConfigurator { }
                if (eventId != null)
                    Sentry.showUserFeedbackDialog(eventId, configurer)
                else
                    Sentry.showUserFeedbackDialog(configurer)
            }
        }
    }
}
