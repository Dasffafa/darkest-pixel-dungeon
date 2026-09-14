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
package com.egoal.darkestpixeldungeon.actors

import com.watabou.utils.SparseArray
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.Statistics
import com.watabou.noosa.Game
import com.watabou.utils.Bundlable
import com.watabou.utils.Bundle
import java.util.*

abstract class Actor : Bundlable {
    private var time = 0f

    private var id = 0

    //used to determine what order actors act in.
    //hero should always act on 0, therefore negative is before hero, positive
    // is after hero
    protected var actPriority = Integer.MAX_VALUE

    protected abstract fun act(): Boolean

    fun resetTime() {
        time = 0f
    }

    open fun spend(time: Float) {
        this.time += time
    }

    fun postpone(time: Float) {
        if (this.time < now + time)
            this.time = now + time
    }

    fun cooldown(): Float {
        return time - now
    }

    protected fun diactivate() {
        time = Float.MAX_VALUE
    }

    protected open fun onAdd() {}

    protected open fun onRemove() {}

    override fun storeInBundle(bundle: Bundle) {
        bundle.put(TIME, time)
        bundle.put(ID, id)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        time = bundle.getFloat(TIME)
        id = bundle.getInt(ID)
    }

    fun id(): Int {
        if (id <= 0) id = nextID++
        return id
    }

    /*protected*/
    open operator fun next() {
        if (current === this)
            current = null
    }

    companion object {
        const val TICK = 1f

        private const val TIME = "time"
        private const val ID = "id"

        private var nextID: Int = 1

        // **********************
        // *** Static members ***

        private val all = HashSet<Actor>()
        private val chars = HashSet<Char>()

        @Volatile
        private var current: Actor? = null

        // When false, the persistent actor thread exits its loop (app teardown).
        @Volatile
        var keepActorThreadAlive: Boolean = true

        // True only while the actor thread is actually running a turn. The
        // thread parks in wait() between turns, so `current != null` is no
        // longer a useful "is it stuck" signal for the watchdog.
        @Volatile
        private var threadActive: Boolean = false

        private val ids = SparseArray<Actor>()

        private var now = 0f

        @Synchronized
        fun clear() {
            now = 0f

            all.clear()
            chars.clear()

            ids.clear()
        }

        @Synchronized
        fun fixTime() {
            if (!Dungeon.isHeroNull && all.contains(Dungeon.hero)) Statistics.Duration += now

            var min = Float.MAX_VALUE
            for (a in all) {
                if (a.time < min) {
                    min = a.time
                }
            }
            for (a in all) {
                a.time -= min
            }
            now = 0f
        }

        fun init() {
            add(Dungeon.hero)

            for (mob in Dungeon.level.mobs) {
                add(mob)
            }

            for (blob in Dungeon.level.blobs.values) {
                add(blob)
            }

            current = null
        }

        private const val NEXTID = "nextid"

        fun storeNextID(bundle: Bundle) {
            bundle.put(NEXTID, nextID)
        }

        fun restoreNextID(bundle: Bundle) {
            nextID = bundle.getInt(NEXTID)
        }

        fun resetNextID() {
            nextID = 1
        }

        fun processing(): Boolean {
            return current != null
        }

        fun process() {
            var doNext: Boolean
            var interrupted = false

            threadActive = true
            do {
                current = null
                if (!interrupted && !Game.switchingScene()) {
                    var earliest = java.lang.Float.MAX_VALUE
                    for (actor in all) {

                        //some actors will always go before others if time is equal.
                        if (actor.time < earliest ||
                                actor.time == earliest && (current == null || actor.actPriority < current!!.actPriority)) {
                            earliest = actor.time
                            current = actor
                        }

                    }
                }

                if (current != null) {
                    now = current!!.time
                    val acting = current!!

                    if (acting is Char) {
                        // If it's character's turn to act, but its sprite
                        // is moving, wait till the movement is over
                        try {
                            synchronized(acting.sprite) {
                                if (acting.sprite.isMoving) {
                                    (acting.sprite as java.lang.Object).wait()
                                }
                            }
                        } catch (e: InterruptedException) {
                            interrupted = true
                        }

                    }

                    interrupted = interrupted || Thread.interrupted()

                    if (interrupted) {
                        doNext = false
                        current = null
                    } else {
                        doNext = acting.act()
                        if (doNext && (Dungeon.isHeroNull || !Dungeon.hero.isAlive)) {
                            doNext = false
                            current = null
                        }
                    }
                } else {
                    doNext = false
                }

                if (!doNext) {
                    // Nothing left to do: report that a turn finished, then park
                    // on our own monitor until the render thread wakes us for the
                    // next turn. Interrupts are expected (save/teardown), so they
                    // only make us skip this turn rather than abort the thread.
                    synchronized(Thread.currentThread()) {
                        interrupted = interrupted || Thread.interrupted()
                        if (interrupted) {
                            current = null
                            interrupted = false
                        }

                        threadActive = false
                        (Thread.currentThread() as java.lang.Object).notify()
                        try {
                            (Thread.currentThread() as java.lang.Object).wait()
                        } catch (e: InterruptedException) {
                            interrupted = true
                        }
                        threadActive = true
                    }
                }

            } while (keepActorThreadAlive)

            threadActive = false
        }

        fun isThreadActive(): Boolean = threadActive

        fun add(actor: Actor) {
            add(actor, now)
        }

        fun addDelayed(actor: Actor, delay: Float) {
            add(actor, now + delay)
        }

        @Synchronized
        private fun add(actor: Actor, time: Float) {
            if (all.contains(actor)) {
                return
            }

            ids.put(actor.id(), actor)

            all.add(actor)
            actor.time += time
            actor.onAdd()

            if (actor is Char) {
                chars.add(actor)
                for (buff in actor.buffs()) {
                    // may add twice when loading: the level is not initialized.
                    // if (all.contains(buff)) continue;

                    all.add(buff)
                    buff.onAdd()
                }
            }
        }

        @Synchronized
        fun remove(actor: Actor?) {

            if (actor != null) {
                all.remove(actor)
                chars.remove(actor)
                actor.onRemove()

                if (actor.id > 0) {
                    ids.remove(actor.id)
                }
            }
        }

        @Synchronized
        fun findChar(pos: Int): Char? = chars.find { it.pos == pos }

        @Synchronized
        fun findById(id: Int): Actor? = ids.get(id)

        // Return defensive copies: render-thread callers iterate these while the
        // actor thread is free to add/remove actors. Never hand out the live set.
        @Synchronized
        fun all(): HashSet<Actor> = HashSet(all)

        @Synchronized
        fun chars(): HashSet<Char> = HashSet(chars)
    }
}
