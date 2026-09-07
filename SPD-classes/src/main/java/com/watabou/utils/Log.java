/*
 * Darkest Pixel Dungeon
 * Copyright (C) 2018-2026 Egoal
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.watabou.utils;

import com.badlogic.gdx.Gdx;

public final class Log {
    private Log() { }
    public static int d(String tag, String message) { Gdx.app.debug(tag, message); return 0; }
    public static int i(String tag, String message) { Gdx.app.log(tag, message); return 0; }
    public static int w(String tag, String message) { Gdx.app.log(tag, message); return 0; }
    public static int e(String tag, String message) { Gdx.app.error(tag, message); return 0; }
    public static int e(String tag, String message, Throwable error) { Gdx.app.error(tag, message, error); return 0; }
}
