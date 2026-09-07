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
package com.egoal.darkestpixeldungeon.scenes;

import com.egoal.darkestpixeldungeon.DungeonTilemap;
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.egoal.darkestpixeldungeon.actors.Actor;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.windows.WndCharActions;
import com.egoal.darkestpixeldungeon.windows.WndCellActions;
import com.watabou.input.Touchscreen.Touch;
import com.watabou.noosa.TouchArea;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;

public class CellSelector extends TouchArea {

  public Listener listener = null;

  public boolean enabled;

  private float dragThreshold;

  public CellSelector(DungeonTilemap map) {
    super(map);
    camera = map.camera();

    dragThreshold = PixelScene.defaultZoom * DungeonTilemap.SIZE / 2;
  }

  @Override
  public void onClick(Touch touch) {
    if (dragging) {

      dragging = false;

    } else {

      int cell = ((DungeonTilemap) target).screenToTile(
              (int) touch.current.x,
              (int) touch.current.y);
      // Desktop right-click opens the same contextual actions as a long press.
      if (touch.button == 1 && cell != -1) {
        onContextClick(cell);
      } else {
        select(cell);
      }
    }
  }

  private void onContextClick(int cell) {
    Char character = Actor.Companion.findChar(cell);
    if (character != null && character != com.egoal.darkestpixeldungeon.Dungeon.INSTANCE.getHero()) {
      GameScene.show(() -> new WndCharActions(character, cell));
    } else {
      GameScene.show(() -> new WndCellActions(cell));
    }
  }

  private float zoom(float value) {

    value = GameMath.INSTANCE.gate(PixelScene.minZoom, value, PixelScene.maxZoom);
    DarkestPixelDungeon.zoom((int) (value - PixelScene.defaultZoom));
    camera.zoom(value);

    //Resets character sprite positions with the new camera zoom
    //This is important as characters are centered on a 16x16 tile, but may 
    // have any sprite size
    //This can lead to none-whole coordinate, which need to be aligned with 
    // the zoom
    for (Char c : Actor.Companion.chars()) {
      if(c.getHasSprite() && !c.sprite.isMoving())
        c.sprite.point(c.sprite.worldToCamera(c.getPos()));
    }

    return value;
  }

  public void zoomBy(float amount) {
    zoom(Math.round(camera.zoom + amount));
  }

  public void select(int cell) {
    if (enabled && listener != null && cell != -1) {

      Listener selectedListener = listener;
      selectedListener.onSelect(cell);
      // A selection callback may start another targeting action. Do not
      // overwrite the new listener when finishing the previous selection.
      if (listener == selectedListener) {
        GameScene.ready();
      }

    } else {

      GameScene.cancel();

    }
  }

  private boolean pinching = false;
  private Touch another;
  private float startZoom;
  private float startSpan;

  @Override
  protected void onTouchDown(Touch t) {

    if (t != touch && another == null) {

      if (!touch.down) {
        touch = t;
        onTouchDown(t);
        return;
      }

      pinching = true;

      another = t;
      startSpan = PointF.distance(touch.current, another.current);
      startZoom = camera.zoom;

      dragging = false;
    } else if (t != touch) {
      reset();
    }
  }

  @Override
  protected void onTouchUp(Touch t) {
    if (pinching && (t == touch || t == another)) {

      pinching = false;

      zoom(Math.round(camera.zoom));

      dragging = true;
      if (t == touch) {
        touch = another;
      }
      another = null;
      lastPos.set(touch.current);
    }
  }

  private boolean dragging = false;
  private PointF lastPos = new PointF();

  @Override
  protected void onDrag(Touch t) {

    camera.target = null;

    if (pinching) {

      float curSpan = PointF.distance(touch.current, another.current);
      camera.zoom(GameMath.INSTANCE.gate(
              PixelScene.minZoom,
              startZoom * curSpan / startSpan,
              PixelScene.maxZoom));

    } else {

      if (!dragging && PointF.distance(t.current, t.start) > dragThreshold) {

        dragging = true;
        lastPos.set(t.current);

      } else if (dragging) {
        camera.scroll.offset(PointF.diff(lastPos, t.current).invScale(camera
                .zoom));
        lastPos.set(t.current);
      }
    }

  }

  @Override
  protected boolean onLongClick(Touch t) {
    int cell = ((DungeonTilemap) target).screenToTile((int) t.current.x, (int) t.current.y);
    Char character = Actor.Companion.findChar(cell);
    if (character != null && character != com.egoal.darkestpixeldungeon.Dungeon.INSTANCE.getHero()) {
      GameScene.show(() -> new WndCharActions(character, cell));
      return true;
    }
    return false;
  }

  public void cancel() {

    if (listener != null) {
      listener.onSelect(null);
    }

    GameScene.ready();
  }

  @Override
  public void reset() {
    super.reset();
    another = null;
    if (pinching) {
      pinching = false;

      zoom(Math.round(camera.zoom));
    }
  }

  public void enable(boolean value) {
    if (enabled != value) {
      enabled = value;
    }
  }

  public interface Listener {
    void onSelect(Integer cell);

    String prompt();
  }
}
