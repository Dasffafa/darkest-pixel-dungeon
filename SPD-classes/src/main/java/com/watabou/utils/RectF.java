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

public class RectF {
    public float left;
    public float top;
    public float right;
    public float bottom;

    public RectF() { this(0, 0, 0, 0); }
    public RectF(RectF rect) { this(rect.left, rect.top, rect.right, rect.bottom); }
    public RectF(Rect rect) { this(rect.left, rect.top, rect.right, rect.bottom); }
    public RectF(float left, float top, float right, float bottom) { set(left, top, right, bottom); }
    public float width() { return right - left; }
    public float height() { return bottom - top; }
    public float square() { return width() * height(); }
    public RectF set(float left, float top, float right, float bottom) {
        this.left = left; this.top = top; this.right = right; this.bottom = bottom; return this;
    }
    public RectF set(RectF rect) { return set(rect.left, rect.top, rect.right, rect.bottom); }
    public RectF set(Rect rect) { return set(rect.left, rect.top, rect.right, rect.bottom); }
    public RectF setPos(float x, float y) { return set(x, y, x + width(), y + height()); }
    public RectF shift(float x, float y) { return set(left + x, top + y, right + x, bottom + y); }
    public void offset(float x, float y) { shift(x, y); }
    public RectF resize(float width, float height) { return set(left, top, left + width, top + height); }
    public boolean isEmpty() { return right <= left || bottom <= top; }
    public RectF setEmpty() { left = right = top = bottom = 0; return this; }
    public RectF intersect(RectF other) {
        return new RectF(Math.max(left, other.left), Math.max(top, other.top),
                Math.min(right, other.right), Math.min(bottom, other.bottom));
    }
    public RectF union(RectF other) {
        return new RectF(Math.min(left, other.left), Math.min(top, other.top),
                Math.max(right, other.right), Math.max(bottom, other.bottom));
    }
    public RectF union(float x, float y) {
        if (isEmpty()) return set(x, y, x + 1, y + 1);
        if (x < left) left = x; else if (x >= right) right = x + 1;
        if (y < top) top = y; else if (y >= bottom) bottom = y + 1;
        return this;
    }
    public boolean contains(float x, float y) {
        return x >= left && x < right && y >= top && y < bottom;
    }
    public boolean inside(Point point) {
        return point.x >= left && point.x < right && point.y >= top && point.y < bottom;
    }
    public RectF shrink(float amount) {
        return new RectF(left + amount, top + amount, right - amount, bottom - amount);
    }
    public RectF shrink() { return shrink(1); }
}
