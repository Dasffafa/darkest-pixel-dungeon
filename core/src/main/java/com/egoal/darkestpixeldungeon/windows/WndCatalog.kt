package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.perks.Perk
import com.egoal.darkestpixeldungeon.actors.hero.perks.PerkImageSheet
import com.egoal.darkestpixeldungeon.actors.mobs.Mob
import com.egoal.darkestpixeldungeon.actors.mobs.npcs.NPC
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.CatalogAppearance
import com.egoal.darkestpixeldungeon.items.armor.Armor
import com.egoal.darkestpixeldungeon.items.potions.Potion
import com.egoal.darkestpixeldungeon.items.weapon.Enchantment
import com.egoal.darkestpixeldungeon.items.weapon.Inscription
import com.egoal.darkestpixeldungeon.items.weapon.Weapon
import com.egoal.darkestpixeldungeon.items.weapon.melee.MeleeWeapon
import com.egoal.darkestpixeldungeon.items.weapon.missiles.MissileWeapon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Image
import com.watabou.noosa.TextureFilm
import java.util.Locale

/**
 * Shared helpers for the catalog windows: the richer `catalog_desc` text is
 * preferred over the regular `desc`, and mob stats are rendered from the live
 * mob config so the numbers can never drift from the game.
 */
object CatalogInfo {

    /**
     * The catalog prose of a class, or of one variant of it: a class with several
     * looks keeps one "catalog_desc_<variant>" text per look, falling back to the
     * in-game "desc_<variant>" prose when no catalog text exists.
     */
    fun text(cls: Class<*>, variant: String? = null): String = when {
        variant != null && Messages.has(cls, "catalog_desc_$variant") -> M.L(cls, "catalog_desc_$variant")
        variant != null && Messages.has(cls, "desc_$variant") -> M.L(cls, "desc_$variant")
        Messages.has(cls, "catalog_desc") -> M.L(cls, "catalog_desc")
        else -> M.L(cls, "desc")
    }

    /**
     * The full text of a mob catalog entry. An NPC has no meaningful combat data
     * (it never attacks and is not meant to be fought), so its entry stays prose
     * only; a monster gets its stats appended.
     */
    fun mobText(mob: Mob, variant: String? = null): String {
        val text = text(mob.javaClass, variant)
        return if (mob is NPC) text else text + "\n\n" + mobStats(mob)
    }

    fun mobStats(mob: Mob): String {
        val cfg = mob.Config

        val lines = ArrayList<String>()
        lines.add(M.L(CatalogInfo::class.java, "stat_vital", cfg.MaxHealth, cfg.Shield))
        lines.add(M.L(CatalogInfo::class.java, "stat_damage",
                cfg.MinDamage, cfg.MaxDamage, damageType(cfg.TypeDamage)))
        lines.add(M.L(CatalogInfo::class.java, "stat_combat",
                cfg.AttackSkill, cfg.DefendSkill, cfg.MinDefend, cfg.MaxDefend))
        lines.add(M.L(CatalogInfo::class.java, "stat_crit", cfg.CritChance * 100f, cfg.CritRatio))
        lines.add(M.L(CatalogInfo::class.java, "stat_exp", cfg.EXP))

        val res = resistance(mob)
        if (res.isNotEmpty()) lines.add(M.L(CatalogInfo::class.java, "stat_resist", res))

        // a few mobs scale their stats with the current depth, which is 0 while
        // no run is loaded
        if (Dungeon.isHeroNull) lines.add(M.L(CatalogInfo::class.java, "no_run_note"))

        return lines.joinToString("\n")
    }

    /**
     * The numbers of a weapon, read off the item itself so a balance change can
     * never leave the catalog behind. Nothing here touches the hero: the catalog
     * also opens from the title screen. Damage growth is the per-level delta of
     * the weapon's own roll, which every subclass may redefine.
     */
    fun weaponStats(weapon: Weapon): String = when (weapon) {
        is MeleeWeapon -> lines(
                M.L(CatalogInfo::class.java, "weapon_stat",
                        weapon.tier, weapon.min(0), weapon.max(0),
                        weapon.min(1) - weapon.min(0), weapon.max(1) - weapon.max(0),
                        weapon.STRReq(0)),
                M.L(CatalogInfo::class.java, "weapon_mods", weapon.ACC, 1f / weapon.DLY, weapon.RCH))
        is MissileWeapon -> lines(
                M.L(CatalogInfo::class.java, "missile_stat",
                        weapon.tier, weapon.min(0), weapon.max(0), weapon.STRReq(0)),
                M.L(CatalogInfo::class.java, "missile_mods",
                        weapon.ACC, 1f / weapon.DLY, Math.round(weapon.baseBreakChance() * 100f)))
        else -> ""
    }

    /** the numbers of an armor, at +0 and at the two levels worth comparing */
    fun armorStats(armor: Armor): String = lines(
            M.L(CatalogInfo::class.java, "armor_stat", armor.tier,
                    armor.DRMin(0), armor.DRMax(0), armor.SHLD(0), percent(armor.MRES(0)), armor.STRReq(0)),
            growth(armor, 5),
            growth(armor, 10))

    private fun growth(armor: Armor, level: Int): String = M.L(CatalogInfo::class.java, "armor_growth", level,
            armor.DRMin(level), armor.DRMax(level), armor.SHLD(level), percent(armor.MRES(level)))

    private fun percent(value: Float): Float = value * 100f

    /**
     * The full catalog text of an armor or a weapon: the in-game prose, then the
     * numbers above, then the hand-written mechanics of that one item.
     */
    fun equipmentText(item: Item, stats: String): String {
        val parts = ArrayList<String>()
        parts.add(M.L(item.javaClass, "desc"))
        parts.add(stats)
        if (Messages.has(item.javaClass, "catalog_desc")) parts.add(M.L(item.javaClass, "catalog_desc"))
        return parts.joinToString("\n\n")
    }

    private fun lines(vararg lines: String): String = lines.joinToString("\n")

    /** the perk sheet, one 16x16 cell per perk */
    val perkFilm = TextureFilm(TextureCache.get(Assets.PERKS), 16, 16)

    fun perkIcon(cls: Class<*>): Image =
            Image(TextureCache.get(Assets.PERKS)).apply { frame(perkFilm.get(perk(cls).image())) }

    /** perks have no name text, so the level cap opens the body instead of a title */
    fun perkText(cls: Class<*>): String {
        val perk = perk(cls)
        val body = if (Messages.has(cls, "catalog_desc")) M.L(cls, "catalog_desc") else perk.description()
        return M.L(CatalogInfo::class.java, "perk_level", perk.maxLevel) + "\n\n" + body
    }

    private fun perk(cls: Class<*>): Perk = cls.newInstance() as Perk

    fun effectIcon(cls: Class<*>): Int = when (cls.newInstance()) {
        is Armor.Glyph -> ItemSpriteSheet.ARMOR_PLATE
        else -> ItemSpriteSheet.WORN_SHORTSWORD
    }

    fun effectGlowing(cls: Class<*>): ItemSprite.Glowing? = when (val effect = cls.newInstance()) {
        is Armor.Glyph -> effect.glowing()
        is Enchantment -> effect.glowing()
        else -> null
    }

    fun effectName(cls: Class<*>): String = when (val effect = cls.newInstance()) {
        is Armor.Glyph -> effect.name()
        is Enchantment -> effect.name()
        is Inscription -> effect.name()
        else -> M.L(cls, "name")
    }

    private fun damageType(type: Int): String = M.L(CatalogInfo::class.java, when (type) {
        1 -> "dmg_magical"
        2 -> "dmg_mental"
        else -> "dmg_normal"
    })

    private fun resistance(mob: Mob): String {
        // all resistance values live on the first line of the config list
        val r = mob.Config.Resistance.getOrNull(0) ?: return ""
        val entries = listOf(
                M.L(CatalogInfo::class.java, "res_magic") to r.Magic,
                M.L(CatalogInfo::class.java, "res_fire") to r.Fire,
                M.L(CatalogInfo::class.java, "res_poison") to r.Poison,
                M.L(CatalogInfo::class.java, "res_ice") to r.Ice,
                M.L(CatalogInfo::class.java, "res_light") to r.Light,
                M.L(CatalogInfo::class.java, "res_shadow") to r.Shadow,
                M.L(CatalogInfo::class.java, "res_holy") to r.Holy
        )

        return entries.filter { it.second != 0f }.joinToString("　") {
            String.format(Locale.ENGLISH, "%s %+.0f%%", it.first, it.second * 100f)
        }
    }
}

/** detail window for a mob or NPC catalog entry, or for one variant of it */
class WndCatalogMob(private val mob: Mob, variant: String? = null) : WndTitledMessage(WndInfoMob.MobTitle(mob),
        CatalogInfo.mobText(mob, variant))

/** prose only entry, for mobs that cannot be built while no run is loaded */
class WndCatalogText(cls: Class<*>) : WndTitledMessage(
        ItemSprite(ItemSpriteSheet.SOMETHING, null), M.L(cls, "name"), CatalogInfo.text(cls))

/** detail window for a glyph, an enchantment or a curse */
class WndCatalogEffect(cls: Class<*>) : WndTitledMessage(
        ItemSprite(CatalogInfo.effectIcon(cls), CatalogInfo.effectGlowing(cls)),
        CatalogInfo.effectName(cls), CatalogInfo.text(cls))

/** detail window for a perk: its icon and its text, which opens with the level cap */
class WndCatalogPerk(cls: Class<*>) : WndTitledMessage(
        CatalogInfo.perkIcon(cls), "", CatalogInfo.perkText(cls))

/**
 * Detail window for a catalog item entry. An armor or weapon adds its live
 * numbers and its own mechanics to the plain in-game prose. A normal potion
 * shows only its base text, a reinforced one only its reinforced text: the two
 * never mix, so a reinforced entry cannot be read as the plain potion (or the
 * reverse). A ring or artifact shows the catalog's quantitative text, and keeps
 * its lore in the inventory's own description instead.
 */
class WndCatalogItem(private val item: Item) : WndTitledMessage(
        ItemSprite(CatalogAppearance.image(item.javaClass) ?: item.image(), null),
        item.trueName(), text(item)) {

    companion object {
        private fun text(item: Item): String = when {
            item is Armor -> CatalogInfo.equipmentText(item, CatalogInfo.armorStats(item))
            item is Weapon -> CatalogInfo.equipmentText(item, CatalogInfo.weaponStats(item))
            item is Potion && item.reinforced ->
                M.L(WndCatalogItem::class.java, "reinforced") + "\n" + reinforcedText(item)
            // the catalog is also opened outside a run, so rings and artifacts
            // must not go through info(): it asks the hero which item is worn
            Messages.has(item.javaClass, "catalog_desc") -> CatalogInfo.text(item.javaClass)
            item is Potion -> item.info()
            else -> CatalogInfo.text(item.javaClass)
        }

        /**
         * Prefers the quantitative catalog text. The in-game [reinforced_desc] is
         * a prose fallback only, so a reinforced entry never shows the base text.
         */
        private fun reinforcedText(item: Potion): String = when {
            Messages.has(item.javaClass, "reinforced_catalog_desc") ->
                M.L(item.javaClass, "reinforced_catalog_desc")
            Messages.has(item.javaClass, "reinforced_desc") -> M.L(item.javaClass, "reinforced_desc")
            else -> M.L(item.javaClass, "desc")
        }
    }
}
