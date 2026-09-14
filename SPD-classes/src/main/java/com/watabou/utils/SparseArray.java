/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.watabou.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class SparseArray<T> {

	// The backing map is shared between the actor thread (level logic) and the
	// render thread (field-of-view / sprite updates). All access is guarded by
	// this instance's monitor, and reads build a private snapshot in a single
	// traversal so concurrent writes can never trigger a
	// ConcurrentModificationException mid-iteration.
	private final TreeMap<Integer, T> values = new TreeMap<Integer, T>();

	public synchronized T get(int key) { return values.get(key); }
	public synchronized T get(int key, T defaultValue) { return values.containsKey(key) ? values.get(key) : defaultValue; }
	public synchronized void put(int key, T value) { values.put(key, value); }
	public synchronized void append(int key, T value) { values.put(key, value); }
	public synchronized void remove(int key) { values.remove(key); }
	public synchronized void clear() { values.clear(); }
	public synchronized int size() { return values.size(); }
	public synchronized int keyAt(int index) { return new ArrayList<Integer>(values.keySet()).get(index); }
	public synchronized T valueAt(int index) { return new ArrayList<T>(values.values()).get(index); }
	public synchronized int indexOfKey(int key) { return new ArrayList<Integer>(values.keySet()).indexOf(key); }

	public synchronized int[] keyArray() {
		int[] array = new int[values.size()];
		int i = 0;
		for (Integer key : values.keySet()) {
			array[i++] = key;
		}
		return array;
	}

	public synchronized List<T> values() {
		ArrayList<T> list = new ArrayList<T>( values.size() );
		list.addAll( values.values() );
		return list;
	}
}
