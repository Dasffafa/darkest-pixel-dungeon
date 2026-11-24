package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.levels.lighting.LightSource
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.noosa.particles.PixelParticle
import com.watabou.utils.ColorMath
import com.watabou.utils.Random
import com.watabou.utils.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 亡灵/幽灵从地中冒出的召唤特效。
 * 移植自 Wayward Souls `shadowrevive`（shadow0Shock + pushRingShadow）
 * 与破土尘雾/碎石粒子（tremorDust + tremorRumble + physobjground）。
 */
class UndeadEmergeEffect private constructor(
    private val targetSprite: CharSprite?,
    private val cell: Int,
    private val duration: Float,
    private val ringColor: Int
) : Group() {

    private val srcFrame: RectF? = targetSprite?.let { RectF(it.frame()) }
    private val groundY: Float = targetSprite?.let { it.y + it.height }
        ?: (DungeonTilemap.tileCenterToWorld(cell).y + DungeonTilemap.SIZE / 2f - 2f)
    private val footX: Float = targetSprite?.let { it.x + it.width / 2f }
        ?: DungeonTilemap.tileCenterToWorld(cell).x
    private val footY: Float = groundY - 2f

    private val rift: RiftVisual
    private val emergingBody: Image?
    private val dustGroup: Group
    private val shockGroup: ShockGroup
    private var elapsed = 0f
    private var spawnedRings = 1
    private var finished = false

    init {
        targetSprite?.visible = false

        rift = RiftVisual(footX, footY, ringColor)
        add(rift)

        dustGroup = Group()
        add(dustGroup)
        spawnGroundBurst()

        emergingBody = if (targetSprite != null && srcFrame != null) {
            Image().apply {
                texture = targetSprite.texture
                flipHorizontal = targetSprite.flipHorizontal
                frame(RectF(srcFrame.left, srcFrame.top, srcFrame.right, srcFrame.top + srcFrame.height() * 0.05f))
                x = targetSprite.x
                y = groundY - height()
            }.also { add(it) }
        } else {
            null
        }

        shockGroup = ShockGroup(footX, footY, ringColor)
        add(shockGroup)
        shockGroup.spawnRing(duration * 0.72f, 18f)
        shockGroup.spawnShocks(16)
    }

    private fun spawnGroundBurst() {
        // tremorDust: 低空贴地扩散尘雾
        for (i in 0 until 8) {
            val p = dustGroup.recycle(TremorDustParticle::class.java) as TremorDustParticle
            p.reset(footX, footY)
        }
        // tremorRumble: 向上翻涌扩大的土黄色尘团
        for (i in 0 until 6) {
            val p = dustGroup.recycle(TremorRumbleParticle::class.java) as TremorRumbleParticle
            p.reset(footX, footY)
        }
        // physobjground / burrowrock1-2: 带重力弹跳、旋转与滞留淡出的 8x8 破土石块
        for (i in 0 until 7) {
            val p = dustGroup.recycle(BurrowRockParticle::class.java) as BurrowRockParticle
            p.reset(footX, footY)
        }
    }

    override fun update() {
        super.update()
        if (finished) {
            if (shockGroup.countLiving() == 0 && dustGroup.countLiving() == 0 && !rift.alive) {
                killAndErase()
            }
            return
        }

        elapsed += Game.elapsed
        val t = min(1f, elapsed / duration)

        // pushRingShadow: 错开时间连续生成 4 道扩张暗影环
        val ringTrigger = spawnedRings * 0.22f
        if (spawnedRings < 4 && t >= ringTrigger) {
            spawnedRings++
            shockGroup.spawnRing(duration * Random.Float(0.55f, 0.85f), Random.Float(13f, 20f))
        }

        // 三次缓出破土揭露：头 -> 躯干 -> 脚，地面以下自然裁剪
        if (emergingBody != null && targetSprite != null && srcFrame != null) {
            val inv = 1f - t
            val reveal = max(0.05f, 1f - inv * inv * inv)
            val curV = srcFrame.top + srcFrame.height() * reveal
            emergingBody.frame(RectF(srcFrame.left, srcFrame.top, srcFrame.right, curV))
            emergingBody.x = targetSprite.x
            emergingBody.y = groundY - emergingBody.height()
        }

        rift.progress = t

        if (t >= 1f) {
            finished = true
            targetSprite?.visible = true
            emergingBody?.killAndErase()
            rift.beginFade()
        }
    }

    private class RiftVisual(
        cx: Float,
        cy: Float,
        rgb: Int
    ) : Image(softDiscTexture()) {
        var progress = 0f
        private var fading = false
        private var fadeElapsed = 0f

        init {
            origin.set(width / 2f, height / 2f)
            hardlight(ColorMath.interpolate(rgb, 0x090212, 0.74f))
            x = cx - width / 2f
            y = cy - height / 2f
            scale.set(0.2f, 0.1f)
            alpha(0.88f)
        }

        fun beginFade() {
            fading = true
        }

        override fun update() {
            super.update()
            if (!fading) {
                val open = sin(min(1f, progress) * PI * 0.85).toFloat()
                scale.set(0.35f + 0.85f * open, 0.18f + 0.35f * open)
                alpha(0.88f)
            } else {
                fadeElapsed += Game.elapsed
                val k = min(1f, fadeElapsed / 0.18f)
                val s = 1f - k
                scale.set(1.1f * s, 0.45f * s)
                alpha(0.88f * s)
                if (k >= 1f) killAndErase()
            }
        }
    }

    private class ShockGroup(
        private val cx: Float,
        private val cy: Float,
        private val color: Int
    ) : Group() {

        fun spawnRing(dur: Float, targetRadius: Float) {
            add(RingVisual(cx, cy, color, targetRadius, dur))
        }

        fun spawnShocks(count: Int) {
            for (i in 0 until count) {
                val p = recycle(ShadowShockParticle::class.java) as ShadowShockParticle
                p.reset(cx, cy, color)
            }
        }

        override fun draw() {
            Gdx.gl.glBlendFuncSeparate(
                GL20.GL_SRC_ALPHA, GL20.GL_ONE,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA
            )
            super.draw()
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
    }

    internal class RingVisual(
        cx: Float,
        cy: Float,
        rgb: Int,
        private val targetRadius: Float,
        private val duration: Float
    ) : Image(ringTexture()) {
        private var elapsed = 0f

        init {
            origin.set(width / 2f, height / 2f)
            hardlight(rgb)
            x = cx - width / 2f
            y = cy - height / 2f
            scale.set(0.22f, 0.12f)
            alpha(0.92f)
        }

        override fun update() {
            super.update()
            elapsed += Game.elapsed
            val t = min(1f, elapsed / duration)
            val ease = 1f - (1f - t) * (1f - t)
            val baseSize = max(1f, width)
            val s = (4f + targetRadius * 2f * ease) / baseSize
            scale.set(s, s * 0.55f)
            alpha((1f - t) * 0.85f)
            if (t >= 1f) killAndErase()
        }
    }

    /**
     * 移植自 `shadow0Shock`：加色柔光能量团，初速高并受强阻尼迅速减速收缩。
     */
    class ShadowShockParticle : Image(softDiscTexture()) {
        private var lifespan = 0.5f
        private var left = 0.5f
        private var startSize = 6f
        private var endSize = 1.5f
        private var vx = 0f
        private var vy = 0f
        private var delay = 0f
        private var startColor = DEFAULT_COLOR
        private var endColor = 0x330066

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float, color: Int) {
            revive()
            startColor = ColorMath.interpolate(color, 0xCC66FF, Random.Float(0f, 0.35f))
            endColor = ColorMath.interpolate(color, 0x2A0055, 0.65f)
            hardlight(startColor)

            startSize = Random.Float(5.5f, 9.5f)
            endSize = Random.Float(1.2f, 2.2f)
            lifespan = Random.Float(0.28f, 0.68f)
            left = lifespan
            delay = Random.Float(0f, 0.05f)

            val angle = Random.Float((PI * 2.0).toFloat())
            val spd = Random.Float(28f, 52f)
            vx = cos(angle) * spd
            vy = sin(angle) * spd * 0.55f - Random.Float(6f, 16f)

            x = cx - width / 2f
            y = cy - height / 2f
            val s = startSize / max(1f, width)
            scale.set(s)
            alpha(0.85f)
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            if (delay > 0f) {
                delay -= dt
                return
            }
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }

            // 强阻尼减速 + 轻微上浮
            val damp = Math.pow(0.04, dt.toDouble()).toFloat()
            vx *= damp
            vy = vy * damp - 8f * dt
            x += vx * dt
            y += vy * dt

            val p = 1f - left / lifespan
            val curSize = startSize + (endSize - startSize) * p
            val s = curSize / max(1f, width)
            scale.set(s)
            hardlight(ColorMath.interpolate(startColor, endColor, p))
            alpha((1f - p * p) * 0.8f)
        }
    }

    /**
     * 移植自 `tremorDust`：贴地水平扩散的半透明土黄色尘雾。
     */
    class TremorDustParticle : Image(softDiscTexture()) {
        private var lifespan = 0.7f
        private var left = 0.7f
        private var vx = 0f
        private var vy = 0f
        private var startAlpha = 0.5f
        private var startSize = 3.5f
        private var endSize = 5.5f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            val angle = Random.Float((PI * 2.0).toFloat())
            val spd = Random.Float(16f, 36f)
            vx = cos(angle) * spd
            vy = sin(angle) * spd * 0.45f

            startSize = Random.Float(3f, 4.5f)
            endSize = Random.Float(4.5f, 6.5f)
            startAlpha = Random.Float(0.32f, 0.58f)
            lifespan = Random.Float(0.35f, 0.85f)
            left = lifespan

            x = cx - width / 2f
            y = cy - height / 2f
            hardlight(0x5C4228)
            alpha(startAlpha)
            scale.set(startSize / max(1f, width))
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }
            val damp = Math.pow(0.05, dt.toDouble()).toFloat()
            vx *= damp
            vy *= damp
            x += vx * dt
            y += vy * dt

            val p = 1f - left / lifespan
            val s = (startSize + (endSize - startSize) * p) / max(1f, width)
            scale.set(s)
            hardlight(ColorMath.interpolate(0x5C4228, 0x7A624E, p))
            alpha(startAlpha * (1f - p))
        }
    }

    /**
     * 移植自 `tremorRumble`：向上翻涌膨胀并消散的尘云。
     */
    class TremorRumbleParticle : Image(softDiscTexture()) {
        private var lifespan = 0.75f
        private var left = 0.75f
        private var vx = 0f
        private var vy = 0f
        private var startAlpha = 0.45f
        private var startSize = 4f
        private var endSize = 11f
        private var delay = 0f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            vx = Random.Float(-12f, 12f)
            vy = Random.Float(-8f, -2f)
            startSize = Random.Float(3.5f, 5f)
            endSize = Random.Float(9f, 13f)
            startAlpha = Random.Float(0.3f, 0.52f)
            lifespan = Random.Float(0.4f, 0.9f)
            left = lifespan
            delay = Random.Float(0f, 0.12f)

            x = cx + Random.Float(-4f, 4f) - width / 2f
            y = cy - height / 2f
            hardlight(0x5A4027)
            alpha(startAlpha)
            scale.set(startSize / max(1f, width))
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            if (delay > 0f) {
                delay -= dt
                return
            }
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }
            vy -= 26f * dt
            vx *= Math.pow(0.15, dt.toDouble()).toFloat()
            x += vx * dt
            y += vy * dt

            val p = 1f - left / lifespan
            val s = (startSize + (endSize - startSize) * p) / max(1f, width)
            scale.set(s)
            hardlight(ColorMath.interpolate(0x5A4027, 0x78624F, p))
            alpha(startAlpha * (1f - p))
        }
    }

    /**
     * 移植自 `burrowrock1/2` + `physobjground`：临时 8x8 棕色岩石贴图，带重力抛物线、旋转、落地弹跳与滞留淡出。
     */
    class BurrowRockParticle : Image(burrowRockTexture()) {
        private var floorY = 0f
        private var settled = false
        private var vx = 0f
        private var vy = 0f
        private var angVel = 0f
        private var left = 1.4f

        init {
            origin.set(width / 2f, height / 2f)
        }

        fun reset(cx: Float, cy: Float) {
            revive()
            x = cx + Random.Float(-3f, 3f) - width / 2f
            y = cy - Random.Float(0f, 2f) - height / 2f
            floorY = cy + Random.Float(-1.5f, 3f) - height / 2f
            settled = false

            val s = Random.Float(0.65f, 1.05f)
            scale.set(s)
            hardlight(ColorMath.interpolate(0x7A5432, 0x563A20, Random.Float(0f, 1f)))
            left = Random.Float(1.0f, 1.8f)

            vx = Random.Float(-36f, 36f)
            vy = Random.Float(-62f, -28f)
            angVel = Random.Float(-360f, 360f)
            angle = Random.Float(360f)
            alpha(1f)
        }

        override fun update() {
            super.update()
            val dt = Game.elapsed
            left -= dt
            if (left <= 0f) {
                kill()
                return
            }

            if (!settled) {
                vy += 175f * dt
                x += vx * dt
                y += vy * dt
                angle += angVel * dt

                if (y >= floorY && vy > 0f) {
                    y = floorY
                    if (vy > 22f) {
                        vy = -vy * 0.28f
                        vx *= 0.55f
                        angVel *= 0.45f
                    } else {
                        vx = 0f
                        vy = 0f
                        angVel = 0f
                        settled = true
                    }
                } else {
                    vx *= Math.pow(0.35, dt.toDouble()).toFloat()
                }
            }

            if (left < 0.4f) {
                alpha(max(0f, left / 0.4f))
            }
        }
    }

    companion object {
        private const val RING_TEX_KEY = "dpd_emerge_ring_tex"
        private const val DISC_TEX_KEY = "dpd_emerge_disc_tex"
        private const val BURROW_ROCK_TEX_KEY = "dpd_burrow_rock_tex"
        private const val DEFAULT_COLOR = 0x9933FF

        internal fun burrowRockTexture(): SmartTexture {
            if (!TextureCache.contains(BURROW_ROCK_TEX_KEY)) {
                val size = 8
                val pix = Pixmap(size, size, Pixmap.Format.RGBA8888)
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val isBorder = x == 0 || y == 0 || x == size - 1 || y == size - 1
                        val isHighlight = (x == 1 && y in 1..2) || (y == 1 && x in 1..2)
                        when {
                            isBorder -> pix.setColor(0.38f, 0.24f, 0.13f, 1f)
                            isHighlight -> pix.setColor(0.68f, 0.48f, 0.30f, 1f)
                            else -> pix.setColor(0.54f, 0.36f, 0.21f, 1f)
                        }
                        pix.drawPixel(x, y)
                    }
                }
                val tex = SmartTexture(pix)
                pix.dispose()
                TextureCache.add(BURROW_ROCK_TEX_KEY, tex)
            }
            return TextureCache.get(BURROW_ROCK_TEX_KEY)
        }

        internal fun ringTexture(): SmartTexture {
            if (!TextureCache.contains(RING_TEX_KEY)) {
                val size = 32
                val half = (size - 1) / 2f
                val pix = Pixmap(size, size, Pixmap.Format.RGBA8888)
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val dx = (x - half) / half
                        val dy = (y - half) / half
                        val d = sqrt(dx * dx + dy * dy)
                        val band = max(0f, 1f - abs(d - 0.78f) / 0.22f)
                        pix.setColor(1f, 1f, 1f, band * band)
                        pix.drawPixel(x, y)
                    }
                }
                val tex = SmartTexture(pix)
                pix.dispose()
                TextureCache.add(RING_TEX_KEY, tex)
            }
            return TextureCache.get(RING_TEX_KEY)
        }

        internal fun softDiscTexture(): SmartTexture {
            if (!TextureCache.contains(DISC_TEX_KEY)) {
                val size = 24
                val half = (size - 1) / 2f
                val pix = Pixmap(size, size, Pixmap.Format.RGBA8888)
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val dx = (x - half) / half
                        val dy = (y - half) / half
                        val d = sqrt(dx * dx + dy * dy)
                        val falloff = max(0f, 1f - d)
                        pix.setColor(1f, 1f, 1f, falloff * falloff)
                        pix.drawPixel(x, y)
                    }
                }
                val tex = SmartTexture(pix)
                pix.dispose()
                TextureCache.add(DISC_TEX_KEY, tex)
            }
            return TextureCache.get(DISC_TEX_KEY)
        }

        @JvmStatic
        @JvmOverloads
        fun emerge(target: Char, duration: Float = 0.48f, color: Int = DEFAULT_COLOR) {
            if (!Dungeon.visible[target.pos]) return
            Game.runOnRenderThread {
                if (!target.hasSprite) return@runOnRenderThread
                val sprite = target.sprite
                val parent = sprite.parent ?: return@runOnRenderThread
                parent.add(UndeadEmergeEffect(sprite, target.pos, duration, color))

                // 瞬时紫色地光（复用 LightSource）
                Dungeon.level.addTransientLight(
                    LightSource.fromColor(target.pos, color, 3f, 0.7f, duration + 0.15f)
                )
            }
        }

        @JvmStatic
        @JvmOverloads
        fun emergeAt(cell: Int, duration: Float = 0.48f, color: Int = DEFAULT_COLOR) {
            Game.runOnRenderThread {
                val parent = CellEmitter.get(cell).parent ?: return@runOnRenderThread
                parent.add(UndeadEmergeEffect(null, cell, duration, color))
                Dungeon.level.addTransientLight(
                    LightSource.fromColor(cell, color, 3f, 0.7f, duration + 0.15f)
                )
            }
        }
    }
}
