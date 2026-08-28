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
package com.egoal.darkestpixeldungeon.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.watabou.noosa.Game;

public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        loadVersion();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Darkest Pixel Dungeon");
        config.setWindowedMode(1280, 720);
        config.setWindowSizeLimits(720, 400, -1, -1);
        config.useVsync(true);

        new Lwjgl3Application(new DarkestPixelDungeon(new DesktopPlatformSupport()), config);
    }

    private static void loadVersion() {
        Package launcherPackage = DesktopLauncher.class.getPackage();
        Game.version = launcherPackage.getSpecificationVersion();
        if (Game.version == null) {
            Game.version = System.getProperty("Specification-Version");
        }

        String versionCode = launcherPackage.getImplementationVersion();
        if (versionCode == null) {
            versionCode = System.getProperty("Implementation-Version");
        }

        if (Game.version == null || versionCode == null) {
            throw new IllegalStateException(
                    "Desktop version metadata is missing; launch through Gradle or a packaged JAR");
        }

        try {
            Game.versionCode = Integer.parseInt(versionCode);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid desktop version code: " + versionCode, e);
        }
    }
}
