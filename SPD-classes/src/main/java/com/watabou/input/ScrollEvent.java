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
 */
package com.watabou.input;

import com.watabou.utils.PointF;
import com.watabou.utils.Signal;

import java.util.ArrayList;

public class ScrollEvent {

    public final PointF pos;
    public final float amount;

    public ScrollEvent(PointF mousePos, float amount) {
        pos = new PointF(mousePos);
        this.amount = amount;
    }

    private static final Signal<ScrollEvent> scrollSignal = new Signal<>(true);
    private static final ArrayList<ScrollEvent> scrollEvents = new ArrayList<>();

    public static void addScrollListener(Signal.Listener<ScrollEvent> listener) {
        scrollSignal.add(listener);
    }

    public static void removeScrollListener(Signal.Listener<ScrollEvent> listener) {
        scrollSignal.remove(listener);
    }

    public static synchronized void addScrollEvent(ScrollEvent event) {
        scrollEvents.add(event);
    }

    public static synchronized void processScrollEvents() {
        for (ScrollEvent event : scrollEvents) {
            scrollSignal.dispatch(event);
        }
        scrollEvents.clear();
    }
}
