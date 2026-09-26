package com.egoal.darkestpixeldungeon.items

import com.egoal.darkestpixeldungeon.actors.hero.perks.Perk
import com.egoal.darkestpixeldungeon.actors.mobs.*
import com.egoal.darkestpixeldungeon.actors.mobs.npcs.*
import com.egoal.darkestpixeldungeon.items.armor.*
import com.egoal.darkestpixeldungeon.items.armor.curses.*
import com.egoal.darkestpixeldungeon.items.armor.glyphs.*
import com.egoal.darkestpixeldungeon.items.artifacts.*
import com.egoal.darkestpixeldungeon.items.food.*
import com.egoal.darkestpixeldungeon.items.rings.*
import com.egoal.darkestpixeldungeon.items.specials.Astrolabe
import com.egoal.darkestpixeldungeon.items.weapon.curses.*
import com.egoal.darkestpixeldungeon.items.weapon.enchantments.*
import com.egoal.darkestpixeldungeon.items.weapon.enchantments.Healing as WeaponHealing
import com.egoal.darkestpixeldungeon.items.weapon.inscriptions.*
import com.egoal.darkestpixeldungeon.items.weapon.inscriptions.Suppress as SuppressInscription
import com.egoal.darkestpixeldungeon.items.weapon.melee.*
import com.egoal.darkestpixeldungeon.items.weapon.missiles.*
import com.egoal.darkestpixeldungeon.levels.diggers.secret.SecretGuardianDigger
import com.egoal.darkestpixeldungeon.levels.traps.GuardianTrap
import com.watabou.utils.Bundle
import java.io.IOException

/**
 * One line of a catalog tab. Nearly every line stands for a whole class; a class
 * that has several looks of its own contributes one line per variant, and
 * [variant] then says which of them this line is.
 */
data class CatalogKey(val cls: Class<*>, val variant: String? = null)

/** variants of the classes whose single line is split, in display order */
private val classVariants: Map<Class<*>, List<String>> = mapOf(
        Statuary::class.java to Statuary.CatalogVariants
)

// variants that should unlock their base entry instead of being listed on their own
private val classConversions = mapOf<Class<*>, Class<*>>(
        Yog.RottingFist::class.java to Yog::class.java,
        Yog.BurningFist::class.java to Yog::class.java,
        Yog.Larva::class.java to Yog::class.java,
        King.Undead::class.java to King::class.java,
        Tengu.Phantom::class.java to Tengu::class.java,
        GuardianTrap.Guardian::class.java to Statue::class.java,
        SecretGuardianDigger.Companion.Guard::class.java to Statue::class.java
)

private fun converted(cls: Class<*>): Class<*> = classConversions[cls] ?: cls

/** every line a class contributes: itself, or one line per variant */
private fun keysOf(cls: Class<*>): List<CatalogKey> {
    val base = converted(cls)
    val variants = classVariants[base] ?: return listOf(CatalogKey(base))
    return variants.map { CatalogKey(base, it) }
}

/** the line a live mob belongs to */
private fun keyOf(mob: Mob): CatalogKey =
        CatalogKey(converted(mob.javaClass), (mob as? Statuary)?.type?.title)

enum class Catalog(private val baseItems: Map<Class<*>, Boolean>) {
    Ring(linkedMapOf(
            RingOfArcane::class.java to false,
            RingOfEvasion::class.java to false,
            RingOfResistance::class.java to false,
            RingOfForce::class.java to false,
            RingOfFuror::class.java to false,
            RingOfHaste::class.java to false,
            RingOfCritical::class.java to false,
            RingOfMight::class.java to false,
            RingOfSharpshooting::class.java to false,
            RingOfHealth::class.java to false,
            RingOfWealth::class.java to false
    )),

    ARTIFACT(linkedMapOf(
//            Astrolabe::class.java to false,
            CapeOfThorns::class.java to false,
            ChaliceOfBlood::class.java to false,
            CloakOfShadows::class.java to false,
            CrackedCoin::class.java to false,
            HornOfPlenty::class.java to false,
            MasterThievesArmband::class.java to false,
            SandalsOfNature::class.java to false,
            TalismanOfForesight::class.java to false,
            TimekeepersHourglass::class.java to false,
            UnstableSpellbook::class.java to false,
            DriedRose::class.java to false,
            LloydsBeacon::class.java to false,
            EtherealChains::class.java to false,
            RiemannianManifoldShield::class.java to false,
            GoldPlatedStatue::class.java to false,
            HandOfTheElder::class.java to false,
            HandleOfAbyss::class.java to false,
            HeartOfSatan::class.java to false,
            CloakOfSheep::class.java to false,
            EyeballOfTheElder.Right::class.java to false,
            EyeballOfTheElder.Left::class.java to false,
            DragonsSquama::class.java to false,
            GoddessRadiance::class.java to false
            )),

    /** every armor the hero can end up wearing, tier 1-5 first, then the class armors */
    ARMOR(linkedMapOf(
            ClothArmor::class.java to false,
            LeatherArmor::class.java to false,
            MailArmor::class.java to false,
            ScaleArmor::class.java to false,
            PlateArmor::class.java to false,
            WarriorArmor::class.java to false,
            MageArmor::class.java to false,
            RogueArmor::class.java to false,
            HuntressArmor::class.java to false,
            SorceressArmor::class.java to false,
            ExileArmor::class.java to false
    )),

    /**
     * Every weapon the hero can end up wielding, in the order the generator
     * hands them out: melee by tier, then the two fusion/quest weapons, then the
     * thrown ones.
     */
    WEAPON(linkedMapOf(
            // tier 1
            WornShortsword::class.java to false,
            Knuckles::class.java to false,
            Dagger::class.java to false,
            MagesStaff::class.java to false,
            SorceressWand::class.java to false,
            BattleGloves::class.java to false,
            RedHandleDagger::class.java to false,
            ShortSpear::class.java to false,
            ParryingDagger::class.java to false,
            // tier 2
            Dirk::class.java to false,
            ShortSword::class.java to false,
            HandAxe::class.java to false,
            Spear::class.java to false,
            Quarterstaff::class.java to false,
            Sickle::class.java to false,
            DriedLeg::class.java to false,
            Tulwar::class.java to false,
            CeremonialSword::class.java to false,
            ButchersKnife::class.java to false,
            ShortSticks::class.java to false,
            Nunchakus::class.java to false,
            // tier 3
            Sword::class.java to false,
            Mace::class.java to false,
            Scimitar::class.java to false,
            RoundShield::class.java to false,
            Sai::class.java to false,
            Whip::class.java to false,
            CrystalsSwords::class.java to false,
            DaggerAxe::class.java to false,
            InvisibleBlade::class.java to false,
            BoethiahsBlade::class.java to false,
            Flag::class.java to false,
            TengusKatana::class.java to false,
            CarvedStaff::class.java to false,
            // tier 4
            Longsword::class.java to false,
            BattleAxe::class.java to false,
            Flail::class.java to false,
            RunicBlade::class.java to false,
            AssassinsBlade::class.java to false,
            SpikeShield::class.java to false,
            Pitchfork::class.java to false,
            Halberd::class.java to false,
            Scythe::class.java to false,
            Kusarigama::class.java to false,
            LongestSpear::class.java to false,
            // tier 5
            Claymore::class.java to false,
            WarHammer::class.java to false,
            Glaive::class.java to false,
            Greataxe::class.java to false,
            Greatshield::class.java to false,
            Lance::class.java to false,
            LongSpear::class.java to false,
            // forged or given rather than found
            MagicBow::class.java to false,
            PairSwords::class.java to false,
            // thrown
            Boomerang::class.java to false,
            Dart::class.java to false,
            SmokeSparks::class.java to false,
            Salt::class.java to false,
            Shuriken::class.java to false,
            SwallowDart::class.java to false,
            IncendiaryDart::class.java to false,
            CurareDart::class.java to false,
            CeremonialDagger::class.java to false,
            FlyCutter::class.java to false,
            SeventhDart::class.java to false,
            RefinedSalt::class.java to false,
            Javelin::class.java to false,
            Tamahawk::class.java to false
    )),

    GLYPH(linkedMapOf(
            Affection::class.java to false,
            AntiMagic::class.java to false,
            Brimstone::class.java to false,
            Camouflage::class.java to false,
            ChuNeng::class.java to false,
            Clarifying::class.java to false,
            Flow::class.java to false,
            Healing::class.java to false,
            Obfuscation::class.java to false,
            Peaceful::class.java to false,
            Potential::class.java to false,
            Protection::class.java to false,
            Repulsion::class.java to false,
            Stone::class.java to false,
            Swiftness::class.java to false,
            Thorns::class.java to false,
            Tough::class.java to false,
            Viscosity::class.java to false
    )),

    ARMOR_CURSE(linkedMapOf(
            AntiEntropy::class.java to false,
            Corrosion::class.java to false,
            Displacement::class.java to false,
            Entanglement::class.java to false,
            Metabolism::class.java to false,
            Multiplicity::class.java to false,
            Stench::class.java to false
    )),

    ENCHANTMENT(linkedMapOf(
            Bashing::class.java to false,
            Blazing::class.java to false,
            Blinding::class.java to false,
            BloodCoil::class.java to false,
            Chilling::class.java to false,
            WeaponHealing::class.java to false,
            StunningEcht::class.java to false,
            Magical::class.java to false,
            Rousing::class.java to false,
            Shocking::class.java to false,
            Sophisticated::class.java to false,
            Tracking::class.java to false,
            Unstable::class.java to false,
            Venomous::class.java to false
    )),

    INSCRIPTION(linkedMapOf(
            Dazzling::class.java to false,
            Eldritch::class.java to false,
            Lucky::class.java to false,
            Projecting::class.java to false,
            Storming::class.java to false,
            Heavy::class.java to false,
            SuppressInscription::class.java to false,
            Vorpal::class.java to false,
            Grim::class.java to false,
            Stunning::class.java to false,
            Vampiric::class.java to false,
            Holy::class.java to false
    )),

    WEAPON_CURSE(linkedMapOf(
            Annoying::class.java to false,
            Arrogant::class.java to false,
            Bloodthirsty::class.java to false,
            Displacing::class.java to false,
            Exhausting::class.java to false,
            Fragile::class.java to false,
            Provocation::class.java to false,
            Sacrificial::class.java to false,
            Wayward::class.java to false
    )),

    /**
     * Everything edible or drinkable, in rough order of how much they feed: the
     * plain ration first, then the foods, then the special consumable, then the
     * wines, which were this tab's only members while it was still a wine tab.
     */
    FOOD(linkedMapOf(
            Food::class.java to false,
            Pasty::class.java to false,
            Blandfruit::class.java to false,
            ChargrilledMeat::class.java to false,
            OrchidRoot::class.java to false,
            OverpricedRation::class.java to false,
            FrozenCarpaccio::class.java to false,
            StewedMeat::class.java to false,
            SkewerMeat::class.java to false,
            MysteryMeat::class.java to false,
            Humanity::class.java to false,
            Wine::class.java to false,
            BrownAle::class.java to false,
            RiceWine::class.java to false,
            MeadWine::class.java to false
    )),

    MOB(linkedMapOf(
            Acidic::class.java to false,
            Albino::class.java to false,
            AshesSkull::class.java to false,
            Ballista::class.java to false,
            Bandit::class.java to false,
            Bat::class.java to false,
            Bee::class.java to false,
            Brute::class.java to false,
            Crab::class.java to false,
            DarkSpirit::class.java to false,
            DevilGhost::class.java to false,
            DM300::class.java to false,
            Elemental::class.java to false,
            Eye::class.java to false,
            FetidRat::class.java to false,
            Frog::class.java to false,
            Glowworm::class.java to false,
            Gnoll::class.java to false,
            GnollTrickster::class.java to false,
            Golem::class.java to false,
            Goo::class.java to false,
            GreatCrab::class.java to false,
            Guard::class.java to false,
            King::class.java to false,
            MadMan::class.java to false,
            Mimic::class.java to false,
            Monk::class.java to false,
            NewbornElemental::class.java to false,
            Piranha::class.java to false,
            QuickFiringGun::class.java to false,
            Rat::class.java to false,
            RotHeart::class.java to false,
            RotLasher::class.java to false,
            Rotten::class.java to false,
            Scorpio::class.java to false,
            Senior::class.java to false,
            Shaman::class.java to false,
            Shielded::class.java to false,
            Skeleton::class.java to false,
            SkeletonKnight::class.java to false,
            Slug::class.java to false,
            Spinner::class.java to false,
            Statue::class.java to false,
            Succubus::class.java to false,
            Swarm::class.java to false,
            Tengu::class.java to false,
            Thief::class.java to false,
            WandGuard::class.java to false,
            Warlock::class.java to false,
            WitchDoctor::class.java to false,
            Wraith::class.java to false,
            Yog::class.java to false
    )),

    NPC(linkedMapOf(
            AbyssHero::class.java to false,
            Alchemist::class.java to false,
            ArchDemon::class.java to false,
            BarterMan::class.java to false,
            Blacksmith::class.java to false,
            CatEgoal::class.java to false,
            CatRawberry::class.java to false,
            DisheartenedBuddy::class.java to false,
            Ghost::class.java to false,
            GhostHero::class.java to false,
            Imp::class.java to false,
            Jessica::class.java to false,
            KingStatuary::class.java to false,
            Merchant::class.java to false,
            MerchantImp::class.java to false,
            Minstrel::class.java to false,
            MirrorImage::class.java to false,
            Monument::class.java to false,
            Passerby::class.java to false,
            PlagueDoctor::class.java to false,
            PotionSeller::class.java to false,
            Questioner::class.java to false,
            RatKing::class.java to false,
            RobotREN::class.java to false,
            Scholar::class.java to false,
            ScrollSeller::class.java to false,
            Seeker::class.java to false,
            Sheep::class.java to false,
            SPDBattleMage::class.java to false,
            Statuary::class.java to false,
            TheBouquet::class.java to false,
            UndeadShopkeeper::class.java to false,
            Wandmaker::class.java to false,
            Yvette::class.java to false
    )),

    /** the perks the hero can be offered or handed; see [Perk.catalogRoster] */
    PERK(Perk.catalogRoster.associateWith { false }),

    ;

    // keeps the declared roster order, with a split class's variants together
    private val items: HashMap<CatalogKey, Boolean> = LinkedHashMap<CatalogKey, Boolean>().apply {
        for ((cls, seen) in baseItems)
            for (key in keysOf(cls)) put(key, seen)
    }

    fun allItems(): Collection<CatalogKey> = items.keys

    fun allSeen() = items.all { it.value }

    fun see(itemClass: Class<*>) {
        val key = CatalogKey(itemClass)
        if (items[key] == false) {
            items[key] = true
            changed = true
        }
    }

    fun isSeen(itemClass: Class<*>) = items[CatalogKey(itemClass)] == true

    companion object {
        private const val CATALOG_FILE = "catalog.dat"

        private const val CATELOG = "catalog"

        /** the variant of each entry in [CATELOG], empty for the plain classes */
        private const val CATELOG_VARIANTS = "variants"

        private var changed = false

        // set by DebugUnlockAll: a debug run must not write the catalog to the save
        private var debugUnlocked = false

        /** mobs that can turn up on any depth; they get a page of their own */
        private val UniversalMobs = listOf(
                Mimic::class.java, Piranha::class.java, Statue::class.java, WandGuard::class.java,
                Wraith::class.java, Bee::class.java, RotLasher::class.java, DevilGhost::class.java,
                Frog::class.java
        )

        /**
         * Contents of the monster tab's six pages: regions 1-5 by depth band, each
         * ending with its act boss, then a page for the depth-independent mobs. A
         * mob appears on every region page it can spawn on, so the pages overlap,
         * but their union is [MOB]'s roster and each boss is its page's last entry.
         */
        val MobSections: List<List<Class<*>>> = listOf(
                // 一 下水道 1-5，末尾是首领 Goo
                listOf(Rat::class.java, Slug::class.java, Gnoll::class.java, Crab::class.java,
                        Swarm::class.java, Albino::class.java, MadMan::class.java,
                        FetidRat::class.java, GnollTrickster::class.java, GreatCrab::class.java,
                        Glowworm::class.java, Goo::class.java),
                // 二 监狱 6-10，末尾是首领 Tengu
                listOf(Skeleton::class.java, Thief::class.java, Bandit::class.java, Swarm::class.java,
                        Shaman::class.java, Guard::class.java, MadMan::class.java,
                        WitchDoctor::class.java, Rotten::class.java, Bat::class.java,
                        DarkSpirit::class.java, NewbornElemental::class.java, RotHeart::class.java,
                        Glowworm::class.java, Tengu::class.java),
                // 三 洞穴 11-15，末尾是首领 DM300
                listOf(Bat::class.java, AshesSkull::class.java, SkeletonKnight::class.java, Brute::class.java,
                        Shielded::class.java, WitchDoctor::class.java, Rotten::class.java, MadMan::class.java,
                        Spinner::class.java, Ballista::class.java, QuickFiringGun::class.java,
                        Elemental::class.java, DarkSpirit::class.java, Glowworm::class.java, DM300::class.java),
                // 四 城市 16-20，末尾是首领 King
                listOf(Elemental::class.java, Warlock::class.java, Ballista::class.java,
                        QuickFiringGun::class.java, Monk::class.java, Senior::class.java, Golem::class.java,
                        MadMan::class.java, Succubus::class.java, Glowworm::class.java, King::class.java),
                // 五 恶魔大厅 21-25，末尾是首领 Yog
                listOf(Succubus::class.java, Eye::class.java, Scorpio::class.java, Acidic::class.java,
                        MadMan::class.java, Yog::class.java),
                // 通 各层通用
                UniversalMobs
        )

        /**
         * The perk tab's order: grouped by tag, in [Perk.Tag]'s own order, with the
         * untagged perks last; the roster order decides within a group. A perk that
         * carries several tags sits in the group of its earliest one, so every perk
         * is listed exactly once.
         */
        val PerkOrder: List<Class<*>> by lazy {
            val tags = HashMap<Class<*>, Set<Perk.Tag>>()
            PERK.allItems().map { it.cls }
                    .sortedBy { cls ->
                        tags.getOrPut(cls) { (cls.newInstance() as Perk).tags }
                                .minOfOrNull { it.ordinal } ?: Int.MAX_VALUE
                    }
        }

        /**
         * Unlocks every entry for the running session. Used by debug builds
         * (desktop runDebug / the debug preference) and deliberately does not
         * flag the catalog as changed, so a debug run never dirties the save.
         */
        fun DebugUnlockAll() {
            debugUnlocked = true
            for (cat in values())
                for (key in cat.items.keys) cat.items[key] = true
        }

        /** Applies the debug unlock when running a debug build. */
        fun ApplyDebugUnlock() {
            if (com.egoal.darkestpixeldungeon.DarkestPixelDungeon.debug()) DebugUnlockAll()
        }

        fun categoryOf(cls: Class<*>): Catalog? {
            val key = CatalogKey(converted(cls))
            for (cat in values())
                if (cat.items.containsKey(key)) return cat
            return null
        }

        /** a fresh mob for the catalog to draw, told which variant it is when it has one */
        fun instantiate(key: CatalogKey): Mob =
                if (key.cls == Statuary::class.java) Statuary.ForVariant(key.variant)
                else key.cls.newInstance() as Mob

        fun IsSeen(key: CatalogKey): Boolean {
            for (cat in values())
                if (cat.items.containsKey(key))
                    return cat.items[key]!!
            return false
        }

        fun IsSeen(itemClass: Class<*>) = IsSeen(CatalogKey(converted(itemClass)))

        fun SetSeen(key: CatalogKey) {
            for (cat in values())
                if (cat.items[key] == false) {
                    cat.items[key] = true
                    changed = true
                }
        }

        fun SetSeen(itemClass: Class<*>) = SetSeen(CatalogKey(converted(itemClass)))

        /** marks the line of the live mob, variant included, as seen */
        fun SetSeen(mob: Mob) = SetSeen(keyOf(mob))

        fun Save() {
            // debug runs keep their unlocks in memory only
            if (!changed || debugUnlocked) return

            val bundle = Bundle()

            val items = ArrayList<Class<*>>()
            val variants = ArrayList<String>()
            for (cat in values()) {
                for ((key, seen) in cat.items)
                    if (seen) {
                        items.add(key.cls)
                        variants.add(key.variant ?: "")
                    }
            }
            bundle.put(CATELOG, items.toTypedArray())
            bundle.put(CATELOG_VARIANTS, variants.toTypedArray())

            com.watabou.utils.FileUtils.bundleToFile(CATALOG_FILE, bundle)
        }

        fun Load() {
            try {
                val bundle = com.watabou.utils.FileUtils.bundleFromFile(CATALOG_FILE)

                if (bundle.contains(CATELOG)) {
                    val items = bundle.getClassArray(CATELOG)
                    val variants = bundle.getStringArray(CATELOG_VARIANTS) ?: emptyArray<String>()
                    for (i in items.indices) {
                        val item = items[i] ?: continue
                        SetSeen(CatalogKey(item, variants.getOrNull(i)?.takeIf { it.isNotEmpty() }))
                    }
                }

                changed = false
            } catch (e: IOException) {
            }

            ApplyDebugUnlock()
        }
    }
}
