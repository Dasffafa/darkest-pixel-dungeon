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

import com.badlogic.gdx.Gdx;
import com.watabou.utils.PlatformSupport;

import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class DesktopPlatformSupport extends PlatformSupport {
    @Override
    public void promptTextInput(String title, String defaultText, final TextCallback callback) {
        SwingUtilities.invokeLater(() -> {
            JTextField field = new JTextField(defaultText == null ? "" : defaultText, 20);
            int result = JOptionPane.showConfirmDialog(null, field, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            callback.onSelect(result == JOptionPane.OK_OPTION, field.getText());
        });
    }
    @Override
    public Locale getSystemLocale() {
        Locale locale = localeFromEnvironment(System.getenv("LANGUAGE"));
        if (locale == null) locale = localeFromEnvironment(System.getenv("LANG"));
        return locale != null ? locale : super.getSystemLocale();
    }

    private Locale localeFromEnvironment(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        String tag = value.split(":", 2)[0];
        tag = tag.split("[.@]", 2)[0];
        if (tag.equalsIgnoreCase("C") || tag.equalsIgnoreCase("POSIX")) return null;

        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? null : locale;
    }

    @Override
    public void reportException(Throwable throwable) {
        Gdx.app.error("dpd", "Unhandled exception", throwable);
    }
}
