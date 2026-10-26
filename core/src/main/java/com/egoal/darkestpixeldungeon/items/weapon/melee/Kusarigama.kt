package com.egoal.darkestpixeldungeon.items.weapon.melee

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Actor
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.actors.Damage
import com.egoal.darkestpixeldungeon.actors.buffs.Bleeding
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.actors.buffs.Cripple
import com.egoal.darkestpixeldungeon.actors.buffs.Paralysis
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
import com.watabou.utils.Random
import java.util.ArrayList
import kotlin.math.round

class Kusarigama : MeleeWeapon() {

    private var form = MELEE

    // true = sickle, false = hammer. flipped before each landed melee strike, so the first hit uses the hammer.
    private var currentHand = true

    init {
        tier = 4

        defaultAction = AC_SWITCH

        applyForm()
    }

    override fun min(lvl: Int): Int = formDamage(4 + lvl)

    override fun max(lvl: Int): Int = formDamage(20 + 5 * lvl)

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
                updateQuickslot()
                GLog.i(M.L(this, if (form == RANGED) "to_ranged" else "to_melee"))
            }
        }
    }

    private fun applyForm() {
        image = if (form == RANGED) ItemSpriteSheet.KUSARIGAMA_RANGED else ItemSpriteSheet.KUSARIGAMA

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

        val onDragDone = Callback {
            Actor.addDelayed(Pushing(defender, defender.pos, newPos, Callback {
                defender.pos = newPos
                Dungeon.level.press(newPos, defender)
                Buff.prolong(defender, Cripple::class.java, 3f)
            }), -1f)

            // The hero's turn is still suspended on this aborted attack action.
            // Releasing it lets the actor thread run the queued Pushing; once the
            // enemy has been dragged in, the attack action is re-run against it
            // and lands the follow-up melee hit (which pays for the turn).
            attacker.next()
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

    private fun block(attacker: Hero) {
        GLog.w(M.L(this, "blocked"))
        attacker.ready()
    }

    // the cell the dragged enemy should end on, or null if the chain cannot reach it
    private fun pullDestination(attacker: Hero, defender: Char): Int? {
        if (defender.properties().contains(Char.Property.IMMOVABLE)) return null

        val chain = Ballistica(attacker.pos, defender.pos, Ballistica.PROJECTILE)
        if (chain.collisionPos != defender.pos || chain.path.size < 2) return null

        for (i in chain.subPath(1, chain.dist)) {
            if (!Level.solid[i] && Actor.findChar(i) == null) return i
        }
        return null
    }

    // a sweet spot hit plays the slash effect and causes bleeding equal to 1/7 of the damage dealt;
    // melee hits alternate hands: the sickle may cause bleeding, the hammer may paralyse
    override fun proc(dmg: Damage): Damage {
        val d = super.proc(dmg)

        if (d.from !is Hero || d.to !is Char) return d
        val attacker = d.from as Hero
        val defender = d.to as Char

        if (form == RANGED) {
            if (Dungeon.level.distance(attacker.pos, defender.pos) == reachFactor(attacker)) {
                KusarigamaSlash.slash(attacker, defender)
                val bleed = d.value / 7
                if (bleed > 0) Buff.affect(defender, Bleeding::class.java).set(bleed)
            }
            return d
        }

        currentHand = !currentHand
        if (d.value <= 0 || !defender.isAlive) return d

        if (currentHand) {
            if (Random.Float() < HAND_EFFECT_CHANCE) {
                val bleed = round(d.value * HAND_BLEED_FACTOR).toInt()
                if (bleed > 0) Buff.affect(defender, Bleeding::class.java).set(bleed)
            }
        } else {
            if (Random.Float() < HAND_EFFECT_CHANCE)
                Buff.prolong(defender, Paralysis::class.java, HAND_PARALYSIS_DURATION)
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

    // ranged form always sounds like the sickle, melee sounds like the hand that just struck
    override fun hitSound(): String? {
        if (form == RANGED) return Assets.SND_HIT2
        return if (currentHand) Assets.SND_HIT2 else Assets.SND_HIT
    }

    override fun desc(): String = super.desc() + "\n\n" + M.L(this, "desc_$form")

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(FORM, form)
        bundle.put(CURRENT_HAND, currentHand)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        form = bundle.getInt(FORM)
        // saves from before hand tracking have no value; the next strike then uses the hammer
        currentHand = if (bundle.contains(CURRENT_HAND)) bundle.getBoolean(CURRENT_HAND) else true
        applyForm()
    }

    companion object {
        private const val MELEE = 0
        private const val RANGED = 1

        private const val SWEET_SPOT_DAMAGE = 1.33f
        private const val MELEE_DAMAGE = 0.66f

        private const val HAND_EFFECT_CHANCE = 0.3f
        private const val HAND_BLEED_FACTOR = 0.15f
        private const val HAND_PARALYSIS_DURATION = 1f

        private const val AC_SWITCH = "switch"
        private const val FORM = "form"
        private const val CURRENT_HAND = "current_hand"
    }
}
