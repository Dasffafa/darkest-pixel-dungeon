package com.egoal.darkestpixeldungeon.items.artifacts

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.Actor
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.mobs.npcs.Sheep
import com.egoal.darkestpixeldungeon.effects.CellEmitter
import com.egoal.darkestpixeldungeon.effects.Speck
import com.egoal.darkestpixeldungeon.items.Item
import com.egoal.darkestpixeldungeon.items.scrolls.ScrollOfTeleportation
import com.egoal.darkestpixeldungeon.levels.Level
import com.egoal.darkestpixeldungeon.levels.traps.FlockTrap
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.messages.Messages
import com.egoal.darkestpixeldungeon.scenes.CellSelector
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.egoal.darkestpixeldungeon.utils.BArray
import com.egoal.darkestpixeldungeon.utils.GLog
import com.watabou.noosa.audio.Sample
import com.watabou.noosa.tweeners.Delayer
import com.watabou.noosa.tweeners.Tweener
import com.watabou.utils.Bundle
import com.watabou.utils.PathFinder
import com.watabou.utils.Random
import java.util.*


class CloakOfSheep : Artifact() {
    private var autoBlink = true

    init {
        image = ItemSpriteSheet.CLOAK_OF_SHEEP

        levelCap = 10

        cooldown = 0

        defaultAction = AC_BLINK
    }

    override fun actions(hero: Hero): ArrayList<String> {
        val actions = super.actions(hero)
        if (isEquipped(hero)) {
            if(!cursed && cooldown <= 0) actions.add(AC_BLINK)
            actions.add(AC_TOGGLE_AUTO)
        }
        return actions
    }

    override fun execute(hero: Hero, action: String) {
        super.execute(hero, action)

        if (action == AC_BLINK) {
            if (!isEquipped(hero))
                GLog.i(Messages.get(Artifact::class.java, "need_to_equip"))
            else if (cursed)
                GLog.i(Messages.get(this, "cursed"))
            else if (cooldown > 0)
                GLog.i(Messages.get(this, "not-ready"))
            else if (hero.rooted)
                GLog.i(Messages.get(this, "cannot-move"))
            else {
                // blink
                GameScene.selectCell(caster)
            }
        }
        else if (action == AC_TOGGLE_AUTO) {
            autoBlink = !autoBlink
            GLog.i(M.L(Sheep::class.java, if (autoBlink) "Baa!" else "Baa..."))
            updateQuickslot()
        }
    }

    private fun cd(): Int = 20
    private fun range(): Int = 5 + level() / 2
    private fun requireExp(): Int = (level() + 1) * (level() + 2) * 2

    private fun blink(hero: Hero, cell: Int, auto: Boolean) {
        cooldown = cd()

        val travelled = Dungeon.level.distance(cell, hero.pos)
        exp += if (auto) travelled / 2 else travelled
        if (exp > requireExp() && level() < levelCap) {
            exp -= requireExp()
            upgrade()
            GLog.p(Messages.get(this, "levelup"))
        }

        // create a sheep here
        val s = Sheep().apply {
            lifespan = level().toFloat() + Random.Int(Dungeon.depth + 10).toFloat()
            pos = hero.pos
        }
        GameScene.add(s)
        CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 4)
        s.yell(Messages.get(s, "baa?"))

        // jump there
        ScrollOfTeleportation.appear(hero, cell)
        hero.spendAndNext(.5f)
        Dungeon.observe()

        Sample.INSTANCE.play(Assets.SND_PUFF)

        updateQuickslot()
    }

    fun tryAutoBlink(hero: Hero, dest: Int): Boolean {
        if (!autoBlink) return false
        if (!isEquipped(hero) || cursed || cooldown > 0 || hero.rooted) return false
        if (dest == hero.pos) return false

        val len = Dungeon.level.length()
        var r = range()

        val walkable = BooleanArray(len) {
            Level.passable[it] && (Dungeon.level.visited[it] || Dungeon.level.mapped[it])
        }
        val path = Dungeon.findPath(hero, hero.pos, dest, walkable, Level.fieldOfView)
            ?: return false
        if (path.isEmpty()) return false
        if (path.size < minOf(r, 7)) return false

        var landing = -1
        var idx = path.size
        val it = path.descendingIterator()
        while (it.hasNext()) {
            idx--
            val c = it.next()
            if (Dungeon.level.distance(c, hero.pos) > r) continue
            if (Actor.findChar(c) != null || Level.solid[c]) continue
            landing = c
            break
        }

        if (landing < 0 || idx < 3) return false
        blink(hero, landing, true)
        hero.resting = false
        hero.interrupt()   // Pause for a short while
        val d = Delayer(0.3f)   // 300ms
        d.listener = Tweener.Listener { if (hero.curAction == null) hero.resume() }
        hero.sprite.parent.add(d)
        return true
    }

    override fun passiveBuff(): ArtifactBuff = Recharge()

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(AUTO_BLINK, autoBlink)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        if (bundle.contains(AUTO_BLINK)) autoBlink = bundle.getBoolean(AUTO_BLINK)
    }

    override fun desc(): String {
        var desc = super.desc()
        if (isEquipped(Dungeon.hero)) {
            desc += "\n\n" + Messages.get(this, "desc_hint", range())
            desc += "\n" + M.L(this, if (autoBlink) "desc_auto_on" else "desc_auto_off")
            if (isFullyUpgraded) desc += "\n" + M.L(this, "desc_max")
        }
        return desc
    }

    inner class Recharge : Artifact.ArtifactBuff() {
        override fun act(): Boolean {
            if (cooldown > 0 && !cursed)
                --cooldown

            if (cursed && Random.Int(100) == 0) {
                FlockTrap.ActivateAt(target.pos)
            }

            updateQuickslot()
            spend(Actor.TICK)
            return true
        }
    }

    private val caster = object : CellSelector.Listener {
        override fun onSelect(cell: Int?) {
            if (cell != null && (Dungeon.level.visited[cell] || Dungeon.level.mapped[cell])) {
                // valid 
                if (Dungeon.level.distance(cell, Item.curUser.pos) > range())
                    GLog.w(Messages.get(CloakOfSheep::class.java, "out-of-range"))
                else if (Level.solid[cell] || Actor.findChar(cell) != null)
                    GLog.w(Messages.get(CloakOfSheep::class.java, "cannot-go-there"))
                else {
                    // teleport can cross walls, but not into unreachable areas
                    // (e.g. rooms sealed off behind a locked door)
                    val passable = BooleanArray(Dungeon.level.length())
                    BArray.or(Level.passable, Level.avoid, passable)
                    PathFinder.buildDistanceMap(cell, passable, Integer.MAX_VALUE)
                    if (PathFinder.distance[Item.curUser.pos] == Integer.MAX_VALUE)
                        GLog.w(Messages.get(CloakOfSheep::class.java, "cannot-go-there"))
                    else
                        blink(Item.curUser, cell, false)
                }
            }
        }

        override fun prompt(): String = Messages.get(CloakOfSheep::class.java, "prompt")
    }

    companion object {
        private const val AC_BLINK = "blink"
        private const val AC_TOGGLE_AUTO = "toggle_auto"
        private const val AUTO_BLINK = "auto_blink"
    }
}