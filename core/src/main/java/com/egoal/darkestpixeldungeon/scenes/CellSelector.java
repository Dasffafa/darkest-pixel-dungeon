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

import com.badlogic.gdx.Gdx;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.DungeonTilemap;
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon;
import com.egoal.darkestpixeldungeon.actors.Actor;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.hero.Hero;
import com.egoal.darkestpixeldungeon.input.DPDAction;
import com.egoal.darkestpixeldungeon.input.DPDKeyBindings;
import com.egoal.darkestpixeldungeon.levels.traps.Trap;
import com.egoal.darkestpixeldungeon.windows.WndCharActions;
import com.egoal.darkestpixeldungeon.windows.WndCellActions;
import com.watabou.input.Touchscreen.Touch;
import com.watabou.noosa.Game;
import com.watabou.noosa.TouchArea;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.HashSet;

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

  // Used for held-key movement (ported from Shattered / Radish Pixel Dungeon)
  private DPDAction heldAction1 = null;
  private DPDAction heldAction2 = null;
  // Not used for movement, but helpful if the player holds 3 keys briefly
  private DPDAction heldAction3 = null;
  private final HashSet<Integer> pressedMoveKeys = new HashSet<>();

  private float heldDelay = 0f;
  private boolean delayingForRelease = false;
  private boolean steppedInHold = false;
  // Prevents repeated inputs when the hero isn't moving
  private int lastCellMoved = -1;

  private static float initialDelay() {
    switch (DarkestPixelDungeon.movementHoldSensitivity()) {
      case 0:
        return Float.POSITIVE_INFINITY;
      case 1:
        return 0.13f;
      case 2:
        return 0.09f;
      case 3:
      default:
        return 0.06f;
      case 4:
        return 0.03f;
    }
  }

  public void onMoveKey(int keyCode, DPDAction action, boolean pressed) {
    if (action == null) return;

    if (!pressed) {
      if (!pressedMoveKeys.remove(keyCode)) return;

      if (heldAction1 == action) {
        heldAction1 = heldAction2;
        heldAction2 = heldAction3;
        heldAction3 = null;
      } else if (heldAction2 == action) {
        heldAction2 = heldAction3;
        heldAction3 = null;
      } else if (heldAction3 == action) {
        heldAction3 = null;
      }

      // Move immediately if the tap was released before initialDelay elapsed
      if (heldDelay > 0f && !delayingForRelease) {
        heldDelay = 0f;
        moveFromActions(action, heldAction1, heldAction2);
      }

      if (heldAction1 == null && heldAction2 == null) {
        resetKeyHold();
      } else {
        delayingForRelease = true;
        heldDelay = initialDelay();
      }
    } else {
      // Ignore OS key auto-repeat for keys already held down
      if (!pressedMoveKeys.add(keyCode)) return;

      if (!Dungeon.INSTANCE.isHeroNull()) {
        Dungeon.INSTANCE.getHero().setResting(false);
      }
      lastCellMoved = -1;
      if (heldAction1 == null) {
        heldAction1 = action;
        heldDelay = initialDelay();
        delayingForRelease = false;
        steppedInHold = false;
      } else if (heldAction2 == null) {
        heldAction2 = action;
      } else {
        heldAction3 = action;
      }
    }
  }

  @Override
  public void update() {
    super.update();

    if (!pressedMoveKeys.isEmpty()) {
      ArrayList<Integer> released = null;
      for (Integer code : pressedMoveKeys) {
        if (!Gdx.input.isKeyPressed(code)) {
          if (released == null) released = new ArrayList<>();
          released.add(code);
        }
      }
      if (released != null) {
        for (Integer code : released) {
          onMoveKey(code, DPDKeyBindings.actionForKey(code), false);
        }
      }
    }

    if (heldAction1 == null || GameScene.interfaceBlockingHero()) {
      return;
    }

    if (heldDelay > 0) {
      heldDelay -= Game.elapsed;
    }

    if (Dungeon.INSTANCE.isHeroNull()) return;
    Hero hero = Dungeon.INSTANCE.getHero();
    if (heldAction1 != null && hero.getReady()) {
      processKeyHold();
    } else if (hero.getReady()) {
      lastCellMoved = -1;
    }
  }

  private boolean moveFromActions(DPDAction... actions) {
    if (Dungeon.INSTANCE.isHeroNull()) return false;
    Hero hero = Dungeon.INSTANCE.getHero();
    if (!hero.getReady()) {
      return false;
    }

    if (GameScene.cancelCellSelector()) {
      return false;
    }

    int dx = 0;
    int dy = 0;
    for (DPDAction action : actions) {
      if (action == null) continue;
      dx += directionX(action);
      dy += directionY(action);
    }
    dx = Math.max(-1, Math.min(1, dx));
    dy = Math.max(-1, Math.min(1, dy));

    int cell = hero.getPos() + dx + dy * Dungeon.INSTANCE.getLevel().width();

    if (steppedInHold) {
      if (hero.visibleEnemies() > 0) {
        resetKeyHold();
        return false;
      }
      if (cell >= 0 && cell < Dungeon.INSTANCE.getLevel().length()) {
        Trap trap = Dungeon.INSTANCE.getLevel().getTraps().get(cell);
        if (trap != null && trap.getVisible()) {
          resetKeyHold();
          return false;
        }
      }
    }

    if (cell != hero.getPos() && cell != lastCellMoved) {
      lastCellMoved = cell;
      steppedInHold = true;
      if (hero.handle(cell)) {
        hero.next();
      }
      if (hero.visibleEnemies() > 0) {
        resetKeyHold();
      }
      return true;
    }

    return false;
  }

  private static int directionX(DPDAction action) {
    switch (action) {
      case EAST:
      case NORTH_EAST:
      case SOUTH_EAST:
        return 1;
      case WEST:
      case NORTH_WEST:
      case SOUTH_WEST:
        return -1;
      default:
        return 0;
    }
  }

  private static int directionY(DPDAction action) {
    switch (action) {
      case SOUTH:
      case SOUTH_WEST:
      case SOUTH_EAST:
        return 1;
      case NORTH:
      case NORTH_WEST:
      case NORTH_EAST:
        return -1;
      default:
        return 0;
    }
  }

  public void processKeyHold() {
    if (heldAction1 == null || GameScene.interfaceBlockingHero() || Dungeon.INSTANCE.isHeroNull()) {
      return;
    }
    int dx = directionX(heldAction1) + (heldAction2 != null ? directionX(heldAction2) : 0);
    int dy = directionY(heldAction1) + (heldAction2 != null ? directionY(heldAction2) : 0);
    if ((dx != 0 || dy != 0) && heldDelay <= 0) {
      Hero hero = Dungeon.INSTANCE.getHero();
      boolean wasEnabled = enabled;
      boolean wasReady = hero.getReady();
      enabled = true;
      hero.setReady(true);
      hero.setContinuousMoving(true);
      if (moveFromActions(heldAction1, heldAction2)) {
        hero.setReady(false);
      } else {
        enabled = wasEnabled;
        hero.setReady(wasReady);
      }
    }
  }

  public void resetKeyHold() {
    heldAction1 = heldAction2 = heldAction3 = null;
    pressedMoveKeys.clear();
    steppedInHold = false;
    delayingForRelease = false;
    heldDelay = 0f;
    if (!Dungeon.INSTANCE.isHeroNull()) {
      Dungeon.INSTANCE.getHero().stopContinuousMoving();
    }
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
