package com.egoal.darkestpixeldungeon.items.weapon.melee

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Actor
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.buffs.Bleeding
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.actors.buffs.Cripple
import com.egoal.darkestpixeldungeon.actors.buffs.Invisibility
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroAction
import com.egoal.darkestpixeldungeon.effects.Chains
import com.egoal.darkestpixeldungeon.effects.KusarigamaSlash
import com.egoal.darkestpixeldungeon.effects.Pushing
import com.egoal.darkestpixeldungeon.levels.Level
import com.egoal.darkestpixeldungeon.mechanics.Ballistica
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.egoal.darkestpixeldungeon.utils.GLog
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle
import com.watabou.utils.Callback
import java.util.ArrayList
import kotlin.math.round

class Kusarigama : MeleeWeapon() {

    private var form = MELEE

    // this is used to match the description: hammer in 1 hand and scythe in 1 hand. so the attack sound is alternating.
    // this will not be stored in the game saves slot. it's unnecessary.
    private var meleeSoundToggle = false

    init {
        image = ItemSpriteSheet.KUSARIGAMA

        tier = 4

        defaultAction = AC_SWITCH

        applyForm()
    }

    override fun min(lvl: Int): Int = formDamage(4 + lvl)

    override fun max(lvl: Int): Int = formDamage(22 + 4 * lvl)

    private fun formDamage(dmg: Int): Int = round(dmg * formDamageFactor()).toInt()

    private fun formDamageFactor(): Float = if (form == MELEE) MELEE_DAMAGE else SWEET_SPOT_DAMAGE

    override fun actions(hero: Hero): ArrayList<String> = super.actions(hero).apply { add(AC_SWITCH) }

    override fun execute(hero: Hero, action: String) {
        super.execute(hero, action)
        if (action == AC_SWITCH) {
            if (cursed) GLog.w(M.L(this, "cursed"))
            else {
                hero.doOperation(1f) {
                    form = if (form == MELEE) RANGED else MELEE
                    applyForm()
                }
                GLog.i(M.L(this, if (form == RANGED) "to_ranged" else "to_melee"))
            }
        }
    }

    private fun applyForm() {
        if (form == RANGED) {
            RCH = 3
            DLY = 1.5f
        } else {
            RCH = 1
            DLY = 0.66f
        }
    }

    override fun beforeAttack(attacker: Hero, defender: Char, action: HeroAction.Attack): Boolean {
        if (form != RANGED || !defender.isAlive) return true

        val dist = Dungeon.level.distance(attacker.pos, defender.pos)
        val reach = reachFactor(attacker)

        if (dist > reach) return true

        // sweet spot: a normal strike at maximum reach, no chain is thrown
        if (dist == reach) return true

        // adjacent: the blade is out of reach, the player must fold the weapon first
        if (dist == 1) {
            GLog.w(M.L(this, "enemy_too_close_ranged_form"))
            attacker.ready()
            return false
        }

        // mid range: drag the enemy in with the chain, then follow up in melee form
        val newPos = pullDestination(attacker, defender)
        if (newPos == null) {
            block(attacker)
            return false
        }

        form = MELEE
        applyForm()
        attacker.busy()
        attacker.spend(attacker.attackDelay())

        val onDragDone = Callback {
            Actor.addDelayed(Pushing(defender, defender.pos, newPos, Callback {
                defender.pos = newPos
                Dungeon.level.press(newPos, defender)
                Buff.prolong(defender, Cripple::class.java, 4f)
                followUp(attacker, defender)
            }), -1f)
        }
        throwChain(attacker, defender.pos, onDragDone)

        return false
    }

    private fun throwChain(attacker: Hero, target: Int, onDone: Callback?) {
        Sample.INSTANCE.play(Assets.SND_ASTROLABE)

        val parent = attacker.sprite.parent
        if (parent != null)
            parent.add(Chains(attacker.pos, target, onDone))
        else
            onDone?.call()
    }

    private fun followUp(attacker: Hero, defender: Char) {
        attacker.enemy = defender
        if (defender.isAlive && attacker.canAttack(defender)) {
            Invisibility.dispel()
            attacker.sprite.attack(defender.pos)
        } else {
            attacker.spendAndNext(0f)
            attacker.ready()
        }
    }

    private fun block(attacker: Hero) {
        GLog.w(M.L(this, "blocked"))
        attacker.ready()
    }

    // the cell the dragged enemy should end on, or null if the chain cannot reach it
    private fun pullDestination(attacker: Hero, defender: Char): Int? {
        if (defender.properties().contains(Char.Property.IMMOVABLE)) return null

        val chain = Ballistica(attacker.pos, defender.pos, Ballistica.PROJECTILE)
        if (chain.collisionPos != defender.pos || chain.path.size < 2 || Level.pit[chain.path[1]]) return null

        for (i in chain.subPath(1, chain.dist)) {
            if (!Level.solid[i] && Actor.findChar(i) == null) return i
        }
        return null
    }

    // a sweet spot hit plays the slash effect and causes bleeding equal to 10% of the damage dealt
    override fun proc(dmg: Damage): Damage {
        val d = super.proc(dmg)

        if (form == RANGED && d.from is Hero && d.to is Char) {
            val attacker = d.from as Hero
            val defender = d.to as Char
            if (Dungeon.level.distance(attacker.pos, defender.pos) == reachFactor(attacker)) {
                KusarigamaSlash.slash(attacker, defender)
                val bleed = d.value / 10
                if (bleed > 0) Buff.affect(defender, Bleeding::class.java).set(bleed)
            }
        }

        return d
    }

    override fun giveDamage(hero: Hero, target: Char): Damage {
        val dmg = super.giveDamage(hero, target)
        // min/max already apply the form's damage factor; a ranged hit only keeps the
        // sweet spot bonus when striking at maximum reach (e.g. Combo's Kick does not)
        if (form == RANGED && Dungeon.level.distance(hero.pos, target.pos) != reachFactor(hero))
            dmg.value = round(dmg.value / SWEET_SPOT_DAMAGE).toInt()
        return dmg
    }

    // ranged form always sounds like the sickle, melee alternates hammer and sickle
    override fun hitSound(): String? {
        if (form == RANGED) return Assets.SND_HIT2
        meleeSoundToggle = !meleeSoundToggle
        return if (meleeSoundToggle) Assets.SND_HIT else Assets.SND_HIT2
    }

    override fun desc(): String = super.desc() + "\n\n" + M.L(this, "desc_$form")

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(FORM, form)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        form = bundle.getInt(FORM)
        applyForm()
    }

    companion object {
        private const val MELEE = 0
        private const val RANGED = 1

        private const val SWEET_SPOT_DAMAGE = 1.33f
        private const val MELEE_DAMAGE = 0.66f

        private const val AC_SWITCH = "switch"
        private const val FORM = "form"
    }
}
