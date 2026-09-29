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

import com.egoal.darkestpixeldungeon.Challenge;
import com.egoal.darkestpixeldungeon.QuickSlot;
import com.egoal.darkestpixeldungeon.actors.hero.Belongings;
import com.egoal.darkestpixeldungeon.actors.hero.perks.Perk;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.scenes.PixelScene;
import com.egoal.darkestpixeldungeon.sprites.HeroSprite;
import com.egoal.darkestpixeldungeon.Assets;
import com.egoal.darkestpixeldungeon.Badges;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.Rankings;
import com.egoal.darkestpixeldungeon.Statistics;
import com.egoal.darkestpixeldungeon.items.Item;
import com.egoal.darkestpixeldungeon.ui.BadgesList;
import com.egoal.darkestpixeldungeon.ui.Icons;
import com.egoal.darkestpixeldungeon.ui.ItemSlot;
import com.egoal.darkestpixeldungeon.ui.PerkSlot;
import com.egoal.darkestpixeldungeon.ui.RedButton;
import com.egoal.darkestpixeldungeon.ui.RenderedTextMultiline;
import com.egoal.darkestpixeldungeon.ui.ScrollPane;
import com.egoal.darkestpixeldungeon.ui.Window;
import com.watabou.input.ScrollEvent;
import com.watabou.input.Touchscreen;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.RenderedText;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Button;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Point;
import com.watabou.utils.Signal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class WndRanking extends WndTabbed {

  private static final int WIDTH = 91;
  private static final int HEIGHT = 182;

  private Thread thread;
  private String error = null;

  private Image busy;

  public WndRanking(final Rankings.Record rec) {

    super();
    resize(WIDTH, HEIGHT);

    thread = new Thread() {
      @Override
      public void run() {
        try {
          Badges.INSTANCE.loadGlobal();
          Rankings.INSTANCE.LoadGameData(rec);
        } catch (Exception e) {
          error = Messages.get(WndRanking.class, "error");
        }
      }
    };
    thread.start();

    busy = Icons.BUSY.get();
    busy.origin.set(busy.width / 2, busy.height / 2);
    busy.angularSpeed = 720;
    busy.x = (WIDTH - busy.width) / 2;
    busy.y = (HEIGHT - busy.height) / 2;
    add(busy);
  }

  @Override
  public void update() {
    super.update();

    if (thread != null && !thread.isAlive()) {
      thread = null;
      if (error == null) {
        remove(busy);
        createControls();
      } else {
        hide();
        Game.scene().add(new WndError(error));
      }
    }
  }

  private void createControls() {

    String[] labels =
            {Messages.get(this, "stats"), Messages.get(this, "perks"),
                    Messages.get(this, "items"), Messages.get(this, "badges")};
    Group[] pages =
            {new StatsTab(), new PerksTab(), new ItemsTab(), new BadgesTab()};

    for (int i = 0; i < pages.length; i++) {

      add(pages[i]);

      Tab tab = new RankingTab(labels[i], pages[i]);
      add(tab);
    }

    layoutTabs();

    select(0);
  }

  private class RankingTab extends LabeledTab {

    private Group page;

    public RankingTab(String label, Group page) {
      super(label);
      this.page = page;
    }

    @Override
    protected void select(boolean value) {
      super.select(value);
      if (page != null) {
        page.visible = page.active = selected;
      }
    }
  }

  private class StatsTab extends Group {

    private int GAP = 6;

    public StatsTab() {
      super();

      String heroClass = Dungeon.INSTANCE.getHero().className();

      IconTitle title = new IconTitle();
      title.icon(HeroSprite.Companion.avatar(Dungeon.INSTANCE.getHero().getHeroClass(), Dungeon.INSTANCE.getHero()
              .tier
                      ()));
      title.label(Messages.get(this, "title", Dungeon.INSTANCE.getHero().getLvl(), heroClass)
              .toUpperCase(Locale.ENGLISH));
      title.color(Window.SHPX_COLOR);
      title.setRect(0, 0, WIDTH, 0);
      add(title);

      float pos = title.bottom();

      pos += GAP + GAP;

      pos = statSlot(this, Messages.get(this, "str"), Integer.toString
              (Dungeon.INSTANCE.getHero().getSTR()), pos);
      pos = statSlot(this, Messages.get(this, "health"), Integer.toString
              (Dungeon.INSTANCE.getHero().getHT()), pos);

      pos += GAP;

      pos = statSlot(this, Messages.get(this, "duration"), Integer.toString(
              (int) Statistics.INSTANCE.getDuration()), pos);

      pos += GAP;

      pos = statSlot(this, Messages.get(this, "depth"), Integer.toString
              (Statistics.INSTANCE.getDeepestFloor()), pos);
      pos = statSlot(this, Messages.get(this, "highest-damage"), Integer
              .toString(Statistics.INSTANCE.getHighestDamage()), pos);
      pos = statSlot(this, Messages.get(this, "enemies"), Integer.toString
              (Statistics.INSTANCE.getEnemiesSlain()), pos);
      pos = statSlot(this, Messages.get(this, "gold"), Integer.toString
              (Statistics.INSTANCE.getGoldCollected()), pos);

      pos += GAP;

      pos = statSlot(this, Messages.get(this, "food"), Integer.toString
              (Statistics.INSTANCE.getFoodEaten()), pos);
      pos = statSlot(this, Messages.get(this, "alchemy"), Integer.toString
              (Statistics.INSTANCE.getPotionsCooked()), pos);
      pos = statSlot(this, Messages.get(this, "ankhs"), Integer.toString
              (Statistics.INSTANCE.getAnkhsUsed()), pos);
    }

    private float statSlot(Group parent, String label, String value, float
            pos) {

      RenderedText txt = PixelScene.renderText(label, 7);
      txt.y = pos;
      parent.add(txt);

      txt = PixelScene.renderText(value, 7);
      txt.x = WIDTH * 0.65f;
      txt.y = pos;
      PixelScene.align(txt);
      parent.add(txt);

      return pos + GAP + txt.baseLine();
    }
  }

  private class PerksTab extends Group {
    public PerksTab() {
      super();

      int i = 0;
      for (final Perk perk : Dungeon.INSTANCE.getHero().getHeroPerk().getPerks()) {
        float x = i % 4 * 23f;
        float y = i / 4 * 23f;

        PerkSlot ps = new PerkSlot(perk) {
          @Override
          public void onClick() {
            Game.scene().add(new WndMessage(perk.description()));
          }
        };
        ps.setRect(x, y, 22f, 22f);

        add(ps);
        ++i;
      }
    }
  }

  private class ItemsTab extends Group {

    /**
     * Cell size of one quickslot. WIDTH (91) is exactly 4 of these plus the
     * gaps, so the grid is 4 columns wide by construction -- the width cannot
     * be traded for more rows.
     */
    private static final int COLUMNS = 4;
    private static final float SLOT = 22;
    private static final float SLOT_GAP = 1;
    private static final float QUICKSLOT_TOP_GAP = 6;

    public ItemsTab() {
      super();

      // Seed this Group's camera before building the list. The list lays its
      // content out against camera(), which walks the parent chain up to this
      // window; WndTabbed builds every page inside WndRanking.createControls()
      // BEFORE adding them to the window, so at construction time this Group
      // still has no parent and camera() would return null. BadgesTab handles
      // the same ordering problem the same way.
      camera = WndRanking.this.camera;

      // The equipped items plus the quickslot grid are taller than the window
      // for any run that filled more than one row of slots, so the list is
      // clipped to its own camera. Without that the grid simply drew outside
      // the content box: the equipment block ends at y = 138 and the window is
      // only HEIGHT = 182 tall, so a single row of slots (138..160) is all that
      // fits and every further row spilled over the tab bar.
      ScrollableList list = new ScrollableList(new Content());
      add(list);
      list.setRect(0, 0, WIDTH, HEIGHT);
    }

    /**
     * Scrolls a taller-than-visible content component without stealing input.
     *
     * <p>The game's ScrollPane would be the obvious choice, but its
     * TouchController is registered with Touchscreen.event *after* the
     * content's buttons, and that signal is stack-mode: add() pushes to the
     * front and dispatch stops at the first listener returning true. The
     * controller is also stretched over the whole pane, so it swallowed every
     * touch and the item cells -- although still drawn -- could never be
     * tapped or highlighted.
     *
     * <p>This class therefore registers no TouchArea at all. It only clips via
     * a Camera and scrolls on wheel and touch-drag events, which leaves the
     * cells' own hot areas as the sole touch listeners, so tapping,
     * highlighting and the click sound behave exactly as they would with no
     * container in between.
     *
     * <p>Touch dragging works by listening to Touchscreen.event without ever
     * consuming a press: a DOWN is passed straight through (return false), so
     * the cell underneath still highlights and clicks. Touchscreen delivers
     * every drag frame as dispatch(null) -- see processTouchEvents, where a
     * moving pointer only updates its Touch and dispatches null -- so the
     * listener keeps the Touch it saw go down and measures against its
     * start/current points. Because the cell's own TouchArea receives those
     * same null frames, it calls onDrag rather than onTouchUp, which is exactly
     * the behaviour wanted: the cell stays pressed while the finger moves and
     * no click fires. Only once a real drag has occurred does the UP get
     * consumed, which suppresses the release-click on a cell the finger
     * happened to end on.
     */
    private class ScrollableList extends Component implements Signal.Listener<ScrollEvent> {

      /** Finger travel, in screen pixels, before a press becomes a scroll. */
      private static final float DRAG_SLOP = 8;

      private final Component content;
      private final ColorBlock thumb;

      /** The pointer this pane is currently tracking, if any. */
      private Touchscreen.Touch tracked;
      private float dragStartY;
      private float dragStartScroll;
      private boolean dragged;

      /**
       * Drag-to-scroll listener for Touchscreen.event.
       *
       * <p>A separate class rather than a second interface on ScrollableList
       * because Signal.Listener is generic: implementing both
       * Listener&lt;ScrollEvent&gt; and Listener&lt;Touch&gt; on one class is a
       * compile error ("repeated interface"), since both erase to the same
       * raw type.
       */
      private final Signal.Listener<Touchscreen.Touch> dragListener =
              new Signal.Listener<Touchscreen.Touch>() {
        @Override
        public boolean onSignal(Touchscreen.Touch touch) {
          return handleTouch(touch);
        }
      };

      ScrollableList(Component content) {
        super();

        this.content = content;

        // Plain add() rather than addToBack(): Group.addToBack reads
        // members.get(0) without checking for an empty list, so calling it as
        // the very first member throws IndexOutOfBoundsException. ScrollPane
        // only avoids that because its createChildren() has already added a
        // TouchController by then.
        add(content);

        width = content.width();
        height = content.height();

        // Own camera, so the content is clipped to the pane's rect rather than
        // spilling over the tab bar.
        content.camera = new Camera(0, 0, 1, 1, PixelScene.defaultZoom);
        Camera.add(content.camera);

        ScrollEvent.addScrollListener(this);

        // Registered after the cells have added their own hot areas, so the
        // stack-mode signal puts this listener in front of them. That ordering
        // is what lets a DOWN be seen here first and still be passed on.
        Touchscreen.event.add(dragListener);

        thumb = new ColorBlock(2, 1, 0xFF7b8073);
        thumb.am = 0.5f;
        add(thumb);
      }

      @Override
      public void destroy() {
        ScrollEvent.removeScrollListener(this);
        Touchscreen.event.remove(dragListener);
        Camera.remove(content.camera);
        super.destroy();
      }

      @Override
      protected void layout() {
        content.setPos(0, 0);

        Point p = camera().cameraToScreen(x, y);
        Camera cs = content.camera;
        cs.x = p.x;
        cs.y = p.y;
        cs.resize((int) width, (int) height);

        boolean scrollable = height < content.height();
        thumb.visible = scrollable;
        if (scrollable) {
          thumb.scale.set(2, height * height / content.height());
          thumb.x = right() - thumb.width();
          thumb.y = y + height * content.camera.scroll.y / content.height();
        }
      }

      @Override
      public boolean onSignal(ScrollEvent event) {
        if (!isActive() || !containsScreenPoint(event.pos.x, event.pos.y)) {
          return false;
        }
        scrollBy(event.amount * 10);
        return true;
      }

      /**
       * Touch handling for drag-to-scroll.
       *
       * <p>Returns false for a DOWN on purpose: this listener only observes the
       * press so the cell's hot area still gets it and the cell highlights and
       * clicks normally. The press is claimed only afterwards, once the finger
       * has travelled far enough to count as a drag.
       */
      private boolean handleTouch(Touchscreen.Touch touch) {
        if (!isActive()) {
          return false;
        }

        // A null touch is a drag frame for whichever pointer is already down.
        if (touch == null) {
          if (tracked == null) {
            return false;
          }

          float dy = tracked.current.y - dragStartY;
          if (!dragged && Math.abs(dy) < DRAG_SLOP) {
            // Still within the slop: treat as a stationary press so the cell
            // keeps its normal pressed feedback.
            return false;
          }
          dragged = true;

          // The pane follows the finger, so scrolling up moves the content up.
          // The scroll offset is computed from the press origin rather than
          // accumulated per frame, which keeps it exact and free of drift.
          setScroll(dragStartScroll + dy);
          return true;
        }

        if (touch.down) {
          // Only start tracking a press that landed inside this pane. Touches
          // elsewhere are left entirely alone (the window's own buttons, the
          // tab bar, and the close button all keep working).
          if (tracked == null && containsScreenPoint(touch.current.x, touch.current.y)) {
            tracked = touch;
            dragStartY = touch.current.y;
            dragStartScroll = content.camera.scroll.y;
            dragged = false;
          }
          // Never consume the DOWN, even when tracking: the cell must still
          // receive it.
          return false;
        }

        // A release. Consume it only if this press turned into a scroll, so a
        // drag ending over a cell does not also open that cell's item window.
        boolean consume = dragged && tracked == touch;
        tracked = null;
        dragged = false;
        return consume;
      }

      /**
       * Whether a screen-space point falls inside this pane. Component is not a
       * Visual, so the check is done by converting the point into camera space
       * exactly as Visual.overlapsScreenPoint would.
       */
      private boolean containsScreenPoint(float sx, float sy) {
        Camera c = camera();
        if (c == null) {
          return false;
        }
        float px = (sx - c.x) / c.zoom + c.scroll.x;
        float py = (sy - c.y) / c.zoom + c.scroll.y;
        return px >= x && px < x + width && py >= y && py < y + height;
      }

      private void scrollBy(float dy) {
        setScroll(content.camera.scroll.y + dy);
      }

      /** Sets the vertical scroll offset, clamped to the content's bounds. */
      private void setScroll(float sy) {
        Camera c = content.camera;
        c.scroll.y = Math.max(0, Math.min(sy,
                Math.max(0, content.height() - height)));
        thumb.y = y + height * c.scroll.y / content.height();
      }
    }

    /**
     * Holds the equipped items followed by the quickslots that actually held
     * something in this run. It extends Component (not Group) so it has a real
     * width/height of its own for the list to clip and scroll -- Group has no
     * size at all, and a bare width/height inside a Group would silently
     * resolve to the enclosing Window's fields instead.
     */
    private class Content extends Component {

      private float pos;

      public Content() {
        super();

        Belongings stuff = Dungeon.INSTANCE.getHero().getBelongings();
        if (stuff.getWeapon() != null) {
          addItem(stuff.getWeapon());
        }
        if (stuff.getArmor() != null) {
          addItem(stuff.getArmor());
        }
        if (stuff.getHelmet() != null)
          addItem(stuff.getHelmet());
        if (stuff.getMisc1() != null) {
          addItem(stuff.getMisc1());
        }
        if (stuff.getMisc2() != null) {
          addItem(stuff.getMisc2());
        }
        if (stuff.getMisc3() != null) {
          addItem(stuff.getMisc3());
        }

        // Only quickslots that actually held an item in this run are listed.
        // Empty slots used to be drawn as bare ColorBlock placeholders in a
        // grid sized to the full QuickSlot.SIZE capacity, which both wasted
        // rows and made empty cells look identical to occupied ones.
        ArrayList<Item> quickItems = new ArrayList<Item>();
        for (int i = 0; i < QuickSlot.SIZE; i++) {
          Item item = Dungeon.INSTANCE.getQuickslot().getItem(i);
          if (item != null) {
            quickItems.add(item);
          }
        }
        if (quickItems.isEmpty()) {
          setSize(WIDTH, pos);
          return;
        }

        float baseY = pos + QUICKSLOT_TOP_GAP;
        for (int i = 0; i < quickItems.size(); i++) {
          float posx = (i % COLUMNS) * (SLOT + SLOT_GAP);
          float posy = baseY + (i / COLUMNS) * (SLOT + SLOT_GAP);

          // WndRanking's own read-only QuickSlotButton (extends ItemSlot); its
          // onClick opens WndItem, the item description, like the rows above.
          QuickSlotButton slot = new QuickSlotButton(quickItems.get(i));
          slot.setRect(posx, posy, SLOT, SLOT);
          add(slot);
        }

        setSize(WIDTH, baseY + ((quickItems.size() - 1) / COLUMNS + 1) * (SLOT + SLOT_GAP));
      }

      private void addItem(Item item) {
        ItemButton slot = new ItemButton(item);
        slot.setRect(0, pos, WIDTH, ItemButton.HEIGHT);
        add(slot);

        pos += slot.height() + 1;
      }
    }
  }

  private class BadgesTab extends Group {

    public BadgesTab() {
      super();

      camera = WndRanking.this.camera;

      HashSet<Challenge> challenges = Dungeon.INSTANCE.getHero().getChallenges();
      if (!challenges.isEmpty()) {
        //todo: redesign & use icon
        float x = 2f;
        float y = 2f;
        for(Challenge challenge: challenges) {
          RenderedText title = PixelScene.renderText(challenge.title(), 6);
          title.x = x;
          title.y = y;
          add(title);
          RenderedTextMultiline desc = PixelScene.renderMultiline(challenge.desc(), 6);
          desc.maxWidth(WIDTH - 4);
          desc.setPos(2f, title.y + title.height() + 2f);
          add(desc);

          y = desc.bottom()+ 4f;
        }
      } else {
        ScrollPane list = new BadgesList(false);
        add(list);

        list.setSize(WIDTH, HEIGHT);
      }
    }
  }

  private class ItemButton extends Button {

    public static final int HEIGHT = 22;  // same as bag

    private Item item;

    private ItemSlot slot;
    private ColorBlock bg;
    private RenderedText name;

    public ItemButton(Item item) {

      super();

      this.item = item;

      slot.item(item);
      if (item.getCursed() && item.getCursedKnown()) {
        bg.ra = +0.2f;
        bg.ga = -0.1f;
      } else if (!item.isIdentified()) {
        bg.ra = 0.1f;
        bg.ba = 0.1f;
      }
    }

    @Override
    protected void createChildren() {

      bg = new ColorBlock(HEIGHT, HEIGHT, 0x9953564D);
      add(bg);

      slot = new ItemSlot();
      add(slot);

      name = PixelScene.renderText("?", 7);
      add(name);

      super.createChildren();
    }

    @Override
    protected void layout() {
      bg.x = x;
      bg.y = y;

      slot.setRect(x, y, HEIGHT, HEIGHT);
      PixelScene.align(slot);

      name.x = slot.right() + 2;
      name.y = y + (height - name.baseLine()) / 2;
      PixelScene.align(name);

      String str = Messages.titleCase(item.name());
      name.text(str);
      if (name.width() > width - name.x) {
        do {
          str = str.substring(0, str.length() - 1);
          name.text(str + "...");
        } while (name.width() > width - name.x);
      }

      super.layout();
    }

    @Override
    protected void onTouchDown() {
      bg.brightness(1.5f);
      Sample.INSTANCE.play(Assets.SND_CLICK, 0.7f, 0.7f, 1.2f);
    }

    ;

    protected void onTouchUp() {
      bg.brightness(1.0f);
    }

    ;

    @Override
    public void onClick() {
      Game.scene().add(new WndItem(null, item));
    }
  }

  private class QuickSlotButton extends ItemSlot {

    public static final int HEIGHT = 22;

    private Item item;
    private ColorBlock bg;

    QuickSlotButton(Item item) {
      super(item);
      this.item = item;
    }

    @Override
    protected void createChildren() {
      bg = new ColorBlock(HEIGHT, HEIGHT, 0x9953564D);
      add(bg);

      super.createChildren();
    }

    @Override
    protected void layout() {
      bg.x = x;
      bg.y = y;

      super.layout();
    }

    @Override
    protected void onTouchDown() {
      bg.brightness(1.5f);
      Sample.INSTANCE.play(Assets.SND_CLICK, 0.7f, 0.7f, 1.2f);
    }

    ;

    protected void onTouchUp() {
      bg.brightness(1.0f);
    }

    ;

    @Override
    public void onClick() {
      Game.scene().add(new WndItem(null, item));
    }
  }
}
