/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2019 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.watabou.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileUtils {

    private FileUtils() {
    }

    public static boolean fileExists(String name) {
        FileHandle file = Gdx.files.local(name);
        return file.exists() && !file.isDirectory();
    }

    public static boolean deleteFile(String name) {
        return Gdx.files.local(name).delete();
    }

    public static Bundle bundleFromFile(String fileName) throws IOException {
        FileHandle file = Gdx.files.local(fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("file not found: " + file.path());
        }
        try (InputStream input = file.read()) {
            return Bundle.read(input);
        }
    }

    public static void bundleToFile(String fileName, Bundle bundle) throws IOException {
        FileHandle file = Gdx.files.local(fileName);
        FileHandle temp = Gdx.files.local(fileName + ".tmp");
        try {
            try (OutputStream output = temp.write(false)) {
                Bundle.write(bundle, output);
            }
            try (InputStream input = temp.read()) {
                Bundle.read(input);
            }
            temp.moveTo(file);
        } catch (GdxRuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        } finally {
            if (temp.exists()) temp.delete();
        }
    }
}
