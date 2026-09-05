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
package com.egoal.darkestpixeldungeon.windows;

import com.egoal.darkestpixeldungeon.items.quest.Embers;
import com.egoal.darkestpixeldungeon.ui.RenderedTextMultiline;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.egoal.darkestpixeldungeon.items.Item;
import com.egoal.darkestpixeldungeon.items.quest.CorpseDust;
import com.egoal.darkestpixeldungeon.items.wands.Wand;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.plants.Rotberry;
import com.egoal.darkestpixeldungeon.scenes.PixelScene;
import com.egoal.darkestpixeldungeon.sprites.ItemSprite;
import com.egoal.darkestpixeldungeon.ui.RedButton;
import com.egoal.darkestpixeldungeon.ui.Window;
import com.watabou.input.Keys;
import com.badlogic.gdx.Input;
import java.util.ArrayList;
import com.egoal.darkestpixeldungeon.utils.GLog;

public class WndWandmaker extends Window {
  private final ArrayList<RedButton> buttons = new ArrayList<>();
  private int selected = 0;

  private static final int WIDTH = 120;
  private static final int BTN_HEIGHT = 20;
  private static final float GAP = 2;

  public WndWandmaker(final Wandmaker wandmaker, final Item item) {

    super();

    IconTitle titlebar = new IconTitle();
    titlebar.icon(new ItemSprite(item.image(), null));
    titlebar.label(Messages.titleCase(item.name()));
    titlebar.setRect(0, 0, WIDTH, 0);
    add(titlebar);

    String msg = "";
    if (item instanceof CorpseDust) {
      msg = Messages.get(this, "dust");
    } else if (item instanceof Embers) {
      msg = Messages.get(this, "ember");
    } else if (item instanceof Rotberry.Seed) {
      msg = Messages.get(this, "berry");
    }

    RenderedTextMultiline message = PixelScene.renderMultiline(msg, 6);
    message.maxWidth(WIDTH);
    message.setPos(0, titlebar.bottom() + GAP);
    add(message);

    RedButton btnWand1 = new RedButton(Wandmaker.Quest.INSTANCE.getWand1().name()) {
      @Override
      public void onClick() {
        selectReward(wandmaker, item, Wandmaker.Quest.INSTANCE.getWand1());
      }
    };
    btnWand1.setRect(0, message.top() + message.height() + GAP, WIDTH, 
            BTN_HEIGHT);
    add(btnWand1);
    buttons.add(btnWand1);
    btnWand1.textColor(TITLE_COLOR);

    RedButton btnWand2 = new RedButton(Wandmaker.Quest.INSTANCE.getWand2().name()) {
      @Override
      public void onClick() {
        selectReward(wandmaker, item, Wandmaker.Quest.INSTANCE.getWand2());
      }
    };
    btnWand2.setRect(0, btnWand1.bottom() + GAP, WIDTH, BTN_HEIGHT);
    add(btnWand2);
    buttons.add(btnWand2);

    resize(WIDTH, (int) btnWand2.bottom());
  }

  @Override
  public boolean onSignal(Keys.Key key) {
    if (key.pressed && !buttons.isEmpty()) {
      if (key.code == Input.Keys.UP || key.code == Input.Keys.DOWN) {
        selected = (selected + 1) % buttons.size();
        for (int i = 0; i < buttons.size(); i++) buttons.get(i).textColor(i == selected ? TITLE_COLOR : 0xFFFFFF);
        return true;
      } else if (key.code == Input.Keys.ENTER) {
        buttons.get(selected).onClick();
        return true;
      }
    }
    return super.onSignal(key);
  }

  private void selectReward(Wandmaker wandmaker, Item item, Wand reward) {

    hide();

    item.detach(Dungeon.INSTANCE.getHero().getBelongings().getBackpack());

    reward.identify();
    if (reward.doPickUp(Dungeon.INSTANCE.getHero())) {
      GLog.i(Messages.get(Dungeon.INSTANCE.getHero(), "you_now_have", reward.name()));
    } else {
      Dungeon.INSTANCE.getLevel().drop(reward, wandmaker.getPos()).getSprite().drop();
    }

    wandmaker.yell(Messages.get(this, "farewell", Dungeon.INSTANCE.getHero().givenName()));
    wandmaker.destroy();

    wandmaker.getSprite().die();

    Wandmaker.Quest.INSTANCE.complete();
  }
}
