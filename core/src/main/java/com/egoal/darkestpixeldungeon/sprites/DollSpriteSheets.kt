package com.egoal.darkestpixeldungeon.sprites

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.items.KindOfWeapon
import com.egoal.darkestpixeldungeon.items.weapon.melee.*

object DollSpriteSheets {
    const val BODY_ASSETS = Assets.HERO_BODY
    const val HEAD_ASSETS = Assets.HERO_HEAD
    const val ARMOR_ASSETS = Assets.HERO_ARMOR
    const val HELMET_ASSETS = Assets.HERO_HELMET
    const val WEAPON_ASSETS = Assets.HERO_WEAPONS

    object Body {
        const val BASE = 0
    }

    object Head {
        const val WARRIOR = 0
        const val MAGE = 1
        const val ROGUE = 2
        const val HUNTRESS = 3
        const val SORCERESS = 4
        const val EXILE = 5
    }

    object Armor {
        const val UNDERWARE_MALE = 0
        const val UNDERWARE_FEMALE = 1
        const val CLOTH_ARMOR = 2
        const val LEATHER_ARMOR = 3
        const val MAIL_ARMOR = 4
        const val SCALE_ARMOR = 5
        const val PLATE_ARMOR = 6

        // class armor (tier 6), one row per HeroClass.ordinal
        const val CLASS_ARMOR = 7
    }

    // weapon rows in weapons.png (from NNYPD); -1 means not drawn
    // WeaponSprite ids: https://github.com/ravenwolf/No-Name-Yet-Pixel-Dungeon (GPLv3)
    const val WEAP_DAGGER = 0
    const val WEAP_ASSASSIN_BLADE = 1
    const val WEAP_GLADIUS = 5
    const val WEAP_SWORD = 6
    const val WEAP_KATANA = 7
    const val WEAP_SCIMITAR = 8
    const val WEAP_MAGE_STAFF = 10
    const val WEAP_MACE = 11
    const val WEAP_HAND_AXE = 12
    const val WEAP_BATTLE_AXE = 13
    const val WEAP_SPEAR = 15
    const val WEAP_GLAIVE = 16
    const val WEAP_HALBERD = 17
    const val WEAP_GREAT_AXE = 18
    const val WEAP_WARHAMMER = 19
    const val WEAP_GREATSWORD = 20
    const val WEAP_FLAIL = 21
    const val WEAP_DOUBLE_BLADE = 23

    sealed class WeaponVisual {
        object None : WeaponVisual()
        data class Legacy(val row: Int) : WeaponVisual()
        data class Custom(val name: String) : WeaponVisual()
    }

    /**
     * Maps a weapon to either a custom sheet under `actors/hero/weapons/<name>.png+.json`
     * or a legacy NNYPD row in `weapons.png`.
     */
    fun weaponVisual(weapon: KindOfWeapon?): WeaponVisual {
        val custom = customWeaponName(weapon)
        if (custom != null) return WeaponVisual.Custom(custom)
        val row = weaponDrawId(weapon)
        return if (row >= 0) WeaponVisual.Legacy(row) else WeaponVisual.None
    }

    // Add custom weapon sheet names here (matching assets/actors/hero/weapons/<name>.png + .json)
    fun customWeaponName(weapon: KindOfWeapon?): String? = when (weapon) {
        else -> null
    }

    fun weaponDrawId(weapon: KindOfWeapon?): Int = when (weapon) {
        is Dagger, is ParryingDagger -> WEAP_DAGGER
        is AssassinsBlade -> WEAP_ASSASSIN_BLADE
        is ShortSword, is WornShortsword, is ButchersKnife -> WEAP_GLADIUS
        is Sword, is Longsword -> WEAP_SWORD
        is TengusKatana -> WEAP_KATANA
        is Scimitar, is Tulwar -> WEAP_SCIMITAR
        is MagesStaff, is CarvedStaff, is Quarterstaff -> WEAP_MAGE_STAFF
        is Mace -> WEAP_MACE
        is HandAxe -> WEAP_HAND_AXE
        is BattleAxe -> WEAP_BATTLE_AXE
        is Spear, is ShortSpear, is LongSpear -> WEAP_SPEAR
        is Glaive -> WEAP_GLAIVE
        is Halberd -> WEAP_HALBERD
        is Greataxe -> WEAP_GREAT_AXE
        is WarHammer -> WEAP_WARHAMMER
        is Claymore -> WEAP_GREATSWORD
        is Flail -> WEAP_FLAIL
        is PairSwords -> WEAP_DOUBLE_BLADE
        else -> -1
    }
}
