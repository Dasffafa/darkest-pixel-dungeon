package com.egoal.darkestpixeldungeon.sprites

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.Hero
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.egoal.darkestpixeldungeon.sprites.DollSpriteSheets.WeaponVisual
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
    private var currentHelmetRow = -1
    private var currentFrameIndex = 0
    private var weaponVisual: WeaponVisual = WeaponVisual.None

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
        val renderHead = if (drawId >= 0) loadHelmetMeta().renderHeadFor(drawId, !hat!!.replacesHead) else true
        val state = if (drawId >= 0) drawId * 2 + (if (renderHead) 0 else 1) else -1
        if (state == helmetState) return
        helmetState = state
        currentHelmetRow = drawId

        if (drawId >= 0) {
            helmet.visible = true
            helmet.row(drawId)
            head.visible = renderHead
        } else {
            helmet.visible = false
            head.visible = true
        }
        syncComponentsFrame()
    }

    private fun refreshWeapon() {
        val visual = DollSpriteSheets.weaponVisual((ch as? Hero)?.belongings?.weapon)
        if (visual != weaponVisual) {
            weaponVisual = visual
            weapon.updateGear(visual)
            weapon.syncFrame(animationId(), curFrame)
        }
    }

    private fun syncComponentsFrame() {
        val index = (curAnim as? IndexedAnimation)?.frameIndexes?.getOrNull(curFrame) ?: 0
        currentFrameIndex = index
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
        head.syncFlip(flipHorizontal)
        armor.syncFlip(flipHorizontal)
        helmet.syncFlip(flipHorizontal)
        weapon.syncFlip(flipHorizontal)
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

    /**
     * Play a named special animation from the custom weapon's JSON `animations` map
     * (e.g. "skill", "parry", "charge"). Non-looped animations return to the hero's
     * current state automatically when finished.
     */
    fun playWeaponAnim(name: String, onComplete: Callback? = null): Boolean {
        return weapon.playNamed(name, onComplete)
    }

    fun clearWeaponAnim() {
        weapon.clearNamed()
        weapon.syncFrame(animationId(), curFrame)
    }

    // do nothing to reduce the violence rating of the game
    override fun bloodBurstA(from: PointF, damage: Int) {}

    override fun update() {
        sleeping = ch.isAlive && (ch as Hero).resting
        super.update()

        updateHelmet()
        refreshWeapon()
        weapon.updateAnim(Game.elapsed)
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
        // Sync transform & camera in draw() so PosTweener updates from this frame are reflected
        if (head.visible) {
            head.syncFrom(this)
            head.draw()
        }
        armor.syncFrom(this)
        armor.draw()
        if (helmet.visible) {
            helmet.syncFrom(this)
            val off = loadHelmetMeta().offsetFor(currentHelmetRow, currentFrameIndex)
            helmet.x += off[0] * (if (flipHorizontal) -1f else 1f)
            helmet.y += off[1]
            helmet.draw()
        }
        weapon.drawLayer(curFrame)
    }

    override fun destroy() {
        super.destroy()
        head.destroy()
        armor.destroy()
        helmet.destroy()
        weapon.destroy()
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

        fun syncFlip(flip: Boolean) {
            if (flipHorizontal != flip) {
                flipHorizontal = flip
                updateFrame()
            }
        }

        fun syncFrom(owner: CharSprite) {
            point(owner.point())
            origin.set(owner.origin)
            scale.set(owner.scale)
            angle = owner.angle
            camera = owner.camera
            alpha(owner.alpha())
            ra = owner.ra
            ga = owner.ga
            ba = owner.ba
            rm = owner.rm
            gm = owner.gm
            bm = owner.bm
            syncFlip(owner.flipHorizontal)
        }
    }

    class Head : Component(Assets.HERO_HEAD) {}

    class Armor : Component(Assets.HERO_ARMOR) {}

    class HelmetLayer : Component(Assets.HERO_HELMET) {}

    data class CustomWeaponAnim(
            val frames: IntArray,
            val delay: Float,
            val looped: Boolean,
            val offsetX: Float,
            val offsetY: Float,
            val frameOffsets: Array<FloatArray>? = null
    )

    data class CustomWeaponSpec(
            val frameWidth: Int,
            val frameHeight: Int,
            val anims: Map<Int, CustomWeaponAnim>,
            val namedAnims: Map<String, CustomWeaponAnim>
    )

    // Supports both NNYPD-style legacy rows in weapons.png and custom per-weapon
    // sheets (actors/hero/weapons/<name>.png + .json) with arbitrary size & FPS.
    class WeaponLayer(private val owner: CharSprite) : Image(TextureCache.get(Assets.HERO_WEAPONS)) {
        private val legacyTex = texture
        private val legacyRows = TextureFilm(legacyTex, legacyTex.width, WEAP_FRAME_HEIGHT)
        private var film: TextureFilm? = null
        private var drawData: Array<IntArray>? = null

        private var customSpec: CustomWeaponSpec? = null
        private var customAnim: CustomWeaponAnim? = null
        private var customAnimId = -1
        private var customFrame = 0
        private var customTimer = 0f
        private var overrideAnimName: String? = null
        private var overrideCallback: Callback? = null

        fun updateGear(visual: WeaponVisual) {
            drawData = null
            customSpec = null
            customAnim = null
            customAnimId = -1
            customFrame = 0
            customTimer = 0f
            overrideAnimName = null
            overrideCallback = null

            when (visual) {
                WeaponVisual.None -> {
                    film = null
                }
                is WeaponVisual.Legacy -> {
                    texture(legacyTex)
                    film = TextureFilm(legacyRows, visual.row, WEAP_FRAME_WIDTH, WEAP_FRAME_HEIGHT)
                }
                is WeaponVisual.Custom -> {
                    val tex = TextureCache.get("actors/hero/weapons/${visual.name}.png")
                    val spec = loadCustomWeaponSpec(visual.name)
                    if (tex != null && spec != null) {
                        texture(tex)
                        customSpec = spec
                        film = TextureFilm(tex, spec.frameWidth, spec.frameHeight)
                    } else {
                        film = null
                    }
                }
            }
        }

        fun playNamed(name: String, onComplete: Callback? = null): Boolean {
            val spec = customSpec ?: return false
            val anim = spec.namedAnims[name] ?: return false
            val f = film ?: return false
            overrideAnimName = name
            overrideCallback = onComplete
            customAnim = anim
            customFrame = 0
            customTimer = 0f
            if (anim.frames.isNotEmpty()) {
                val rect = f.get(anim.frames[0])
                if (rect != null) frame(rect)
            }
            syncFlip(owner.flipHorizontal)
            return true
        }

        fun clearNamed() {
            overrideAnimName = null
            overrideCallback = null
            customAnimId = -1
        }

        fun syncFlip(flip: Boolean) {
            if (flipHorizontal != flip) {
                flipHorizontal = flip
                updateFrame()
            }
        }

        fun syncFrame(animationId: Int, curFrame: Int) {
            val spec = customSpec
            if (spec != null) {
                if (overrideAnimName != null) {
                    customAnimId = animationId
                    return
                }
                if (animationId != customAnimId) {
                    customAnimId = animationId
                    customAnim = if (animationId >= 0) {
                        spec.anims[animationId] ?: spec.anims[ANIM_IDLE]
                    } else {
                        null
                    }
                    customFrame = 0
                    customTimer = 0f
                }
                val anim = customAnim ?: return
                val f = film ?: return
                if (anim.frames.isNotEmpty()) {
                    val rect = f.get(anim.frames[minOf(customFrame, anim.frames.size - 1)])
                    if (rect != null) frame(rect)
                }
                syncFlip(owner.flipHorizontal)
                return
            }

            val data = drawTable(animationId)
            drawData = data
            val f = film ?: return
            if (data == null || curFrame >= data[0].size) return

            frame(f.get(data[0][curFrame]))
            syncFlip(owner.flipHorizontal)
        }

        fun updateAnim(elapsed: Float) {
            val anim = customAnim ?: return
            val f = film ?: return
            if (anim.delay <= 0f || anim.frames.isEmpty()) return
            if (anim.frames.size <= 1 && (anim.looped || overrideAnimName == null)) return

            customTimer += elapsed
            var changed = false
            var finishedOverride = false
            while (customTimer > anim.delay) {
                customTimer -= anim.delay
                if (customFrame >= anim.frames.size - 1) {
                    if (anim.looped) {
                        customFrame = 0
                        changed = true
                    } else {
                        customTimer = 0f
                        if (overrideAnimName != null) finishedOverride = true
                        break
                    }
                } else {
                    customFrame++
                    changed = true
                }
            }
            if (finishedOverride) {
                val cb = overrideCallback
                val restoreId = customAnimId
                clearNamed()
                syncFrame(restoreId, 0)
                cb?.call()
                return
            }
            if (changed) {
                val rect = f.get(anim.frames[customFrame])
                if (rect != null) {
                    frame(rect)
                    updateFrame()
                }
            }
        }

        fun drawLayer(curFrame: Int) {
            val f = film ?: return
            val dx: Float
            val dy: Float

            val anim = customAnim
            if (customSpec != null) {
                if (anim == null || anim.frames.isEmpty()) return
                val idx = minOf(customFrame, anim.frames.size - 1)
                val fo = anim.frameOffsets?.getOrNull(idx)
                dx = fo?.getOrNull(0) ?: anim.offsetX
                dy = fo?.getOrNull(1) ?: anim.offsetY
            } else {
                val data = drawData ?: return
                val i = minOf(curFrame, data[0].size - 1)
                dx = data[1][i].toFloat()
                dy = data[2][i].toFloat()
            }

            camera = owner.camera
            syncFlip(owner.flipHorizontal)
            center(owner.center())
            x += dx * (if (flipHorizontal) -1f else 1f)
            y += dy
            scale.set(owner.scale)
            angle = owner.angle
            alpha(owner.alpha())
            ra = owner.ra
            ga = owner.ga
            ba = owner.ba
            rm = owner.rm
            gm = owner.gm
            bm = owner.bm
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
        private val customWeaponSpecs = HashMap<String, CustomWeaponSpec?>()
        private var cachedHelmetMeta: HelmetMeta? = null

        class HelmetRowMeta(
                val offset: FloatArray?,
                val frameDeltas: Map<Int, FloatArray>,
                val frameOffsets: Map<Int, FloatArray>,
                val renderHead: Boolean? = null
        )

        class HelmetMeta(
                val defaultOffset: FloatArray,
                val defaultFrameDeltas: Map<Int, FloatArray>,
                val defaultFrameOffsets: Map<Int, FloatArray>,
                val rows: Map<Int, HelmetRowMeta>
        ) {
            fun renderHeadFor(row: Int, fallback: Boolean = true): Boolean {
                return rows[row]?.renderHead ?: fallback
            }

            private fun mirrorIfHeadTurn(base: FloatArray, frameIdx: Int): FloatArray {
                return if (frameIdx == 1) floatArrayOf(1f - base[0], base[1]) else base
            }

            fun offsetFor(row: Int, frameIdx: Int): FloatArray {
                val r = rows[row]
                if (r != null && !r.frameDeltas.containsKey(frameIdx)) {
                    r.frameOffsets[frameIdx]?.let { return it }
                }
                val rawBase = r?.offset ?: defaultOffset
                val base = mirrorIfHeadTurn(rawBase, frameIdx)
                val delta = r?.frameDeltas?.get(frameIdx) ?: defaultFrameDeltas[frameIdx]
                if (delta != null) {
                    return floatArrayOf(base[0] + delta[0], base[1] + delta[1])
                }
                if (r?.offset == null) {
                    defaultFrameOffsets[frameIdx]?.let { return it }
                }
                return base
            }
        }

        private fun parsePair(node: JsonValue?, fallback: FloatArray): FloatArray {
            if (node == null || node.size < 2) return fallback
            return floatArrayOf(node.getFloat(0), node.getFloat(1))
        }

        private fun parseFrameOffsetMap(node: JsonValue?): Map<Int, FloatArray> {
            val map = HashMap<Int, FloatArray>()
            var c = node?.child
            while (c != null) {
                val idx = c.name.toIntOrNull()
                if (idx != null && c.size >= 2) {
                    map[idx] = floatArrayOf(c.getFloat(0), c.getFloat(1))
                }
                c = c.next
            }
            return map
        }

        fun loadHelmetMeta(): HelmetMeta {
            cachedHelmetMeta?.let { return it }
            val fallbackDef = floatArrayOf(1f, -1f)
            val file = Gdx.files.internal("actors/hero/helmets_meta.json")
            val meta = if (file.exists()) {
                val root = JsonReader().parse(file)
                val defOff = parsePair(root.get("defaultOffset"), fallbackDef)
                val defDeltas = parseFrameOffsetMap(root.get("defaultFrameDeltas"))
                val defFrames = parseFrameOffsetMap(root.get("defaultFrameOffsets"))
                val rowsMap = HashMap<Int, HelmetRowMeta>()
                var rNode = root.get("rows")?.child
                while (rNode != null) {
                    val rowId = rNode.name.toIntOrNull()
                    if (rowId != null) {
                        val rOff = if (rNode.has("offset")) parsePair(rNode.get("offset"), defOff) else null
                        val rDeltas = parseFrameOffsetMap(rNode.get("frameDeltas"))
                        val rFrames = parseFrameOffsetMap(rNode.get("frameOffsets"))
                        val rRenderHead = if (rNode.has("renderHead")) rNode.getBoolean("renderHead", true) else null
                        rowsMap[rowId] = HelmetRowMeta(rOff, rDeltas, rFrames, rRenderHead)
                    }
                    rNode = rNode.next
                }
                HelmetMeta(defOff, defDeltas, defFrames, rowsMap)
            } else {
                HelmetMeta(fallbackDef, emptyMap(), emptyMap(), emptyMap())
            }
            cachedHelmetMeta = meta
            return meta
        }

        private fun animIdFromName(name: String): Int? = when (name) {
            "idle" -> ANIM_IDLE
            "run" -> ANIM_RUN
            "attack" -> ANIM_ATTACK
            "fly" -> ANIM_FLY
            "operate" -> ANIM_OPERATE
            else -> null
        }

        fun loadCustomWeaponSpec(name: String): CustomWeaponSpec? {
            if (customWeaponSpecs.containsKey(name)) return customWeaponSpecs[name]
            val file = Gdx.files.internal("actors/hero/weapons/$name.json")
            if (!file.exists()) {
                customWeaponSpecs[name] = null
                return null
            }
            val root: JsonValue = JsonReader().parse(file)
            val fw = root.getInt("frameWidth", WEAP_FRAME_WIDTH)
            val fh = root.getInt("frameHeight", WEAP_FRAME_HEIGHT)
            val defOx = root.getFloat("offsetX", 0f)
            val defOy = root.getFloat("offsetY", 0f)
            val anims = HashMap<Int, CustomWeaponAnim>()
            val namedAnims = HashMap<String, CustomWeaponAnim>()
            val animsNode = root.get("animations")
            if (animsNode != null) {
                var child = animsNode.child
                while (child != null) {
                    val animId = animIdFromName(child.name)
                    val frames = child.get("frames")?.asIntArray() ?: intArrayOf(0)
                    val fps = child.getFloat("fps", 1f)
                    val defaultLoop = animId == ANIM_IDLE || animId == ANIM_RUN || animId == ANIM_FLY
                    val looped = child.getBoolean("looped", defaultLoop)
                    val ox = child.getFloat("offsetX", defOx)
                    val oy = child.getFloat("offsetY", defOy)
                    val offsetsNode = child.get("offsets")
                    val frameOffsets = if (offsetsNode != null && offsetsNode.size > 0) {
                        Array(offsetsNode.size) { i ->
                            val pair = offsetsNode.get(i)
                            floatArrayOf(
                                    if (pair.size >= 1) pair.getFloat(0) else ox,
                                    if (pair.size >= 2) pair.getFloat(1) else oy
                            )
                        }
                    } else null
                    val parsed = CustomWeaponAnim(
                            frames = frames,
                            delay = if (fps > 0f) 1f / fps else 0f,
                            looped = looped,
                            offsetX = ox,
                            offsetY = oy,
                            frameOffsets = frameOffsets
                    )
                    namedAnims[child.name] = parsed
                    if (animId != null) anims[animId] = parsed
                    child = child.next
                }
            }
            val spec = CustomWeaponSpec(fw, fh, anims, namedAnims)
            customWeaponSpecs[name] = spec
            return spec
        }

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
