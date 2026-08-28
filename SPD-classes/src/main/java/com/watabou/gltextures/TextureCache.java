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

package com.watabou.gltextures;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.glwrap.Texture;

public class TextureCache {

	private static HashMap<Object,SmartTexture> all = new HashMap<>();

	public static SmartTexture createSolid( int color ) {
		final String key = "1x1:" + color;
		
		if (all.containsKey( key )) {
			
			return all.get( key );
			
		} else {
		
			Pixmap bmp = new Pixmap( 1, 1, Pixmap.Format.RGBA8888 );
			bmp.setColor( (color << 8) | (color >>> 24) );
			bmp.fill();
			
			SmartTexture tx = new SmartTexture( bmp );
			all.put( key, tx );
			
			return tx;
		}
	}
	
	public static SmartTexture createGradient( int... colors ) {
		
		final String key = "" + colors;
		
		if (all.containsKey( key )) {
			
			return all.get( key );
			
		} else {
		
			Pixmap bmp = new Pixmap( colors.length, 1, Pixmap.Format.RGBA8888 );
			for (int i=0; i < colors.length; i++) {
				bmp.drawPixel( i, 0, (colors[i] << 8) | (colors[i] >>> 24) );
			}
			SmartTexture tx = new SmartTexture( bmp );

			tx.filter( Texture.LINEAR, Texture.LINEAR );
			tx.wrap( Texture.CLAMP, Texture.CLAMP );

			all.put( key, tx );
			return tx;
		}
		
	}
	
	public static void add( Object key, SmartTexture tx ) {
		all.put( key, tx );
	}

	public static void remove( Object key ) {
		SmartTexture tx = all.remove(key);
		if (tx != null) tx.delete();
	}

	public static SmartTexture get( Object src ) {
		
		if (all.containsKey( src )) {
			
			return all.get( src );
			
		} else if (src instanceof SmartTexture) {
			
			return (SmartTexture)src;
			
		} else {

			Pixmap bitmap = getBitmap(src);
			if (bitmap == null) {
				throw new IllegalArgumentException("Unable to load texture: " + src);
			}
			SmartTexture tx = new SmartTexture(bitmap);
			all.put( src, tx );
			return tx;
		}
		
	}
	
	public static void clear() {
		
		for (Texture txt:all.values()) {
			txt.delete();
		}
		all.clear();
		
	}
	
	public static void reload() {
		for (SmartTexture tx:all.values()) {
			tx.reload();
		}
	}
	
	public static Pixmap getBitmap( Object src ) {
		
		try {
			if (src instanceof Integer){
				return null;
				
			} else if (src instanceof String) {
				
				return new Pixmap(Gdx.files.internal((String)src));
				
			} else if (src instanceof Pixmap) {
				
				return (Pixmap)src;
				
			} else {
				
				return null;
				
			}
		} catch (Exception e) {
			Gdx.app.error("TextureCache", "Unable to load texture: " + src, e);
			return null;
			
		}
	}
	
	public static boolean contains( Object key ) {
		return all.containsKey( key );
	}
	
}
