package com.egoal.darkestpixeldungeon.sprites

import com.badlogic.gdx.graphics.Pixmap
import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.egoal.darkestpixeldungeon.items.KindOfWeapon
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Camera
import com.watabou.noosa.Game
import com.watabou.noosa.Image
import com.watabou.noosa.TextureFilm
import com.watabou.utils.Callback
import com.watabou.utils.PointF

// paper doll for hero: body + head + helmet + armor + weapon layers

class DollSprite : CharSprite() {
    private lateinit var fly: IndexedAnimation
    private lateinit var read: IndexedAnimation

    // char sprite is not a group, make this ugly
    private val head = Head()
    private val armor = Armor()
    private val helmet = HelmetLayer()
    private val weapon = WeaponLayer(this)

    private var helmetState = Int.MIN_VALUE
    private var weaponId = -1

    init {
        initBodyAnimation()

        link(Dungeon.hero)
        updateHead()
        updateArmor()
        updateHelmet()
        refreshWeapon()

        head.flipHorizontal = flipHorizontal
        armor.flipHorizontal = flipHorizontal
        helmet.flipHorizontal = flipHorizontal

        if (ch.isAlive) idle() else die()
    }

    private fun initBodyAnimation() {
        texture(Assets.HERO_BODY)
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
        armor.row(armorRow(hero.heroClass, hero.tier()))
        syncComponentsFrame()
    }

    fun updateHelmet() {
        val hat = (ch as? Hero)?.belongings?.helmet
        val drawId = hat?.drawId ?: -1
        val state = if (drawId >= 0) drawId * 2 + (if (hat!!.replacesHead) 1 else 0) else -1
        if (state == helmetState) return
        helmetState = state

        if (drawId >= 0) {
            helmet.visible = true
            helmet.row(drawId)
            head.visible = !hat!!.replacesHead
        } else {
            helmet.visible = false
            head.visible = true
        }
        syncComponentsFrame()
    }

    private fun refreshWeapon() {
        val id = DollSpriteSheets.weaponDrawId((ch as? Hero)?.belongings?.weapon)
        if (id != weaponId) {
            weaponId = id
            weapon.updateGear(id)
            weapon.syncFrame(animationId(), curFrame)
        }
    }

    private fun syncComponentsFrame() {
        val index = (curAnim as? IndexedAnimation)?.frameIndexes?.getOrNull(curFrame) ?: 0
        if (head.visible) head.col(index)
        armor.col(index)
        if (helmet.visible) helmet.col(index)
        weapon.syncFrame(animationId(), curFrame)
    }

    private fun animationId(): Int = when (curAnim) {
        idle -> ANIM_IDLE
        run -> ANIM_RUN
        attack, zap -> ANIM_ATTACK
        operate -> ANIM_OPERATE
        fly -> ANIM_FLY
        else -> -1
    }

    override fun place(cell: Int) {
        super.place(cell)
        Camera.main.target = this
    }

    override fun turnTo(from: Int, to: Int) {
        super.turnTo(from, to)
        head.flipHorizontal = flipHorizontal
        armor.flipHorizontal = flipHorizontal
        helmet.flipHorizontal = flipHorizontal
    }

    override fun move(from: Int, to: Int) {
        super.move(from, to)
        if (ch.flying) play(fly)
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

    // do nothing to reduce the violence rating of the game
    override fun bloodBurstA(from: PointF, damage: Int) {}

    override fun update() {
        sleeping = ch.isAlive && (ch as Hero).resting
        super.update()

        updateHelmet()
        refreshWeapon()

        // sync pos
        head.point(point())
        armor.point(point())
        helmet.point(point())

        head.flipHorizontal = flipHorizontal
        armor.flipHorizontal = flipHorizontal
        helmet.flipHorizontal = flipHorizontal

        head.camera = camera
        armor.camera = camera
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
                // update frames
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
        if (head.visible) head.draw()
        armor.draw()
        if (helmet.visible) helmet.draw()
        weapon.drawLayer(curFrame)
    }

    fun sprint(on: Boolean): Boolean {
        run.delay = if (on) 0.667f / RUN_FRAMERATE else 1f / RUN_FRAMERATE
        return on
    }

    // record indexes
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

    class Head : Component(Assets.HERO_HEAD) {}

    class Armor : Component(Assets.HERO_ARMOR) {}

    class HelmetLayer : Component(Assets.HERO_HELMET) {}

    // NNYPD style equipment overlay: an independent texture sheet, whose row is
    // the gear id, and whose columns are animation frames selected by drawData
    class WeaponLayer(private val owner: CharSprite) : Image(TextureCache.get(Assets.HERO_WEAPONS)) {
        private val rows: TextureFilm = TextureFilm(texture, texture.width, WEAP_FRAME_HEIGHT)
        private var film: TextureFilm? = null
        private var drawData: Array<IntArray>? = null

        fun updateGear(id: Int) {
            drawData = null
            film = if (id >= 0) TextureFilm(rows, id, WEAP_FRAME_WIDTH, WEAP_FRAME_HEIGHT) else null
        }

        fun syncFrame(animationId: Int, curFrame: Int) {
            val data = drawTable(animationId)
            drawData = data
            val f = film ?: return
            if (data == null || curFrame >= data[0].size) return

            frame(f.get(data[0][curFrame]))
            flipHorizontal = owner.flipHorizontal
        }

        fun drawLayer(curFrame: Int) {
            val data = drawData ?: return
            val f = film ?: return
            val i = minOf(curFrame, data[0].size - 1)

            flipHorizontal = owner.flipHorizontal
            center(owner.center())
            x += data[1][i] * (if (flipHorizontal) -1f else 1f)
            y += data[2][i]
            alpha(owner.alpha())
            ra = owner.ra
            ga = owner.ga
            ba = owner.ba
            draw()
        }

        private fun drawTable(animationId: Int): Array<IntArray>? = when (animationId) {
            ANIM_IDLE -> arrayOf(
                    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                    intArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
                    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0))
            ANIM_RUN -> arrayOf(
                    intArrayOf(0, 0, 1, 1, 0, 0),
                    intArrayOf(2, 2, 3, 3, 2, 2),
                    intArrayOf(0, 0, 0, 0, 0, 0))
            ANIM_ATTACK -> arrayOf(
                    intArrayOf(1, 2, 3, 0),
                    intArrayOf(1, 2, 5, 3),
                    intArrayOf(0, 0, 0, 0))
            ANIM_OPERATE -> arrayOf(
                    intArrayOf(0, 0, 0, 0),
                    intArrayOf(2, 2, 2, 2),
                    intArrayOf(0, 0, 0, 0))
            ANIM_FLY -> arrayOf(
                    intArrayOf(0),
                    intArrayOf(3),
                    intArrayOf(0))
            else -> null
        }
    }

    companion object {
        private const val FRAME_WIDTH = 12
        private const val FRAME_HEIGHT = 15
        private const val RUN_FRAMERATE = 20

        private const val WEAP_FRAME_WIDTH = 20
        private const val WEAP_FRAME_HEIGHT = 19

        const val ANIM_IDLE = 0
        const val ANIM_RUN = 1
        const val ANIM_ATTACK = 2
        const val ANIM_FLY = 6
        const val ANIM_OPERATE = 20

        private const val PORTRAIT_SIZE = 26

        private val avatars = HashMap<Int, Image>()

        fun armorRow(hc: HeroClass, tier: Int): Int = when {
            tier >= 6 -> DollSpriteSheets.Armor.CLASS_ARMOR + hc.ordinal
            tier == 0 && hc != HeroClass.HUNTRESS && hc != HeroClass.SORCERESS -> DollSpriteSheets.Armor.UNDERWARE_MALE
            tier == 0 -> DollSpriteSheets.Armor.UNDERWARE_FEMALE
            else -> tier + 1
        }

        // full-body film over the legacy tiered sheet, for mobs borrowing hero
        // looks (mirror images, dark spirits)
        fun Tiers(): TextureFilm {
            val tex = TextureCache.get(Assets.ROGUE)
            return TextureFilm(tex, tex.width, FRAME_HEIGHT)
        }

        // static composite of body + head + armor, for UI thumbnails
        fun Avatar(hc: HeroClass, tier: Int): Image {
            val key = hc.ordinal * 8 + tier
            val cached = avatars[key]
            if (cached != null) return Image(cached)

            val pixmap = Pixmap(FRAME_WIDTH, FRAME_HEIGHT, Pixmap.Format.RGBA8888)
            pixmap.setBlending(Pixmap.Blending.SourceOver)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()

            fun blit(file: String, row: Int) {
                val tex = TextureCache.get(file)
                pixmap.drawPixmap(tex.bitmap,
                        0, row * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT,
                        0, 0, FRAME_WIDTH, FRAME_HEIGHT)
            }

            blit(Assets.HERO_BODY, 0)
            blit(Assets.HERO_HEAD, hc.ordinal)
            blit(Assets.HERO_ARMOR, armorRow(hc, tier))

            val avatar = Image(SmartTexture(pixmap))
            avatars[key] = avatar
            return Image(avatar)
        }

        fun Portrait(hc: HeroClass, tier: Int): Image {
            return Image(Assets.PORTRAITS, 0, PORTRAIT_SIZE * hc.ordinal, PORTRAIT_SIZE, PORTRAIT_SIZE)
        }
    }
}
