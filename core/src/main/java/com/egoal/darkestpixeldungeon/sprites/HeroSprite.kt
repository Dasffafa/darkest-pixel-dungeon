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
package com.egoal.darkestpixeldungeon.sprites

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Camera
import com.watabou.noosa.Game
import com.watabou.noosa.Image
import com.watabou.noosa.TextureFilm
import com.watabou.utils.Callback
import com.watabou.utils.PointF

class HeroSprite : CharSprite() {
    private lateinit var fly: IndexedAnimation
    private lateinit var read: IndexedAnimation

    private val head = Head()
    private val armorComp = ArmorComponent()
    private val helmet = HelmetComponent()

    init {
        texture(Assets.HERO_BODY)

        link(Dungeon.hero)
        initBodyAnimation()
        updateHead()
        updateArmor()
        updateHelmet()

        head.flipHorizontal = flipHorizontal
        armorComp.flipHorizontal = flipHorizontal
        helmet.flipHorizontal = flipHorizontal

        if (ch.isAlive)
            idle()
        else
            die()
    }

    private fun initBodyAnimation() {
        val body = TextureFilm(texture, FRAME_WIDTH, FRAME_HEIGHT)

        idle = IndexedAnimation(1, true)
        idle.frames(body, 0, 0, 0, 1, 0, 0, 1, 1)

        run = IndexedAnimation(RUN_FRAMERATE, true)
        run.frames(body, 2, 3, 4, 5, 6, 7)

        die = IndexedAnimation(20, false)
        die.frames(body, 8, 9, 10, 11, 12, 11)

        attack = IndexedAnimation(15, false)
        attack!!.frames(body, 13, 14, 15, 0)

        zap = (attack as IndexedAnimation).clone()

        operate = IndexedAnimation(8, false)
        operate!!.frames(body, 16, 17, 16, 17)

        fly = IndexedAnimation(1, true)
        fly.frames(body, 18)

        read = IndexedAnimation(20, false)
        read.frames(body, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19)
    }

    fun updateHead() {
        val hero = ch as? Hero ?: return
        head.row(hero.heroClass.ordinal)
        syncComponentsFrame()
    }

    fun updateArmor() {
        val hero = ch as? Hero ?: return
        val tier = hero.tier()
        if (tier == 0 && (hero.heroClass != HeroClass.HUNTRESS && hero.heroClass != HeroClass.SORCERESS))
            armorComp.row(0)
        else
            armorComp.row(tier + 1)
        syncComponentsFrame()
    }

    fun updateHelmet() {
        val hero = ch as? Hero ?: return
        val hat = hero.belongings.helmet
        if (hat != null && hat.drawId >= 0) {
            helmet.visible = true
            helmet.row(hat.drawId)
            head.visible = !hat.replacesHead
        } else {
            helmet.visible = false
            head.visible = true
        }
        syncComponentsFrame()
    }

    private fun syncComponentsFrame() {
        val index = (curAnim as? IndexedAnimation)?.frameIndexes?.getOrNull(curFrame) ?: 0
        if (head.visible) head.col(index)
        armorComp.col(index)
        if (helmet.visible) helmet.col(index)
    }

    override fun place(cell: Int) {
        super.place(cell)
        Camera.main.target = this
    }

    override fun turnTo(from: Int, to: Int) {
        super.turnTo(from, to)
        head.flipHorizontal = flipHorizontal
        armorComp.flipHorizontal = flipHorizontal
        helmet.flipHorizontal = flipHorizontal
    }

    override fun move(from: Int, to: Int) {
        super.move(from, to)
        if (ch.flying) {
            play(fly)
        }
        Camera.main.target = this
    }

    override fun jump(from: Int, to: Int, callback: Callback?) {
        super.jump(from, to, callback)
        play(fly)
    }

    fun read() {
        animCallback = Callback {
            idle()
            ch.onOperateComplete()
        }
        play(read)
    }

    override fun bloodBurstA(from: PointF, damage: Int) {
        // Does nothing to reduce violence rating
    }

    override fun update() {
        sleeping = ch.isAlive && (ch as Hero).resting
        super.update()

        val p = point()
        head.point(p)
        armorComp.point(p)
        helmet.point(p)

        head.flipHorizontal = flipHorizontal
        armorComp.flipHorizontal = flipHorizontal
        helmet.flipHorizontal = flipHorizontal

        head.camera = camera
        armorComp.camera = camera
        helmet.camera = camera
    }

    override fun updateAnimation() {
        if (curAnim != null && curAnim.delay > 0 && (curAnim.looped || !finished)) {
            val lastFrame = curFrame

            frameTimer += Game.elapsed
            while (frameTimer > curAnim.delay) {
                frameTimer -= curAnim.delay
                if (curFrame == curAnim.frames.size - 1) {
                    if (curAnim.looped) curFrame = 0
                    finished = true
                    if (listener != null) {
                        listener.onComplete(curAnim)
                        if (curAnim == null) return
                    }
                } else curFrame++
            }

            if (curFrame != lastFrame) {
                frame(curAnim.frames[curFrame])
                syncComponentsFrame()
            }
        }
    }

    override fun play(anim: Animation?, force: Boolean) {
        super.play(anim, force)
        syncComponentsFrame()
    }

    override fun draw() {
        super.draw()
        if (head.visible) {
            head.draw()
        }
        armorComp.draw()
        if (helmet.visible) {
            helmet.draw()
        }
    }

    fun sprint(on: Boolean): Boolean {
        run.delay = if (on) 0.667f / RUN_FRAMERATE else 1f / RUN_FRAMERATE
        return on
    }

    class IndexedAnimation(fps: Int, looped: Boolean) : Animation(fps, looped) {
        lateinit var frameIndexes: Array<Int>

        override fun frames(film: TextureFilm, vararg frames: Any?): Animation {
            super.frames(film, *frames)
            frameIndexes = Array(frames.size) { frames[it] as Int }
            return this
        }

        override fun clone(): IndexedAnimation {
            val anim = IndexedAnimation(Math.round(1 / delay), looped)
            anim.frames = frames
            if (::frameIndexes.isInitialized) {
                anim.frameIndexes = frameIndexes.clone()
            }
            return anim
        }
    }

    open class Component(texFile: String) : Image() {
        private val texRows: TextureFilm
        private lateinit var texGrids: TextureFilm

        init {
            val tex = TextureCache.get(texFile)
            texture(tex)
            texRows = TextureFilm(tex, tex.width, FRAME_HEIGHT)
            set(0, 0)
        }

        fun set(row: Int, col: Int) {
            row(row).col(col)
        }

        fun row(row: Int): Component {
            texGrids = TextureFilm(texRows, row, FRAME_WIDTH, FRAME_HEIGHT)
            return this
        }

        fun col(index: Int): Component {
            if (::texGrids.isInitialized) {
                val r = texGrids.get(index)
                if (r != null) {
                    frame(r)
                }
            }
            return this
        }
    }

    class Head : Component(Assets.HERO_HEAD)
    class ArmorComponent : Component(Assets.HERO_ARMOR)
    class HelmetComponent : Component(Assets.HERO_HELMET)

    companion object {
        private const val FRAME_WIDTH = 12
        private const val FRAME_HEIGHT = 15

        private const val RUN_FRAMERATE = 20

        private var tiers: TextureFilm? = null

        fun tiers(): TextureFilm {
            if (tiers == null) {
                val texture = TextureCache.get(Assets.ROGUE)
                tiers = TextureFilm(texture, texture.width, FRAME_HEIGHT)
            }

            return tiers!!
        }

        fun avatar(cl: HeroClass, armorTier: Int): Image {
            val patch = tiers().get(armorTier)
            val avatar = Image(cl.spritesheet())
            val frame = avatar.texture.uvRect(1f, 0f, FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat())
            frame.offset(patch.left, patch.top)
            avatar.frame(frame)

            return avatar
        }

        //todo: refactor this after assets ready,
        fun Portrait(cl: HeroClass, armorTier: Int): Image {
            val row = when (cl) {
                HeroClass.WARRIOR -> 0
                HeroClass.MAGE -> 1
                HeroClass.ROGUE -> 2
                HeroClass.HUNTRESS -> 3
                HeroClass.SORCERESS -> 4
                HeroClass.EXILE -> 5
            }

            return Image(Assets.PORTRAITS, 0, 26 * row, 26, 26)
        }
    }
}
