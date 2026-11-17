package com.egoal.darkestpixeldungeon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.egoal.darkestpixeldungeon.actors.buffs.Light
import com.egoal.darkestpixeldungeon.levels.Level
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.glwrap.Texture
import com.watabou.noosa.Game
import com.watabou.noosa.Image
import com.watabou.noosa.LightingScript
import com.watabou.noosa.NoosaScript
import com.watabou.utils.Rect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FogOfWar(private val mapWidth: Int, private val mapHeight: Int) : Image() {

    private val pWidth: Int = mapWidth + 1
    private val pHeight: Int = mapHeight + 1

    private var width2: Int = 1
    private var height2: Int = 1

    @Volatile
    private var updated: Rect
    private var updating: Rect = Rect()
    private var flickerTimer: Float = 0f

    init {
        while (width2 < pWidth) width2 = width2 shl 1
        while (height2 < pHeight) height2 = height2 shl 1

        val size = DungeonTilemap.SIZE.toFloat()
        width = width2 * size
        height = height2 * size

        texture(FogTexture(width2, height2))
        scale.set(size, size)
        x = -size / 2f
        y = -size / 2f

        updated = Rect(0, 0, pWidth, pHeight)
    }

    @Synchronized
    fun updateFog() {
        updated.set(0, 0, pWidth, pHeight)
    }

    @Synchronized
    fun updateFog(cell: Int, radius: Int) {
        val cx = cell % mapWidth
        val cy = cell / mapWidth
        val update = Rect(
            max(0, cx - radius),
            max(0, cy - radius),
            min(mapWidth, cx + radius + 1),
            min(mapHeight, cy + radius + 1)
        )
        if (update.isEmpty) return
        updateFogArea(update.left, update.top, update.width(), update.height())
    }

    @Synchronized
    fun updateFogArea(x: Int, y: Int, w: Int, h: Int) {
        updated.union(x, y)
        updated.union(x + w + 1, y + h + 1)
        updated.left = max(0, updated.left)
        updated.top = max(0, updated.top)
        updated.right = min(pWidth, updated.right)
        updated.bottom = min(pHeight, updated.bottom)
    }

    @Synchronized
    fun moveToUpdating() {
        updating = Rect(updated)
        updated.setEmpty()
    }

    override fun update() {
        super.update()
        val level = Dungeon.level
        if (level.lightMap.tick(Game.elapsed)) {
            flickerTimer += Game.elapsed
            if (flickerTimer >= 0.066f) { // ~15 Hz lightmap refresh
                flickerTimer = 0f
                updateFog()
            }
        }
    }

    private fun updateTexture(visible: BooleanArray, visited: BooleanArray, mapped: BooleanArray) {
        moveToUpdating()
        if (updating.isEmpty) return

        val level = Dungeon.level
        val lightMap = level.lightMap
        lightMap.resize(mapWidth, mapHeight)

        val heroPos = if (!Dungeon.isHeroNull) Dungeon.hero.pos else -1
        val heroDist = if (!Dungeon.isHeroNull) Dungeon.hero.viewDistance() else 4
        val torchLit = !Dungeon.isHeroNull && Dungeon.hero.buff(Light::class.java) != null
        lightMap.updateFrame(heroPos, heroDist, torchLit, Level.losBlocking, Level.lighted)

        val lr = lightMap.r
        val lg = lightMap.g
        val lb = lightMap.b
        val len = level.length()

        val fog = texture as FogTexture
        val brightnessIdx = (DarkestPixelDungeon.brightness() + 2).coerceIn(0, 4)
        val brightnessBoost = 1.0f + DarkestPixelDungeon.brightness() * 0.14f

        for (i in updating.top until updating.bottom) {
            fog.pixels.position(width2 * i + updating.left)
            for (j in updating.left until updating.right) {
                if (i == 0 || i >= mapHeight || j == 0 || j >= mapWidth) {
                    fog.pixels.put(INVISIBLE_COLOR)
                    continue
                }

                val c0 = i * mapWidth + j
                val c1 = c0 - mapWidth
                val c2 = c0 - 1
                val c3 = c1 - 1

                var visCount = 0
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f

                if (c0 in 0 until len && visible[c0]) {
                    visCount++
                    sumR += lr[c0]; sumG += lg[c0]; sumB += lb[c0]
                }
                if (c1 in 0 until len && visible[c1]) {
                    visCount++
                    sumR += lr[c1]; sumG += lg[c1]; sumB += lb[c1]
                }
                if (c2 in 0 until len && visible[c2]) {
                    visCount++
                    sumR += lr[c2]; sumG += lg[c2]; sumB += lb[c2]
                }
                if (c3 in 0 until len && visible[c3]) {
                    visCount++
                    sumR += lr[c3]; sumG += lg[c3]; sumB += lb[c3]
                }

                if (visCount >= 1) {
                    // Any adjacent cell is visible: illuminate smoothly so visible mobs are never covered by fog.
                    val inv = brightnessBoost / visCount
                    val ri = (sumR * inv * 255f).roundToInt().coerceIn(0, 255)
                    val gi = (sumG * inv * 255f).roundToInt().coerceIn(0, 255)
                    val bi = (sumB * inv * 255f).roundToInt().coerceIn(0, 255)
                    fog.pixels.put(packRgba(ri, gi, bi, 255))
                } else if (visited[c0] && visited[c1] && visited[c2] && visited[c3]) {
                    fog.pixels.put(VISITED[brightnessIdx])
                } else if (mapped[c0] && mapped[c1] && mapped[c2] && mapped[c3]) {
                    fog.pixels.put(MAPPED[brightnessIdx])
                } else {
                    fog.pixels.put(INVISIBLE_COLOR)
                }
            }
        }

        if (updating.width() == pWidth && updating.height() == pHeight) {
            fog.update()
        } else {
            fog.update(updating.top, updating.bottom)
        }
    }

    override fun script(): NoosaScript = LightingScript.get()

    override fun draw() {
        if (!updated.isEmpty) {
            updateTexture(Dungeon.visible, Dungeon.level.visited, Dungeon.level.mapped)
            updating.setEmpty()
        }

        Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ZERO)
        super.draw()
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private inner class FogTexture(w: Int, h: Int) : SmartTexture() {
        val pixels: IntBuffer

        init {
            width = w
            height = h
            pixels = ByteBuffer
                .allocateDirect(w * h * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
            TextureCache.add(FogOfWar::class.java, this)
        }

        override fun generate() {
            id = Gdx.gl.glGenTexture()
        }

        override fun reload() {
            generate()
            update()
        }

        fun update() {
            bind()
            filter(Texture.LINEAR, Texture.LINEAR)
            pixels.position(0)
            Gdx.gl.glTexImage2D(
                GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA,
                width, height, 0, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels
            )
        }

        fun update(top: Int, bottom: Int) {
            bind()
            filter(Texture.LINEAR, Texture.LINEAR)
            pixels.position(top * width)
            Gdx.gl.glTexSubImage2D(
                GL20.GL_TEXTURE_2D, 0, 0, top,
                width, bottom - top, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels
            )
        }
    }

    companion object {
        private val IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

        private fun packRgba(r: Int, g: Int, b: Int, a: Int): Int {
            return if (IS_LITTLE_ENDIAN) {
                (a shl 24) or (b shl 16) or (g shl 8) or r
            } else {
                (r shl 24) or (g shl 16) or (b shl 8) or a
            }
        }

        private val INVISIBLE_COLOR = packRgba(0, 0, 0, 255)

        // Multiply factors for visited (dim grey-blue memory) and mapped (sepia)
        private val VISITED = intArrayOf(
            packRgba(20, 22, 28, 255),
            packRgba(35, 38, 46, 255),
            packRgba(52, 56, 66, 255),
            packRgba(75, 80, 92, 255),
            packRgba(105, 110, 122, 255)
        )

        private val MAPPED = intArrayOf(
            packRgba(45, 30, 18, 255),
            packRgba(68, 45, 25, 255),
            packRgba(92, 60, 32, 255),
            packRgba(120, 78, 42, 255),
            packRgba(150, 98, 52, 255)
        )
    }
}
