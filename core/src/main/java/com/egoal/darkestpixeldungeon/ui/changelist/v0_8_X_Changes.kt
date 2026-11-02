package com.egoal.darkestpixeldungeon.ui.changelist

import com.egoal.darkestpixeldungeon.actors.buffs.Pressure
import com.egoal.darkestpixeldungeon.actors.buffs.Vulnerable
import com.egoal.darkestpixeldungeon.actors.mobs.npcs.Yvette
import com.egoal.darkestpixeldungeon.effects.FloatingText
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.egoal.darkestpixeldungeon.ui.Icons
import com.watabou.noosa.Image
import com.egoal.darkestpixeldungeon.sprites.RotHeartSprite

object v0_8_X_Changes {

    const val VERSION = "0.8.1b"

    /**
     * Adds every release to [list] below [top], within [width], and returns the
     * y position under the last one.
     */
    fun addAll(list: ChangesScrollPane, width: Float, top: Float): Float {
        var y = top
        for (section in sections()) {
            section.setRect(0f, y, width, 0f)
            list.content().add(section)
            list.addInfo(section)
            y = section.bottom()
        }
        return y
    }

    private fun sections(): List<ChangeInfo> = listOf(
            header("0.8.1b"),

            section("updates",
                    entry("0.8.1b", "update_check", ChangeIcons.icon(Icons.INFO)),
                    entry("0.8.1b", "epitaph", ChangeIcons.item(ItemSpriteSheet.GRAVE)),
                    entry("0.8.1b", "spirit_record", ChangeIcons.icon(Icons.NETWORK)),
                    entry("0.8.1b", "hunter_portrait", ChangeIcons.icon(Icons.HUNTRESS))),

            section("adjustments",
                    entry("0.8.1b", "hunger", ChangeIcons.item(ItemSpriteSheet.RATION)),
                    entry("0.8.1b", "luck", ChangeIcons.icon(Icons.CLOVER)),
                    entry("0.8.1b", "lucky_coin", ChangeIcons.item(ItemSpriteSheet.LUCKY_COIN)),
                    entry("0.8.1b", "rogue_armor", ChangeIcons.icon(Icons.ROGUE)),
                    entry("0.8.1b", "artifact", ChangeIcons.item(ItemSpriteSheet.ARTIFACT_HORN4)),
                    entry("0.8.1b", "vulnerable", ChangeIcons.buff(Vulnerable())),
                    entry("0.8.1b", "amulet_return", ChangeIcons.item(ItemSpriteSheet.AMULET))),

            section("ui",
                    entry("0.8.1b", "ranking", ChangeIcons.icon(Icons.RANKINGS)),
                    entry("0.8.1b", "container", ChangeIcons.icon(Icons.BACKPACK)),
                    entry("0.8.1b", "catalog", ChangeIcons.item(ItemSpriteSheet.DPD_BOOKS))),

            section("fixes",
                    entry("0.8.1b", "spellbook", ChangeIcons.item(ItemSpriteSheet.ARTIFACT_SPELLBOOK)),
                    entry("0.8.1b", "rot_heart", RotHeartSprite()),
                    entry("0.8.1b", "torch", ChangeIcons.item(ItemSpriteSheet.TORCH)),
                    entry("0.8.1b", "text", ChangeIcons.icon(Icons.NOTES)),
                    entry("0.8.1b", "yvette", Yvette.Sprite())),

            section("others",
                    entry("0.8.1b", "server", ChangeIcons.icon(Icons.SERVER))),

            header("0.8.1a"),

            section("updates",
                    entry("0.8.1a", "codex", ChangeIcons.item(ItemSpriteSheet.DPD_BOOKS)),
                    entry("0.8.1a", "luck", ChangeIcons.item(ItemSpriteSheet.LUCKY_COIN)),
                    entry("0.8.1a", "pressure", ChangeIcons.buff(Pressure())),
                    entry("0.8.1a", "madness_mask", ChangeIcons.item(ItemSpriteSheet.MASK_OF_MADNESS))),

            section("adjustments",
                    entry("0.8.1a", "hunger", ChangeIcons.item(ItemSpriteSheet.RATION)),
                    entry("0.8.1a", "diet", ChangeIcons.icon(Icons.PERK)),
                    entry("0.8.1a", "fast_regen", ChangeIcons.item(ItemSpriteSheet.POTION_CRIMSON)),
                    entry("0.8.1a", "elder_hand", ChangeIcons.item(ItemSpriteSheet.BONE_HAND)),
                    entry("0.8.1a", "vulnerable", ChangeIcons.buff(Vulnerable())),
                    entry("0.8.1a", "teleport", ChangeIcons.icon(Icons.COMPASS)),
                    entry("0.8.1a", "latin_font", ChangeIcons.icon(Icons.LANGS)),
                    entry("0.8.1a", "kusarigama", ChangeIcons.item(ItemSpriteSheet.KUSARIGAMA)),
                    entry("0.8.1a", "longest_spear", ChangeIcons.item(ItemSpriteSheet.LONGEST_SPEAR)),
                    entry("0.8.1a", "carvedstaff", ChangeIcons.item(ItemSpriteSheet.CARVED_STAFF))),

            section("ui",
                    entry("0.8.1a", "damage_icons", ChangeIcons.textIcon(FloatingText.PHYS_DMG)),
                    entry("0.8.1a", "turn_disc", ChangeIcons.icon(Icons.BUSY)),
                    entry("0.8.1a", "bag_drop", ChangeIcons.icon(Icons.BACKPACK)),
                    entry("0.8.1a", "desktop_keys", ChangeIcons.icon(Icons.KEYBOARD)),
                    entry("0.8.1a", "spirit_sync", ChangeIcons.icon(Icons.NETWORK)),
                    entry("0.8.1a", "style_text", ChangeIcons.icon(Icons.NOTES))),

            section("fixes",
                    entry("0.8.1a", "ring_identify", ChangeIcons.item(ItemSpriteSheet.RING_OPAL)),
                    entry("0.8.1a", "wizard_hat", ChangeIcons.item(ItemSpriteSheet.WIZARD_HAT)),
                    entry("0.8.1a", "mead_wine", ChangeIcons.item(ItemSpriteSheet.MEAD_WINE)),
                    entry("0.8.1a", "zero_damage", ChangeIcons.item(ItemSpriteSheet.WAND_CORRUPTION)),
                    entry("0.8.1a", "headgear", ChangeIcons.item(ItemSpriteSheet.HEADGEAR)),
                    entry("0.8.1a", "wine_stat", ChangeIcons.item(ItemSpriteSheet.DPD_WINE)),
                    entry("0.8.1a", "protected_drops", ChangeIcons.item(ItemSpriteSheet.CHEST)),
                    entry("0.8.1a", "sacrificial_fire", ChangeIcons.item(ItemSpriteSheet.CANDLE))),


            header("0.8.0c"),

            section("fixes", bugfix("0.8.0c")),

            header("0.8.0b"),

            section("updates",
                    entry("0.8.0b", "bouquet", ChangeIcons.item(ItemSpriteSheet.PETAL)),
                    entry("0.8.0b", "spirit_dupe", ChangeIcons.icon(Icons.SKULL)),
                    entry("0.8.0b", "online_sync", ChangeIcons.icon(Icons.QQ))),

            section("fixes", bugfix("0.8.0b")),

            section("others",
                    entry("0.8.0b", "crash_report", ChangeIcons.icon(Icons.WARNING)),
                    entry("0.8.0b", "demon_statue", ChangeIcons.item(ItemSpriteSheet.DEMONIC_SKULL))),

            header("0.8.0a"),

            section("fixes", bugfix("0.8.0a")),

            section("others",
                    entry("0.8.0a", "android", ChangeIcons.icon(Icons.SHPX)),
                    entry("0.8.0a", "about", ChangeIcons.icon(Icons.INFO)),
                    entry("0.8.0a", "warrior", ChangeIcons.icon(Icons.WARRIOR))),

            header("0.8.0"),

            section("updates",
                    entry("0.8.0", "red_spirit", ChangeIcons.icon(Icons.SKULL)),
                    entry("0.8.0", "spirit_loot", ChangeIcons.item(ItemSpriteSheet.WORN_SHORTSWORD)),
                    entry("0.8.0", "desktop", ChangeIcons.icon(Icons.WATA)),
                    entry("0.8.0", "bouquet", ChangeIcons.item(ItemSpriteSheet.PETAL))),

            section("adjustments",
                    entry("0.8.0", "tengu_katana", ChangeIcons.item(ItemSpriteSheet.KATANA)),
                    entry("0.8.0", "light_map", ChangeIcons.item(ItemSpriteSheet.TORCH)),
                    entry("0.8.0", "travel", ChangeIcons.icon(Icons.ALERT)),
                    entry("0.8.0", "boethiahs_blade", ChangeIcons.item(ItemSpriteSheet.BOETHIAHS_BLADE)),
                    entry("0.8.0", "crush", ChangeIcons.icon(Icons.TARGET))),

            section("fixes", bugfix("0.8.0")),

            section("others",
                    entry("0.8.0", "sentry", ChangeIcons.icon(Icons.WARNING))))

    private fun header(version: String): ChangeInfo = ChangeInfo(text("v$version"), true, null)

    private fun section(kind: String, vararg buttons: ChangeButton): ChangeInfo =
            ChangeInfo(text("section.$kind"), false, null).apply {
                buttons.forEach { addButton(it) }
            }

    /** every fix of a release sits behind one squashed spider */
    private fun bugfix(version: String): ChangeButton =
            entry(version, "bugfix", ChangeIcons.bugfix())

    private fun entry(version: String, key: String, icon: Image): ChangeButton =
            ChangeButton(icon, text("v$version.$key.title"), text("v$version.$key.text"))

    private fun text(key: String): String = M.L("scenes.changesscene.$key")
}
