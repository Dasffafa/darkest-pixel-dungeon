package com.watabou.utils;

import com.badlogic.gdx.utils.SharedLibraryLoader;

/** Platform helpers shared by desktop and mobile builds. */
public final class DeviceCompat {

    private DeviceCompat() {
    }

    public static boolean isDesktop() {
        return SharedLibraryLoader.isWindows || SharedLibraryLoader.isMac || SharedLibraryLoader.isLinux;
    }
}
