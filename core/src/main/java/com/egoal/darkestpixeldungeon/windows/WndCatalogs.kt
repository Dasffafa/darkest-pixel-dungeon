package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.perks.PerkImageSheet
import com.egoal.darkestpixeldungeon.actors.mobs.Mob
import com.egoal.darkestpixeldungeon.items.Catalog
import com.egoal.darkestpixeldungeon.items.CatalogAppearance
import com.egoal.darkestpixeldungeon.items.CatalogKey
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.artifacts.Artifact
import com.egoal.darkestpixeldungeon.items.armor.Armor
import com.egoal.darkestpixeldungeon.items.food.Wine
import com.egoal.darkestpixeldungeon.items.potions.Potion
import com.egoal.darkestpixeldungeon.items.potions.Reagent
import com.egoal.darkestpixeldungeon.items.rings.Ring
import com.egoal.darkestpixeldungeon.items.scrolls.Scroll
import com.egoal.darkestpixeldungeon.items.weapon.Weapon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.sprites.CrabSprite
import com.egoal.darkestpixeldungeon.sprites.GhostSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.egoal.darkestpixeldungeon.ui.RedButton
import com.egoal.darkestpixeldungeon.ui.RenderedTextMultiline
import com.egoal.darkestpixeldungeon.ui.ScrollPane
import com.egoal.darkestpixeldungeon.ui.Window
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.ColorBlock
import com.watabou.noosa.Image
import com.watabou.noosa.Visual
import com.watabou.noosa.ui.Component
import com.watabou.utils.Log
import java.util.ArrayList

class WndCatalogs : WndTabbed() {

    private val btnTitle: RedButton

    /** the sub-page buttons of every tab that has sub-pages, by tab index */
    private val sectionButtons = HashMap<Int, List<RedButton>>()

    /** how many rows those buttons take, by tab index */
    private val sectionRows = HashMap<Int, Int>()

    /** the rows of the list tabs */
    private val entries = ArrayList<ListItem>()

    /** the cells of the perk tab, which is a grid of icons rather than a list */
    private val perkCells = ArrayList<PerkCell>()

    /** the active sub-page of the current tab */
    private var section = 0

    private val list = object : ScrollPane(Component()) {
        override fun onClick(x: Float, y: Float) {
            for (cell in perkCells) {
                if (cell.onClick(x, y)) {
                    return
                }
            }

            val size = entries.size
            for (i in 0 until size) {
                if (entries[i].onClick(x, y)) {
                    break
                }
            }
        }
    }

    init {
        // debug runs (desktop runDebug / the debug preference) list everything
        Catalog.ApplyDebugUnlock()

        // the randomized looks (potions, scrolls, rings) are the catalog's own,
        // re-dealt on every open rather than borrowed from the running save
        CatalogAppearance.reroll()

        val wndWidth = if (DarkestPixelDungeon.landscape()) WIDTH_L else WIDTH_P
        resize(wndWidth, HEIGHT)

        btnTitle = RedButton(titleText(), 9)
        btnTitle.textColor(Window.TITLE_COLOR)
        btnTitle.setRect(0f, 0f, wndWidth.toFloat(), btnTitle.reqHeight())
        PixelScene.align(btnTitle)
        add(btnTitle)

        // one button per sub-page, on the title's row(s); only the active tab's
        // buttons are ever visible
        for ((tab, keys) in SECTIONS) {
            val buttons = keys.mapIndexed { i, key ->
                val btn = object : RedButton(M.L(WndCatalogs::class.java, key), 9) {
                    override fun onClick() {
                        selectSection(i)
                    }
                }
                btn.setRect(0f, 0f, wndWidth.toFloat(), btnTitle.height())
                btn.visible = false
                add(btn)
                btn
            }

            val columns = sectionColumns(buttons, wndWidth.toFloat())
            val cellWidth = wndWidth / columns.toFloat()
            buttons.forEachIndexed { i, btn ->
                btn.setRect((i % columns) * cellWidth, (i / columns) * btnTitle.height(),
                        cellWidth, btnTitle.height())
                PixelScene.align(btn)
            }

            sectionButtons[tab] = buttons
            sectionRows[tab] = (keys.size + columns - 1) / columns
        }

        add(list)
        list.setRect(0f, btnTitle.bottom() + 1f, wndWidth.toFloat(), height.toFloat() - btnTitle.height() - 1f)

        // every category is a tab of its own, so all of them switch the same way
        for (i in TABS.indices) {
            val icon = tabIcon(i)
            val tab = object : Tab() {
                override fun layout() {
                    super.layout()

                    icon.x = x + (width - icon.width()) / 2f
                    icon.y = y + (height - icon.height()) / 2f - 1f
                    PixelScene.align(icon)
                }

                override fun select(value: Boolean) {
                    super.select(value)

                    icon.am = if (selected) 1f else 0.6f

                    if (selected) {
                        CurrentTab = i
                        section = 0
                        btnTitle.text(titleText())
                        updateHeader()
                        updateList()
                    }
                }
            }

            tab.add(icon)
            add(tab)
        }

        layoutTabs()

        select(CurrentTab)
    }

    private fun titleText(): String = "${M.L(this, "title")} · ${M.L(WndCatalogs::class.java, TABS[CurrentTab])}"

    /**
     * One icon per category, taken from the game's own art. Ten tabs share the
     * width of the window, so every icon is scaled down to [TAB_ICON_SCALE].
     */
    private fun tabIcon(index: Int): Visual {
        val icon: Visual = when (index) {
            0 -> ItemSprite(ItemSpriteSheet.POTION_CRIMSON, null)
            1 -> ItemSprite(ItemSpriteSheet.SCROLL_KAUNAN, null)
            2 -> ItemSprite(ItemSpriteSheet.RING_HOLDER, null)
            3 -> ItemSprite(ItemSpriteSheet.ARTIFACT_HOURGLASS, null)
            4 -> ItemSprite(ItemSpriteSheet.RATION, null)
            5 -> CrabSprite()
            6 -> GhostSprite()
            7 -> ItemSprite(ItemSpriteSheet.ARMOR_PLATE, null)
            8 -> ItemSprite(ItemSpriteSheet.WORN_SHORTSWORD, null)
            9 -> Image(TextureCache.get(Assets.PERKS))
                    .apply { frame(CatalogInfo.perkFilm.get(PerkImageSheet.EXTRA_CHOICE)) }
            else -> ItemSprite(ItemSpriteSheet.SOMETHING, null)
        }

        icon.scale.set(TAB_ICON_SCALE, TAB_ICON_SCALE)
        return icon
    }

    private fun selectSection(index: Int) {
        if (section == index) return
        section = index
        updateHeader()
        updateList()
    }

    /**
     * How many sub-page buttons share a row. A label is allowed to overrun its
     * button a little, which the Russian potion row already does with
     * "Улучшенные"; a longer one such as "Enchantments" wraps onto another row.
     */
    private fun sectionColumns(buttons: List<RedButton>, wndWidth: Float): Int {
        for (columns in buttons.size downTo 1) {
            val slot = wndWidth / columns
            if (buttons.all { it.reqWidth() <= slot * MAX_LABEL_SPILL }) return columns
        }
        return 1
    }

    private fun updateHeader() {
        val keys = SECTIONS[CurrentTab]
        btnTitle.visible = keys == null

        // the active sub-page keeps the title colour, the others are dimmed
        for ((tab, buttons) in sectionButtons) {
            val active = tab == CurrentTab
            buttons.forEachIndexed { i, btn ->
                btn.visible = active
                if (active) btn.textColor(if (section == i) Window.TITLE_COLOR else SECTION_DIM)
            }
        }

        // a wrapped row of sub-page buttons pushes the list down
        val headerHeight = btnTitle.height() * (if (keys == null) 1 else sectionRows[CurrentTab] ?: 1)
        list.setRect(0f, headerHeight + 1f, width.toFloat(), height.toFloat() - headerHeight - 1f)
    }

    private fun updateList() {
        // the perk tab draws a grid of icons instead of rows of text
        if (CurrentTab == PERK_TAB) {
            showPerkGrid()
            return
        }

        val entries = ArrayList<Entry>()

        try {
            when (CurrentTab) {
                POTIONS_TAB -> when (section) {
                    0 -> {
                        Potion.known.forEach { entries.add(Entry.ItemEntry(it)) }
                        Potion.unknown.forEach { entries.add(Entry.ItemEntry(it)) }
                    }
                    1 -> (Potion.known + Potion.unknown).forEach { cls ->
                        if (cls.newInstance().canBeReinforced())
                            entries.add(Entry.ItemEntry(cls, reinforced = true))
                    }
                    else -> Reagent.all.forEach { entries.add(Entry.ItemEntry(it)) }
                }
                1 -> {
                    Scroll.known.forEach { entries.add(Entry.ItemEntry(it)) }
                    Scroll.unknown.forEach { entries.add(Entry.ItemEntry(it)) }
                }
                2 -> entries.addAll(catalogEntries(Catalog.Ring))
                3 -> entries.addAll(catalogEntries(Catalog.ARTIFACT))
                4 -> entries.addAll(catalogEntries(Catalog.FOOD))
                MOBS_TAB -> entries.addAll(mobEntries(Catalog.MobSections.getOrElse(section) { emptyList() }))
                6 -> entries.addAll(mobEntries(Catalog.NPC))
                ARMOR_TAB -> when (section) {
                    0 -> entries.addAll(catalogEntries(Catalog.ARMOR))
                    1 -> entries.addAll(effectEntries(Catalog.GLYPH))
                    else -> entries.addAll(effectEntries(Catalog.ARMOR_CURSE))
                }
                WEAPON_TAB -> when (section) {
                    0 -> entries.addAll(catalogEntries(Catalog.WEAPON))
                    1 -> entries.addAll(effectEntries(Catalog.ENCHANTMENT))
                    2 -> entries.addAll(effectEntries(Catalog.INSCRIPTION))
                    else -> entries.addAll(effectEntries(Catalog.WEAPON_CURSE))
                }
            }
        } catch (e: Exception) {
            // the catalog is also opened outside a run, where some game state
            // (item labels, gems) may not exist yet: show what we have instead
            Log.e("dpd", "failed to build catalog tab " + CurrentTab, e)
        }

        showList(entries)
    }

    private fun catalogEntries(cat: Catalog): List<Entry> =
            cat.allItems().sortedBy { if (Catalog.IsSeen(it)) 0 else 1 }
                    .map { Entry.ItemEntry(castClass(it.cls)) }

    private fun mobEntries(cat: Catalog): List<Entry> =
            cat.allItems().sortedBy { if (Catalog.IsSeen(it)) 0 else 1 }
                    .map { Entry.MobEntry(it) }

    private fun mobEntries(classes: Collection<Class<*>>): List<Entry> =
            classes.map { CatalogKey(it) }.sortedBy { if (Catalog.IsSeen(it)) 0 else 1 }
                    .map { Entry.MobEntry(it) }

    private fun effectEntries(cat: Catalog): List<Entry> =
            cat.allItems().sortedWith(compareBy({ if (Catalog.IsSeen(it)) 0 else 1 }, { it.cls.simpleName }))
                    .map { Entry.EffectEntry(it.cls) }

    /**
     * The perk tab is a grid of icons, laid out like the hero's own perk page. A
     * perk carries no name, so a row of text has nothing to show; the cells are
     * plain components rather than buttons, as a button would swallow the drag
     * and the grid is taller than the window.
     */
    private fun showPerkGrid() {
        entries.clear()
        perkCells.clear()

        val content = list.content()
        content.clear()
        list.scrollTo(0f, 0f)

        // the hero's own perk page: four columns of 20px slots, two pixels apart
        val columns = 4
        val cellSize = 20f
        val gap = 2f

        val order = Catalog.PerkOrder.sortedBy { if (Catalog.IsSeen(it)) 0 else 1 }
        val pitch = cellSize + gap
        val left = (width - (columns * cellSize + (columns - 1) * gap)) / 2f

        for ((i, cls) in order.withIndex()) {
            val cell = PerkCell(cls)
            cell.setRect(left + (i % columns) * pitch, (i / columns) * pitch, cellSize, cellSize)
            content.add(cell)
            perkCells.add(cell)
        }

        content.setSize(width.toFloat(), ((order.size + columns - 1) / columns) * pitch)
        list.setSize(list.width(), list.height())
    }

    /** one cell of the perk grid: the perk's icon, or a placeholder while unseen */
    private class PerkCell(private val cls: Class<*>) : Component() {

        private val revealed = Catalog.IsSeen(cls)
        private val icon: Visual =
                if (revealed) CatalogInfo.perkIcon(cls)
                else ItemSprite(ItemSpriteSheet.SOMETHING, null)

        init {
            add(icon)
        }

        override fun layout() {
            icon.x = x + (width - icon.width()) / 2f
            icon.y = y + (height - icon.height()) / 2f
            PixelScene.align(icon)
        }

        fun onClick(x: Float, y: Float): Boolean {
            if (inside(x, y)) {
                if (revealed) CatalogUI.show(WndCatalogPerk(cls))
                return true
            }
            return false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun castClass(cls: Class<*>): Class<out Item> = cls as Class<out Item>

    private fun showList(entryList: List<Entry>) {
        entries.clear()

        val content = list.content()
        content.clear()
        list.scrollTo(0f, 0f)

        var pos = 0f
        for (entry in entryList) {
            val item = ListItem(entry)
            item.setRect(0f, pos, width.toFloat(), ITEM_HEIGHT)
            content.add(item)
            entries.add(item)

            pos += item.height()
        }

        content.setSize(width.toFloat(), pos)
        list.setSize(list.width(), list.height())
    }

    private sealed class Entry {
        class ItemEntry(val cls: Class<out Item>, val reinforced: Boolean = false) : Entry()
        class MobEntry(val key: CatalogKey) : Entry()
        class EffectEntry(val cls: Class<*>) : Entry()
    }

    private class ListItem(val entry: Entry) : Component() {

        private var revealed = false
        private var name = ""

        private var item: Item? = null
        private var mob: Mob? = null

        private lateinit var sprite: Image
        private val label: RenderedTextMultiline
        private val line: ColorBlock

        init {
            // children are built here instead of in createChildren(), as the
            // entry is not assigned yet when the super constructor runs
            try {
                when (entry) {
                    is Entry.ItemEntry -> initItem(entry.cls, entry.reinforced)
                    is Entry.MobEntry -> initMob(entry.key)
                    is Entry.EffectEntry -> initEffect(entry.cls)
                }
            } catch (e: Exception) {
                // a single bad entry should not break the whole catalog
                Log.w("dpd", "catalog entry failed: " + e)
                revealed = false
                name = ""
                sprite = ItemSprite(ItemSpriteSheet.SOMETHING, null)
            }

            add(sprite)

            label = PixelScene.renderMultiline(7)
            if (name.isEmpty()) {
                label.text(M.L(WndCatalogs::class.java, "unknown"))
                label.hardlight(0xcccccc)
            } else label.text(name)
            add(label)

            line = ColorBlock(1f, 1f, -0xddddde)
            add(line)
        }

        private fun initItem(cls: Class<out Item>, reinforced: Boolean) {
            val item = cls.newInstance()
            this.item = item

            val sprite = ItemSprite()
            this.sprite = sprite

            when {
                item is Potion -> {
                    // potions are listed by their appearance; the effect text sits
                    // behind the catalog's own spoiler gate
                    revealed = true
                    if (reinforced) item.reinforce()
                    name = item.trueName() + (item.status() ?: "")
                    sprite.view(CatalogAppearance.image(cls) ?: item.image(), null)
                }
                item is Scroll -> {
                    // same for scrolls, their rune is visible from the start
                    revealed = true
                    name = item.trueName()
                    sprite.view(CatalogAppearance.image(cls) ?: item.image(), null)
                }
                item is Ring -> {
                    // and for rings: the gem is the identifying look, so the tab
                    // shows every ring by its catalog gem, named and all
                    revealed = true
                    name = item.trueName()
                    sprite.view(CatalogAppearance.image(cls) ?: item.image(), null)
                }
                item is Wine -> {
                    revealed = Catalog.IsSeen(cls)
                    name = if (revealed) item.trueName() else ""
                    sprite.view(if (revealed) item.image() else ItemSpriteSheet.SOMETHING, null)
                }
                item is Armor || item is Weapon -> {
                    // armor and weapons are catalogued as soon as the hero has
                    // held one, whether or not its level and curse are known
                    revealed = Catalog.IsSeen(cls)
                    name = if (revealed) item.trueName() else ""
                    sprite.view(if (revealed) item.image() else ItemSpriteSheet.SOMETHING, null)
                }
                else -> {
                    // rings are handled above; the rest are listed by name only
                    // once they are known to the hero
                    revealed = if (item is Artifact) Catalog.IsSeen(cls) else item.isIdentified
                    name = if (item.isIdentified || Catalog.IsSeen(cls)) item.trueName() else ""
                    sprite.view(
                            if (item.isIdentified || Catalog.IsSeen(cls)) item.image()
                            else ItemSpriteSheet.SOMETHING, null)
                }
            }
        }

        private fun initMob(key: CatalogKey) {
            revealed = Catalog.IsSeen(key)
            name = if (revealed) M.L(key.cls, "name") else ""
            if (!revealed) {
                sprite = ItemSprite(ItemSpriteSheet.SOMETHING, null)
                return
            }

            try {
                val mob = Catalog.instantiate(key)
                this.mob = mob
                name = mob.name
                sprite = mob.sprite()
            } catch (e: Exception) {
                // a mob that cannot be built without a run is expected: keep the
                // entry as name plus prose, and keep the log to one line. In a
                // run the same failure is a bug, so keep the stack trace then.
                if (Dungeon.isHeroNull) Log.w("dpd", "catalog mob unavailable: " + key.cls.simpleName)
                else Log.e("dpd", "catalog mob failed: " + key.cls.simpleName, e)
                sprite = ItemSprite(ItemSpriteSheet.SOMETHING, null)
            }
        }

        private fun initEffect(cls: Class<*>) {
            revealed = Catalog.IsSeen(cls)
            name = if (revealed) CatalogInfo.effectName(cls) else ""

            sprite = if (revealed) ItemSprite(CatalogInfo.effectIcon(cls), CatalogInfo.effectGlowing(cls))
            else ItemSprite(ItemSpriteSheet.SOMETHING, null)
        }

        override fun layout() {
            sprite.y = y + 1f + (height - 1f - sprite.height) / 2f
            PixelScene.align(sprite)

            line.size(width, 1f)
            line.x = 0f
            line.y = y

            label.maxWidth((width - sprite.width - 1f).toInt())
            label.setPos(sprite.x + sprite.width + 1f, y + 1f + (height - 1f - label.height()) / 2f)
            PixelScene.align(label)
        }

        /** potions and scrolls only open once their catalog text exists */
        private fun canOpen(): Boolean {
            val itemEntry = entry as? Entry.ItemEntry ?: return true
            if (item !is Potion && item !is Scroll) return true
            return Messages.has(itemEntry.cls, "catalog_desc")
        }

        fun onClick(x: Float, y: Float): Boolean {
            if (inside(x, y)) {
                if (revealed && canOpen()) when (entry) {
                    is Entry.MobEntry -> {
                        val mob = this.mob
                        if (mob != null) CatalogUI.show(WndCatalogMob(mob, entry.key.variant))
                        else CatalogUI.show(WndCatalogText(entry.key.cls))
                    }
                    is Entry.EffectEntry -> CatalogUI.show(WndCatalogEffect(entry.cls))
                    is Entry.ItemEntry -> CatalogUI.show(WndCatalogItem(item!!))
                }
                return true
            } else {
                return false
            }
        }
    }

    companion object {
        private const val WIDTH_P = 120
        private const val WIDTH_L = 144

        private const val HEIGHT = 141

        private const val ITEM_HEIGHT = 17f

        private const val POTIONS_TAB = 0
        private const val MOBS_TAB = 5
        private const val ARMOR_TAB = 7
        private const val WEAPON_TAB = 8
        private const val PERK_TAB = 9

        /** ten tabs share the window width, so every tab icon is drawn smaller */
        private const val TAB_ICON_SCALE = 0.75f

        /** label keys of the monster tab's pages: five acts, then the depth-independent mobs */
        private val MOB_PAGES = arrayOf("act1", "act2", "act3", "act4", "act5", "universal")

        /**
         * The sub-pages of every tab that has them, as label keys: the potion
         * flavours, the mob pages, and the armor and weapon breakdowns. A tab
         * missing from here shows its title in the header instead.
         */
        private val SECTIONS = linkedMapOf(
                POTIONS_TAB to listOf("normal", "reinforced", "reagents"),
                MOBS_TAB to MOB_PAGES.toList(),
                ARMOR_TAB to listOf("desc", "glyphs", "curses"),
                WEAPON_TAB to listOf("desc", "enchantments", "inscriptions", "curses")
        )

        /** colour of the sub-page buttons that are not the active one */
        private const val SECTION_DIM = 0x888888

        /** how far a sub-page label may overrun its button before its row wraps */
        private const val MAX_LABEL_SPILL = 1.3f

        private val TABS = arrayOf(
                "potions", "scrolls", "rings", "artifacts", "food",
                "mobs", "npcs", "armor", "weapons", "perks"
        )

        private var CurrentTab = 0
    }
}
