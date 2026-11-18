package com.egoal.darkestpixeldungeon.levels.lighting

import com.egoal.darkestpixeldungeon.levels.Level
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class LightMap {
    private var width: Int = 0
    private var height: Int = 0
    private var length: Int = 0

    var ambient: AmbientLightProfile = AmbientLightProfile(0.32f, 0.35f, 0.42f)
        private set

    private val staticSources = ArrayList<LightSource>()
    private val transientSources = ArrayList<LightSource>()

    private var baseR = FloatArray(0)
    private var baseG = FloatArray(0)
    private var baseB = FloatArray(0)

    var r = FloatArray(0)
        private set
    var g = FloatArray(0)
        private set
    var b = FloatArray(0)
        private set
    var luminance = FloatArray(0)
        private set

    private var elapsedTime = 0f
    private var lastHeroPos = -1
    private var lastHeroTorch = false
    private var frameDirty = true

    fun resize(w: Int, h: Int) {
        if (width == w && height == h && length == w * h) return
        width = w
        height = h
        length = w * h
        baseR = FloatArray(length)
        baseG = FloatArray(length)
        baseB = FloatArray(length)
        r = FloatArray(length)
        g = FloatArray(length)
        b = FloatArray(length)
        luminance = FloatArray(length)
        frameDirty = true
    }

    fun setStaticSources(sources: List<LightSource>, ambientProfile: AmbientLightProfile, losBlocking: BooleanArray) {
        ambient = ambientProfile
        staticSources.clear()
        staticSources.addAll(sources)
        rebuildStaticBase(losBlocking)
        frameDirty = true
    }

    fun addTransient(source: LightSource) {
        synchronized(transientSources) {
            transientSources.add(source)
        }
        frameDirty = true
    }

    fun clearTransient() {
        synchronized(transientSources) {
            transientSources.clear()
        }
        frameDirty = true
    }

    fun hasAnimatedLights(): Boolean {
        if (transientSources.isNotEmpty()) return true
        for (i in 0 until staticSources.size) {
            if (staticSources[i].flickerAmp > 0f) return true
        }
        return false
    }

    fun tick(dt: Float): Boolean {
        elapsedTime += dt
        var changed = false
        synchronized(transientSources) {
            val iter = transientSources.iterator()
            while (iter.hasNext()) {
                val src = iter.next()
                src.elapsed += dt
                changed = true
                if (src.isExpired) iter.remove()
            }
        }
        if (changed || hasAnimatedLights()) {
            frameDirty = true
            return true
        }
        return false
    }

    fun updateFrame(
        heroPos: Int,
        heroViewDist: Int,
        heroTorchLit: Boolean,
        losBlocking: BooleanArray,
        outLighted: BooleanArray?
    ): Boolean {
        if (!frameDirty && heroPos == lastHeroPos && heroTorchLit == lastHeroTorch) {
            return false
        }
        lastHeroPos = heroPos
        lastHeroTorch = heroTorchLit
        frameDirty = false

        if (length == 0) return false

        System.arraycopy(baseR, 0, r, 0, length)
        System.arraycopy(baseG, 0, g, 0, length)
        System.arraycopy(baseB, 0, b, 0, length)

        // Apply subtle flicker modulation from static flickering sources
        for (i in 0 until staticSources.size) {
            val src = staticSources[i]
            if (src.flickerAmp > 0f) {
                val wave = sin((elapsedTime * src.flickerSpeed + src.phase).toDouble()).toFloat()
                val modSrc = src.copy(
                    intensity = src.intensity * (wave * src.flickerAmp),
                    radius = max(1f, src.radius + wave * src.flickerRadius)
                )
                accumulateSource(modSrc, losBlocking, r, g, b)
            }
        }

        // Hero personal aura (warm dim glow normally, brighter when torch is active)
        if (heroPos in 0 until length) {
            val heroRadius = if (heroTorchLit) max(4.5f, heroViewDist.toFloat()) else 2.4f
            val heroIntensity = if (heroTorchLit) 0.78f else 0.36f
            val heroLight = LightSource(
                cell = heroPos,
                colorR = 1.0f,
                colorG = if (heroTorchLit) 0.78f else 0.72f,
                colorB = if (heroTorchLit) 0.48f else 0.56f,
                radius = heroRadius,
                intensity = heroIntensity,
                blocksLos = true
            )
            accumulateSource(heroLight, losBlocking, r, g, b)
        }

        // Transient spell / projectile / explosion lights
        synchronized(transientSources) {
            for (i in 0 until transientSources.size) {
                val src = transientSources[i]
                val scale = src.currentScale()
                if (scale > 0f) {
                    accumulateSource(
                        src.copy(intensity = src.intensity * scale),
                        losBlocking, r, g, b
                    )
                }
            }
        }

        // Compute luminance and sync logical Level.lighted
        for (i in 0 until length) {
            val cr = r[i].coerceIn(0f, 1.35f)
            val cg = g[i].coerceIn(0f, 1.35f)
            val cb = b[i].coerceIn(0f, 1.35f)
            r[i] = cr
            g[i] = cg
            b[i] = cb
            val lum = 0.299f * cr + 0.587f * cg + 0.114f * cb
            luminance[i] = lum
            if (outLighted != null && i < outLighted.size) {
                outLighted[i] = lum >= LIGHT_FOV_THRESHOLD
            }
        }
        return true
    }

    private fun rebuildStaticBase(losBlocking: BooleanArray) {
        if (length == 0) return
        for (i in 0 until length) {
            baseR[i] = ambient.r
            baseG[i] = ambient.g
            baseB[i] = ambient.b
        }
        for (i in 0 until staticSources.size) {
            accumulateSource(staticSources[i], losBlocking, baseR, baseG, baseB)
        }
    }

    private fun accumulateSource(
        src: LightSource,
        losBlocking: BooleanArray,
        dstR: FloatArray,
        dstG: FloatArray,
        dstB: FloatArray
    ) {
        if (src.cell < 0 || src.cell >= length || src.radius <= 0f) return
        val sx = src.cell % width
        val sy = src.cell / width
        val rInt = ceil(src.radius.toDouble()).toInt()

        val x0 = max(0, sx - rInt)
        val x1 = min(width - 1, sx + rInt)
        val y0 = max(0, sy - rInt)
        val y1 = min(height - 1, sy + rInt)
        val rSq = src.radius * src.radius
        val mountedInWall = src.blocksLos && src.cell in losBlocking.indices && losBlocking[src.cell]
        val openFaces = if (mountedInWall) collectOpenFaces(sx, sy, losBlocking) else emptyList()

        for (y in y0..y1) {
            val dy = y - sy
            val dySq = dy * dy
            var cell = y * width + x0
            for (x in x0..x1) {
                val dx = x - sx
                val distSq = (dx * dx + dySq).toFloat()
                if (distSq <= rSq) {
                    val lit = when {
                        !src.blocksLos -> true
                        cell == src.cell -> true
                        mountedInWall -> hasWallLightLos(sx, sy, x, y, openFaces, losBlocking)
                        else -> hasLineOfSight(sx, sy, x, y, losBlocking)
                    }
                    if (lit) {
                        val t = 1f - sqrt(distSq) / src.radius
                        val falloff = t * t * (3f - 2f * t) * src.intensity // smoothstep attenuation
                        dstR[cell] += src.colorR * falloff
                        dstG[cell] += src.colorG * falloff
                        dstB[cell] += src.colorB * falloff
                    }
                }
                cell++
            }
        }
    }

    private fun collectOpenFaces(sx: Int, sy: Int, losBlocking: BooleanArray): List<Pair<Int, Int>> {
        val cardinal = ArrayList<Pair<Int, Int>>(4)
        for ((ox, oy) in CARDINAL_DIRS) {
            val nx = sx + ox
            val ny = sy + oy
            if (nx in 0 until width && ny in 0 until height && !losBlocking[ny * width + nx]) {
                cardinal.add(ox to oy)
            }
        }
        if (cardinal.isNotEmpty()) return cardinal
        // Corner wall sconce (e.g. WandDigger): fall back to diagonal open neighbor
        val diag = ArrayList<Pair<Int, Int>>(4)
        for ((ox, oy) in DIAGONAL_DIRS) {
            val nx = sx + ox
            val ny = sy + oy
            if (nx in 0 until width && ny in 0 until height && !losBlocking[ny * width + nx]) {
                diag.add(ox to oy)
            }
        }
        return diag
    }

    private fun hasWallLightLos(
        sx: Int,
        sy: Int,
        tx: Int,
        ty: Int,
        openFaces: List<Pair<Int, Int>>,
        losBlocking: BooleanArray
    ): Boolean {
        val dx = tx - sx
        val dy = ty - sy
        for ((ox, oy) in openFaces) {
            if (dx * ox + dy * oy > 0 && hasLineOfSight(sx + ox, sy + oy, tx, ty, losBlocking)) {
                return true
            }
        }
        return false
    }

    private fun hasLineOfSight(x0: Int, y0: Int, x1: Int, y1: Int, losBlocking: BooleanArray): Boolean {
        if (x0 == x1 && y0 == y1) return true
        var x = x0
        var y = y0
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            if (x == x1 && y == y1) return true
            if (!(x == x0 && y == y0)) {
                val idx = y * width + x
                if (idx in losBlocking.indices && losBlocking[idx]) return false
            }
            val e2 = err shl 1
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }

    companion object {
        const val LIGHT_FOV_THRESHOLD = 0.58f
        private val CARDINAL_DIRS = arrayOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
        private val DIAGONAL_DIRS = arrayOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)
    }
}
