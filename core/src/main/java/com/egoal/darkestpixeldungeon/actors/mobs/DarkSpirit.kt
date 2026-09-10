package com.egoal.darkestpixeldungeon.actors.mobs

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.GamesInProgress
import com.egoal.darkestpixeldungeon.Statistics
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.actors.buffs.Mending
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.egoal.darkestpixeldungeon.actors.hero.perks.Perk
import com.egoal.darkestpixeldungeon.effects.CellEmitter
import com.egoal.darkestpixeldungeon.effects.PerkGain
import com.egoal.darkestpixeldungeon.effects.Speck
import com.egoal.darkestpixeldungeon.items.Generator
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.armor.Armor
import com.egoal.darkestpixeldungeon.items.potions.PotionOfHealing
import com.egoal.darkestpixeldungeon.items.weapon.Inscription
import com.egoal.darkestpixeldungeon.items.weapon.Weapon
import com.egoal.darkestpixeldungeon.items.weapon.melee.MeleeWeapon
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.egoal.darkestpixeldungeon.sprites.HeroSprite
import com.egoal.darkestpixeldungeon.sprites.MobSprite
import com.watabou.noosa.TextureFilm
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.FileUtils
import com.watabou.utils.Random
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DarkSpirit : Mob() {
    private var potions = 0
    private var potionCD = 0

    private var spiritClass = HeroClass.ROGUE
    private var spiritPerk: Perk? = null
    private var spiritName = DEFAULT_NAME
    private var spiritLevel = 1
    private var spiritArmor: Armor? = null
    private var spiritWeapon: Weapon? = null
    private var spiritStrength = SPIRIT_STRENGTH_DEEP

    init {
        applySpirit()

        potions = Random.IntRange(2, 3)

        state = WANDERING

        properties.add(Property.UNDEAD)
    }

    /** adopted from the dead hero this spirit is a record of */
    internal fun initFrom(record: SpiritRecord) {
        spiritClass = record.heroClass
        spiritPerk = record.perk
        spiritName = record.userName
        spiritLevel = record.level
        spiritArmor = record.armor
        spiritWeapon = record.weapon
        spiritStrength = strengthFor(record.depth)

        applySpirit()
    }

    private fun applySpirit() {
        val weapon = spiritWeapon

        Config = Config.copy(
                MaxHealth = 20 + spiritLevel * 4,
                AttackSkill = 10f + spiritLevel,
                DefendSkill = 5f + spiritLevel,
                MinDamage = weapon?.min() ?: Config.MinDamage,
                MaxDamage = weapon?.max() ?: Config.MaxDamage)

        name = spiritName
    }

    /** how far the carried weapon's strength requirement outruns the spirit's own strength */
    private fun encumbrance(weapon: Weapon): Int = max(0, weapon.STRReq() - spiritStrength)

    /** frost cracks open 1-2 of the potions it carries, right where it stands */
    internal fun shatterPotions() {
        repeat(min(potions, Random.IntRange(1, 2))) {
            potions -= 1
            PotionOfHealing().shatter(pos)
        }
    }

    override fun act(): Boolean {
        HP = min(HP + 1, HT)
        potionCD -= 1
        val ratio = HP.toFloat() / HT
        if (ratio <= 0.6f && potions > 0 && potionCD <= 0 && Random.Float() < (2f * (0.6 - ratio))) {
            potions -= 1
            potionCD = 5

            Sample.INSTANCE.play(Assets.SND_DRINK)

            val value = HT / 3
            recoverHP(value)
            Buff.affect(this, Mending::class.java).set(value)

            spend(TICK)
            return true
        }

        return super.act()
    }

    override fun attackProc(dmg: Damage): Damage {
        val weapon = spiritWeapon ?: return super.attackProc(dmg)

        return weapon.proc(super.attackProc(dmg))
    }

    override fun accRoll(damage: Damage): Float {
        val weapon = spiritWeapon ?: return super.accRoll(damage)

        return super.accRoll(damage) * weapon.ACC / 1.5f.pow(encumbrance(weapon))
    }

    override fun attackSpeed(): Float {
        val weapon = spiritWeapon ?: return super.attackSpeed()

        return super.attackSpeed() / (weapon.DLY * 1.2f.pow(encumbrance(weapon)))
    }

    override fun canAttack(enemy: Char): Boolean {
        val weapon = spiritWeapon ?: return super.canAttack(enemy)

        return Dungeon.level.distance(pos, enemy.pos) <= weapon.RCH
    }

    override fun defendDamage(dmg: Damage): Damage {
        if (spiritArmor != null) {
            spiritArmor!!.glyph?.proc(spiritArmor!!, dmg)
            dmg.value -= Random.IntRange(spiritArmor!!.DRMin(), spiritArmor!!.DRMax())
        }

        return super.defendDamage(dmg)
    }

    override fun createLoot(): Item? {
        if (potions > 0) return PotionOfHealing()

        return super.createLoot()
    }

    override fun description(): String = M.L(this, "desc",
            spiritWeapon?.name() ?: M.L(this, "no_weapon"),
            spiritArmor?.name() ?: M.L(this, "no_armor"),
            potions)

    override fun die(cause: Any?) {
        // however it died, its record is gone for good
        RemoveSpiritStoredInSlot(GamesInProgress.curSlot)

        // the gear it carried may be left behind
        spiritWeapon?.let { if (Random.Float() < GEAR_DROP_CHANCE) Dungeon.level.drop(it, pos).sprite.drop() }
        spiritArmor?.let { if (Random.Float() < GEAR_DROP_CHANCE) Dungeon.level.drop(it, pos).sprite.drop() }

        val perk = spiritPerk
        if (perk != null && perk.isAcquireAllowed(Dungeon.hero)) {
            Dungeon.hero.heroPerk.add(perk)
            PerkGain.Show(Dungeon.hero, perk)
        } else {
            Dungeon.hero.earnExp(Dungeon.hero.maxExp())
            CellEmitter.center(Dungeon.hero.pos).burst(Speck.factory(Speck.STAR), 8)
        }

        super.die(cause)
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)

        bundle.put(POTIONS, potions)
        bundle.put(POTION_CD, potionCD)

        bundle.put(USERNAME, spiritName)
        bundle.put(LEVEL, spiritLevel)
        bundle.put(STRENGTH, spiritStrength)
        bundle.put(PERK, spiritPerk)
        bundle.put(ARMOR, spiritArmor)
        bundle.put(WEAPON, spiritWeapon)
        spiritClass.storeInBundle(bundle)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        // level snapshots saved before the spirit carried its own identity fall back to defaults
        spiritName = bundle.getString(USERNAME).ifEmpty { DEFAULT_NAME }
        spiritLevel = bundle.getInt(LEVEL).coerceAtLeast(1)
        spiritStrength = if (bundle.contains(STRENGTH)) bundle.getInt(STRENGTH) else SPIRIT_STRENGTH_DEEP
        spiritPerk = bundle.get(PERK) as Perk?
        spiritArmor = bundle.get(ARMOR) as Armor?
        spiritWeapon = bundle.get(WEAPON) as Weapon?
        spiritClass = HeroClass.RestoreFromBundle(bundle)

        // stats follow the level, reapply before the saved HP/HT is read back
        applySpirit()

        super.restoreFromBundle(bundle)

        potions = bundle.getInt(POTIONS)
        potionCD = bundle.getInt(POTION_CD)
    }

    override fun sprite(): CharSprite = Sprite()

    inner class Sprite : MobSprite() {
        init {
            texture(spiritClass.spritesheet())
            updateArmor(spiritArmor?.tier ?: 0)
            idle()

            tint(0.3f, 0.1f, 0.1f, 0.75f)
            alpha(0.75f)
        }

        fun updateArmor(tier: Int) {
            val film = TextureFilm(HeroSprite.tiers(), tier, FRAME_WIDTH, FRAME_HEIGHT)

            idle = Animation(1, true)
            idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1)

            run = Animation(20, true)
            run.frames(film, 2, 3, 4, 5, 6, 7)

            die = Animation(20, false)
            die.frames(film, 0)

            attack = Animation(15, false)
            attack!!.frames(film, 13, 14, 15, 0)

            idle()
        }
    }

    companion object {
        private const val FRAME_WIDTH = 12
        private const val FRAME_HEIGHT = 15

        private const val DS_FILE = "darkspirit.dat"
        private const val POOL_COUNT = "count"
        private const val RECORD_PREFIX = "record"

        private const val DEPTH = "depth"
        private const val HELD_BY = "heldby"
        private const val PERK = "perk"
        private const val USERNAME = "username"
        private const val LEVEL = "level"
        private const val STRENGTH = "strength"
        private const val ARMOR = "armor"
        private const val WEAPON = "weapon"

        private const val POTIONS = "potions"
        private const val POTION_CD = "potioncd"

        private val DEFAULT_NAME get() = M.L(DarkSpirit::class.java, "nameless")

        private const val NO_SLOT = -1

        private const val MAX_UPGRADE = 3
        private const val GEAR_DROP_CHANCE = 0.3f

        /** enough for some tier-3 weapon */
        private const val SPIRIT_STRENGTH = 13

        /** spirits out of the deeper half of the range carry one more point */
        private const val SPIRIT_STRENGTH_DEEP = 14

        private val WEAPON_TIER_CHANCES = floatArrayOf(0.66f, 0.31f, 0.02f)
        private val TIER_2_WEAPON_CHANCES = floatArrayOf(0.33f, 0.62f, 0.05f)

        private fun strengthFor(depth: Int): Int = if (depth >= 6) SPIRIT_STRENGTH_DEEP else SPIRIT_STRENGTH

        /**
         * The hero died. This run is over, so the spirit this save was holding goes back
         * to the pool, and the death itself (if it qualifies) leaves a new record as well.
         */
        fun Leave() {
            val records = loadPool()

            var dirty = releaseHeld(records, GamesInProgress.curSlot)
            if (addFromDeath(records)) dirty = true

            if (dirty) savePool(records)
        }

        /** A run ended (won, died, or its save was deleted): the held spirit returns to the pool. */
        fun Release(slot: Int) {
            val records = loadPool()
            if (releaseHeld(records, slot)) savePool(records)
        }

        /** A dark spirit died, by any hand: its record is deleted for good. */
        fun RemoveSpiritStoredInSlot(slot: Int) {
            val records = loadPool()
            if (records.removeAll { it.heldBy == slot }) savePool(records)
        }

        fun Gen(): DarkSpirit? {
            if (Dungeon.IsChallenged()) return null

            val records = loadPool()
            val slot = GamesInProgress.curSlot

            // this save already holds a spirit: it can only (re)appear on its own depth
            val held = records.firstOrNull { it.heldBy == slot }
            if (held != null) {
                if (held.depth != Dungeon.depth) return null

                val spirit = DarkSpirit()
                spirit.initFrom(held)
                return spirit
            }

            val record = records.firstOrNull { it.heldBy == NO_SLOT && it.depth == Dungeon.depth } ?: return null

            record.heldBy = slot
            savePool(records)

            val spirit = DarkSpirit()
            spirit.initFrom(record)
            return spirit
        }

        private fun releaseHeld(records: MutableList<SpiritRecord>, slot: Int): Boolean {
            val held = records.firstOrNull { it.heldBy == slot } ?: return false
            held.heldBy = NO_SLOT
            return true
        }

        private fun addFromDeath(records: MutableList<SpiritRecord>): Boolean {
            if (Dungeon.depth !in 0..10 || Dungeon.bossLevel() || abs(Dungeon.depth - Dungeon.hero.lvl) > 5) return false

            // those who won, die far above their max depth, or who are challenged drop no bones.
            if (Statistics.AmuletObtained || Statistics.DeepestFloor - 5 >= Dungeon.depth || Dungeon.IsChallenged())
                return false

            val hero = Dungeon.hero
            val initialPerks = hero.heroClass.initialPerks()
            // never pass a drawback on to another hero
            val perks = hero.heroPerk.perks.filter { p ->
                !p.isNegative && initialPerks.none { it.javaClass == p.javaClass }
            }
            if (perks.isEmpty()) return false

            val perk = perks.random()
            records.add(SpiritRecord(
                    depth = Dungeon.depth,
                    heldBy = NO_SLOT,
                    heroClass = hero.heroClass,
                    // a detached copy, keeping the level the dead hero had it at
                    perk = perk.javaClass.newInstance().apply { level = perk.level },
                    userName = M.L(DarkSpirit::class.java, "name_of", hero.userName),
                    level = hero.lvl,
                    armor = capUpgrade(hero.belongings.armor),
                    weapon = carriedWeapon(hero)))

            return true
        }

        /**
         * The weapon the spirit will carry. One that is missing, or a plain tier-1, is too
         * forgettable to haunt anyone with, so a stronger one is rolled instead; a tier-2
         * one is rolled as well, with weights of its own.
         */
        private fun carriedWeapon(hero: Hero): Weapon {
            val carried = hero.belongings.weapon as? Weapon

            val weapon = when {
                carried == null -> rollWeapon(WEAPON_TIER_CHANCES)
                carried is MeleeWeapon && carried.tier <= 1 -> rollWeapon(WEAPON_TIER_CHANCES)
                carried is MeleeWeapon && carried.tier == 2 -> rollWeapon(TIER_2_WEAPON_CHANCES)
                else -> carried
            }

            capUpgrade(weapon)
            // known up front so the weapon does not "identify itself" while the spirit fights
            weapon.identify()

            return weapon
        }

        /** a re-rolled weapon is guaranteed to be either inscribed or cursed */
        private fun rollWeapon(chances: FloatArray): MeleeWeapon {
            val weapon = rollPlainWeapon(chances)
            if (weapon.inscription != null || weapon.cursed) return weapon

            return if (Random.Float() < 0.5f) {
                weapon.inscribe()
                weapon
            } else {
                rollPlainWeapon(chances).apply {
                    inscribe(Inscription.randomNegative())
                    cursed = true
                }
            }
        }

        private fun rollPlainWeapon(chances: FloatArray): MeleeWeapon {
            val tier = 2 + Random.chances(chances)

            // same guard as Ghost: a tier's generator is not guaranteed to hand back a melee weapon
            var weapon: Weapon
            do {
                weapon = Generator.WEAPON.MELEE.tier(tier).generate() as Weapon
            } while (weapon !is MeleeWeapon)

            return weapon as MeleeWeapon
        }

        private fun <T : Item> capUpgrade(item: T?): T? {
            item ?: return null

            val over = item.level() - MAX_UPGRADE
            if (over > 0) item.degrade(over)

            return item
        }

        private fun loadPool(): MutableList<SpiritRecord> {
            val records = mutableListOf<SpiritRecord>()

            try {
                val bundle = FileUtils.bundleFromFile(DS_FILE)
                for (i in 0 until bundle.getInt(POOL_COUNT)) {
                    loadRecord(bundle.getBundle(RECORD_PREFIX + i))?.let { records.add(it) }
                }
            } catch (e: IOException) {
                // no pool yet
                records.clear()
            } catch (e: Exception) {
                DarkestPixelDungeon.reportException(e)
                records.clear()
            }

            return records
        }

        private fun savePool(records: List<SpiritRecord>) {
            val bundle = Bundle()
            bundle.put(POOL_COUNT, records.size)
            records.forEachIndexed { i, record ->
                val recordBundle = Bundle()
                saveRecord(recordBundle, record)
                bundle.put(RECORD_PREFIX + i, recordBundle)
            }

            try {
                FileUtils.bundleToFile(DS_FILE, bundle)
            } catch (e: IOException) {
                DarkestPixelDungeon.reportException(e)
            }
        }

        private fun loadRecord(bundle: Bundle): SpiritRecord? {
            val perk = bundle.get(PERK) as Perk? ?: return null

            return SpiritRecord(
                    depth = bundle.getInt(DEPTH),
                    heldBy = bundle.getInt(HELD_BY),
                    heroClass = HeroClass.RestoreFromBundle(bundle),
                    perk = perk,
                    userName = bundle.getString(USERNAME),
                    level = bundle.getInt(LEVEL),
                    armor = bundle.get(ARMOR) as Armor?,
                    weapon = bundle.get(WEAPON) as Weapon?)
        }

        private fun saveRecord(bundle: Bundle, record: SpiritRecord) {
            bundle.put(DEPTH, record.depth)
            bundle.put(HELD_BY, record.heldBy)
            bundle.put(PERK, record.perk)
            bundle.put(USERNAME, record.userName)
            bundle.put(LEVEL, record.level)
            bundle.put(ARMOR, record.armor)
            bundle.put(WEAPON, record.weapon)
            record.heroClass.storeInBundle(bundle)
        }
    }
}

/** One dead hero waiting in the pool. [heldBy] is the save slot currently holding it, or [DarkSpirit.NO_SLOT]. */
internal class SpiritRecord(
        var depth: Int,
        var heldBy: Int,
        var heroClass: HeroClass,
        var perk: Perk,
        var userName: String,
        var level: Int,
        var armor: Armor?,
        var weapon: Weapon?)
