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

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.watabou.noosa.Game;

public class AndroidLauncher extends AndroidApplication {
    private AndroidPlatformSupport platformSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Game.version = getPackageVersionName();
        Game.versionCode = getPackageVersionCode();
        platformSupport = new AndroidPlatformSupport(this);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.depth = 0;
        config.r = config.g = config.b = 8;
        config.useCompass = false;
        config.useAccelerometer = false;
        AndroidCrashReporting.install(this);
        initialize(new DarkestPixelDungeon(platformSupport), config);
    }

    @SuppressWarnings("deprecation")
    private String getPackageVersionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception ignored) { return "???"; }
    }

    @SuppressWarnings("deprecation")
    private int getPackageVersionCode() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; }
        catch (Exception ignored) { return 0; }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && platformSupport != null) platformSupport.updateSystemUI();
    }
}
