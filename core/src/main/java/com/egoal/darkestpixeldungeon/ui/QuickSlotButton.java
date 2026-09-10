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
package com.egoal.darkestpixeldungeon.ui;

import com.egoal.darkestpixeldungeon.DungeonTilemap;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.items.Item;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.scenes.GameScene;
import com.egoal.darkestpixeldungeon.scenes.PixelScene;
import com.egoal.darkestpixeldungeon.utils.BArray;
import com.egoal.darkestpixeldungeon.windows.WndBag;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Button;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QuickSlotButton extends Button implements WndBag.Listener {
  private static final int NUM_BUTTONS = 8* 2;

  private static QuickSlotButton[] instance = new QuickSlotButton[NUM_BUTTONS];
  private int slotNum;

  private ItemSlot slot;

  private static Image crossB;
  private static Image crossM;

  private static boolean targeting = false;
  private static int targetingSlot = -1;
  private static int targetCell = -1;
  private static Item targetingItem = null;
  public static Char lastTarget = null;

  public QuickSlotButton(int slotNum) {
    super();
    this.slotNum = slotNum;
    item(select(slotNum));

    instance[slotNum] = this;
  }

  @Override
  public void destroy() {
    super.destroy();

    reset();
  }

  public static void reset() {
    instance = new QuickSlotButton[NUM_BUTTONS];

    lastTarget = null;
    targetingSlot = -1;
    targetCell = -1;
    targetingItem = null;
  }

  @Override
  protected void createChildren() {
    super.createChildren();

    slot = new ItemSlot() {
      @Override
      public void onClick() {
        QuickSlotButton.this.useItem();
      }

      @Override
      protected boolean onLongClick() {
        return QuickSlotButton.this.onLongClick();
      }

      @Override
      protected void onTouchDown() {
        getIcon().lightness(0.7f);
      }

      @Override
      protected void onTouchUp() {
        getIcon().resetColor();
      }
    };
    slot.showParams(true, false, true);
    add(slot);

    crossB = Icons.TARGET.get();
    crossB.visible = false;
    add(crossB);

    crossM = new Image();
    crossM.copy(crossB);
  }

  public static void use(int slotNum) {
    // Keyboard shortcuts can arrive faster than the actor turn completes.
    // Match the toolbar's disabled state and never execute an item while the
    // hero is busy, otherwise a rapid press can reuse a detached stack item.
    if (Dungeon.INSTANCE.isHeroNull() || !Dungeon.INSTANCE.getHero().getReady()) {
      return;
    }
    if (slotNum >= 0 && slotNum < instance.length && instance[slotNum] != null) {
      QuickSlotButton button = instance[slotNum];
      if (!button.active) return;
      if (select(slotNum) == null) {
        if (targeting) cancel();
        button.onClick();
      } else {
        button.useItem();
      }
    }
  }

  private void useItem() {
    // NOTE: there is deliberately no "same slot while targeting" early return
    // here. Clicking the active slot again is how the player fires at the
    // currently selected/auto-aimed target, so an early return would break that.
    // The duplicate-execution case is instead handled below by rejecting
    // zero-quantity items (a targeting item is consumed when its action starts).
    Item item = select(slotNum);
    // A slot can be cleared (for example, when its item is removed) while the
    // button or an in-progress targeting state is still present. Treat that as
    // an empty slot instead of dereferencing a stale null item.
    if (item == null) {
      if (targeting) cancel();
      return;
    }
    // Keyboard shortcuts reach this method without going through the disabled
    // inner slot, so a used-up placeholder (quantity 0) can get here. Never
    // execute a zero-quantity item: Item.cast would NPE when detach() returns
    // null after the throw.
    if (item.quantity() == 0) {
      if (targeting) cancel();
      return;
    }

    if (targeting) {
      if (targetingSlot != slotNum) {
        cancel();
        useItem();
        return;
      }
      fireAtTarget(item);
    } else {
      if (item.getUsesTargeting()) useTargeting(item);
      item.execute(Dungeon.INSTANCE.getHero());
    }
  }

  @Override
  protected void layout() {
    super.layout();

    slot.fill(this);

    crossB.x = x + (width - crossB.width) / 2;
    crossB.y = y + (height - crossB.height) / 2;
    PixelScene.align(crossB);
  }

  @Override
  public void onClick() {
    GameScene.selectItem(this, WndBag.Mode.QUICKSLOT, Messages.get(this, 
            "select_item"));
  }

  @Override
  protected boolean onLongClick() {
    GameScene.selectItem(this, WndBag.Mode.QUICKSLOT, Messages.get(this, 
            "select_item"));
    return true;
  }

  private static Item select(int slotNum) {
    return Dungeon.INSTANCE.getQuickslot().getItem(slotNum);
  }

  @Override
  public void onSelect(Item item) {
    if (item != null) {
      Dungeon.INSTANCE.getQuickslot().setSlot(slotNum, item);
      refresh();
    }
  }

  public void item(Item item) {
    slot.item(item);
    enableSlot();
  }

  public void enable(boolean value) {
    active = value;
    if (value) {
      enableSlot();
    } else {
      slot.enable(false);
    }
  }

  private void enableSlot() {
    slot.enable(Dungeon.INSTANCE.getQuickslot().isNonePlaceholder(slotNum));
  }

  private void useTargeting(Item item) {
    beginTargeting(item, slotNum);
    if (targeting) {
      crossB.x = x + (width - crossB.width) / 2;
      crossB.y = y + (height - crossB.height) / 2;
      crossB.visible = true;
    }
  }

  public static void beginTargeting(Item item) {
    // Bind targeting to the item's quickslot when it has one, so tapping that
    // slot confirms the target. Items without a slot keep external targeting.
    int slotNum = Dungeon.INSTANCE.getQuickslot().getSlot(item);
    if (slotNum >= 0 && slotNum < instance.length && instance[slotNum] != null) {
      instance[slotNum].useTargeting(item);
    } else {
      beginTargeting(item, slotNum);
    }
  }

  private static void beginTargeting(Item item, int slotNum) {
    List<Char> targets = availableTargets(item);
    lastTarget = targets.isEmpty() ? null : targets.get(0);
    targeting = true;
    targetingSlot = slotNum;
    targetingItem = item;
    if (lastTarget == null) {
      targetCell = Dungeon.INSTANCE.getHero().getPos();
      showTargetCell(targetCell);
    } else {
      targetCell = lastTarget.getPos();
      showTarget(lastTarget);
    }
  }

  public static boolean isTargeting() {
    return targeting;
  }

  public static void cycleTarget() {
    if (!targeting || targetingItem == null) return;
    List<Char> targets = availableTargets(targetingItem);
    if (targets.isEmpty()) return;
    int index = targets.indexOf(lastTarget);
    lastTarget = targets.get((index + 1) % targets.size());
    showTarget(lastTarget);
  }

  public static void moveTarget(int dx, int dy) {
    if (!targeting || targetingItem == null) return;
    int width = Dungeon.INSTANCE.getLevel().width();
    int height = Dungeon.INSTANCE.getLevel().height();
    int x = targetCell % width + dx;
    int y = targetCell / width + dy;
    if (x >= 0 && x < width && y >= 0 && y < height) {
      lastTarget = null;
      targetCell = x + y * width;
      showTargetCell(targetCell);
    }
  }

  private static List<Char> availableTargets(Item item) {
    ArrayList<Char> targets = new ArrayList<>();
    for (Char target : Dungeon.INSTANCE.getHero().visibleEnemyList()) {
      if (target.isAlive() && Dungeon.INSTANCE.getVisible()[target.getPos()] &&
              autoAim(target, item) != -1) targets.add(target);
    }
    final int heroPos = Dungeon.INSTANCE.getHero().getPos();
    targets.sort(Comparator.comparingInt(target ->
            Dungeon.INSTANCE.getLevel().distance(heroPos, target.getPos())));
    return targets;
  }

  private static void showTarget(Char target) {
    targetCell = target.getPos();
    crossM.remove();
    target.getSprite().parent.add(crossM);
    crossM.point(DungeonTilemap.tileToWorld(target.getPos()));
    HealthIndicator.instance.target(target);
  }

  private static void showTargetCell(int cell) {
    crossM.remove();
    Dungeon.INSTANCE.getHero().getSprite().parent.add(crossM);
    crossM.point(DungeonTilemap.tileToWorld(cell));
    HealthIndicator.instance.target(null);
  }

  public static boolean confirmExternalTarget() {
    // Confirm works for any active targeting, whether or not it is bound to a
    // quickslot, so keyboard confirm stays available alongside slot taps.
    if (!targeting || targetCell < 0) return false;
    fireAtTarget(targetingItem);
    return true;
  }

  private static void fireAtTarget(Item item) {
    int cell = lastTarget == null ? targetCell : autoAim(lastTarget, item);
    GameScene.handleCell(cell != -1 ? cell : targetCell);
  }

  public static int autoAim(Char target) {
    //will use generic projectile logic if no item is specified
    return autoAim(target, new Item());
  }

  //FIXME: this is currently very expensive, should either optimize 
  // ballistica or this, or both
  public static int autoAim(Char target, Item item) {

    //first try to directly targetpos
    if (item.throwPos(Dungeon.INSTANCE.getHero(), target.getPos()) == target.getPos()) {
      return target.getPos();
    }

    //Otherwise pick nearby tiles to try and 'angle' the shot, auto-aim 
    // basically.
    PathFinder.buildDistanceMap(target.getPos(), BArray.not(new boolean[Dungeon.INSTANCE.getLevel().length()], null), 2);
    for (int i = 0; i < PathFinder.distance.length; i++) {
      if (PathFinder.distance[i] < Integer.MAX_VALUE
              && item.throwPos(Dungeon.INSTANCE.getHero(), i) == target.getPos())
        return i;
    }

    //couldn't find a cell, give up.
    return -1;
  }

  public static void refresh() {
    for (int i = 0; i < instance.length; i++) {
      if (instance[i] != null) {
        instance[i].item(select(i));
      }
    }
  }

  public static void target(Char target) {
    if (target != Dungeon.INSTANCE.getHero()) {
      lastTarget = target;

      HealthIndicator.instance.target(target);
    }
  }

  public static void cancel() {
    if (targeting) {
      crossB.visible = false;
      crossM.remove();
      targeting = false;
      targetingSlot = -1;
      targetCell = -1;
      targetingItem = null;
    }
  }
}
