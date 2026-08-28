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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class PlatformSupport {
    private static final String CJK_FONT_ASSET = "droid_sans.ttf";
    private static final Pattern CJK_TEXT = Pattern.compile(
            "[\\p{IsHan}\\p{InHiragana}\\p{InKatakana}\\p{InHangul_Syllables}"
                    + "\\p{InCJK_Symbols_and_Punctuation}"
                    + "\\p{InHalfwidth_and_Fullwidth_Forms}]");

    private String fontAsset = "font.ttf";
    private FreeTypeFontGenerator fontGenerator;
    private FreeTypeFontGenerator cjkFontGenerator;
    private PixmapPacker fontPacker;
    private final Map<Integer, BitmapFont> fonts = new HashMap<Integer, BitmapFont>();
    private final Map<Integer, BitmapFont> cjkFonts = new HashMap<Integer, BitmapFont>();
    public void updateSystemUI() { }
    public boolean supportsSystemUI() { return false; }
    public void setLandscape(boolean landscape) { }
    public void reportException(Throwable throwable) { throwable.printStackTrace(); }
    public Locale getSystemLocale() { return Locale.getDefault(); }
    public boolean openURI(String uri) { return Gdx.net.openURI(uri); }
    public void promptTextInput(String title, String defaultText, final TextCallback callback) {
        Gdx.input.getTextInput(new Input.TextInputListener() {
            @Override public void input(String text) { callback.onSelect(true, text); }
            @Override public void canceled() { callback.onSelect(false, defaultText); }
        }, title, defaultText, "");
    }

    public interface TextCallback {
        void onSelect(boolean positive, String text);
    }

    public synchronized void setFont(String asset) {
        if (!asset.equals(fontAsset)) {
            fontAsset = asset;
            resetFonts();
        }
    }

    public synchronized BitmapFont getFont(int size, String text) {
        if (fontGenerator == null) {
            fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal(fontAsset));
            cjkFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal(CJK_FONT_ASSET));
            fontPacker = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 1, false);
        }
        boolean useCjkFont = CJK_TEXT.matcher(text).find();
        FreeTypeFontGenerator generator = useCjkFont ? cjkFontGenerator : fontGenerator;
        Map<Integer, BitmapFont> fontCache = useCjkFont ? cjkFonts : fonts;
        BitmapFont font = fontCache.get(size);
        if (font == null) {
            FreeTypeFontGenerator.FreeTypeFontParameter parameters =
                    new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameters.size = size;
            parameters.flip = true;
            parameters.incremental = true;
            parameters.characters = "";
            parameters.packer = fontPacker;
            parameters.hinting = FreeTypeFontGenerator.Hinting.None;
            parameters.borderWidth = parameters.size / 10f;
            parameters.renderCount = 3;
            parameters.spaceX = -(int) parameters.borderWidth;
            font = generator.generateFont(parameters);
            fontCache.put(size, font);
        }
        return font;
    }

    public synchronized void resetFonts() {
        for (BitmapFont font : fonts.values()) font.dispose();
        for (BitmapFont font : cjkFonts.values()) font.dispose();
        fonts.clear();
        cjkFonts.clear();
        if (fontGenerator != null) fontGenerator.dispose();
        if (cjkFontGenerator != null) cjkFontGenerator.dispose();
        if (fontPacker != null) fontPacker.dispose();
        fontGenerator = null;
        cjkFontGenerator = null;
        fontPacker = null;
    }
}
