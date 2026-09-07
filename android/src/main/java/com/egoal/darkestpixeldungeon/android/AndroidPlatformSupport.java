/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Darkest Pixel Dungeon
 * Copyright (C) 2018-2026 Egoal
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.egoal.darkestpixeldungeon.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.watabou.utils.PlatformSupport;

public class AndroidPlatformSupport extends PlatformSupport {
    private final AndroidLauncher launcher;
    AndroidPlatformSupport(AndroidLauncher launcher) { this.launcher = launcher; }

    @Override
    public void updateSystemUI() {
        launcher.runOnUiThread(() -> launcher.getWindow().getDecorView().setSystemUiVisibility(
                DarkestPixelDungeon.immersed()
                        ? View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : 0));
    }

    @Override public boolean supportsSystemUI() { return true; }

    @Override
    public void setLandscape(boolean landscape) {
        launcher.setRequestedOrientation(landscape
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @Override
    public void reportException(Throwable throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable);
    }

    @Override
    public void copyToClipboard(String text) {
        launcher.runOnUiThread(() -> {
            ClipboardManager cm =
                    (ClipboardManager) launcher.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Error Report", text));
            }
        });
    }
}
