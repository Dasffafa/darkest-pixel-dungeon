/*
 * GPL-3.0-or-later. Adapted from Ling's ColdSnowParticles implementation.
 */
package com.egoal.darkestpixeldungeon.effects.particles

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.particles.Emitter
import com.watabou.noosa.particles.PixelParticle
import com.watabou.utils.Random

/** Prototype for quiet, ground-anchored snow. Rendering flip is exposed as faceAmount. */
class ColdSnowParticles : PixelParticle() {
    private var size = 1f
    private var facePhase = 0f
    private var faceSpeed = 1f
    private var windPhase = 0f
    private var age = 0f
    private var originX = 0f
    private var originY = 0f
    private var fallHeight = 0f
    private var drift = 0f
    private var depth = 0f
    private var depthSpeed = 0f
    private var baseScale = 1f
    private var phaseA = 0f
    private var phaseB = 0f

    init {
        texture("snowflake.png")
        lifespan = Random.Float(1.6f, 2.8f)
        size = Random.Float(.65f, 1.15f)
        scale.set(size)
        origin.set(width / 2f, height / 2f)
    }

    fun reset(x: Float, y: Float) {
        revive()
        this.x = x
        this.y = y
        originX = x
        originY = y
        age = 0f
        fallHeight = Random.Float(18f, 34f)
        drift = Random.Float(5f, 12f)
        // Camera distance is sampled at spawn; near flakes are larger and move more visibly.
        depth = Random.Float(.2f, 1f)
        depthSpeed = Random.Float(-.035f, .035f)
        baseScale = .45f + depth * .8f
        phaseA = Random.Float(0f, 6.283185f)
        phaseB = Random.Float(0f, 6.283185f)
        left = Random.Float(1.6f, 2.8f).also { lifespan = it }
        size = Random.Float(.65f, 1.15f)
        scale.set(size)
        facePhase = Random.Float(0f, 6.283185f)
        faceSpeed = Random.Float(.8f, 1.35f)
        windPhase = Random.Float(0f, 6.283185f)
        speed.set(0f, 0f)
        am = 0f
        color(0xC5F2F0FF.toInt())
    }

    /** 0 at edge-on flip, 1 when the flake faces the camera. */
    fun faceAmount(): Float = kotlin.math.abs(kotlin.math.cos(facePhase)).coerceAtLeast(.025f)

    override fun update() {
        super.update()
        age += Game.elapsed
        depth = (depth + depthSpeed * Game.elapsed).coerceIn(.12f, 1.1f)
        facePhase += Game.elapsed * faceSpeed
        windPhase += Game.elapsed * .35f
        val t = (age / lifespan).coerceIn(0f, 1f)
        val eased = t * t * (3f - 2f * t)
        val breezeA = kotlin.math.sin(windPhase + t * 2.2f) * drift * .55f
        val breezeB = kotlin.math.sin(phaseA + t * 4.7f) * drift * .25f
        val breezeC = kotlin.math.sin(phaseB + t * 8.1f) * drift * .12f
        x = originX + (breezeA + breezeB + breezeC) * depth * t
        y = originY + fallHeight * eased + kotlin.math.sin(phaseB + t * 5.5f) * depth * .8f
        val perspectiveScale = baseScale * (.72f + depth * .48f)
        scale.x = size * perspectiveScale * faceAmount()
        scale.y = size * perspectiveScale
        val p = left / lifespan
        am = (if (p < .5f) p else 1f - p) * 1.2f
    }

    /** Per-cell controller that keeps snow tied to world-space ground tiles. */
    class Snow(private val pos: Int) : Group() {
        private val point = DungeonTilemap.tileToWorld(pos)
        private var delay = Random.Float(0f, 3f)

        override fun update() {
            super.update()
            val inView = pos in Dungeon.visible.indices && Dungeon.visible[pos]
            visible = inView
            if (!inView) return

            delay -= Game.elapsed
            if (delay <= 0f) {
                delay = Random.Float(3f, 6f)
                val particle = recycle(ColdSnowParticles::class.java) as ColdSnowParticles
                particle.reset(point.x + Random.Float(3f, 13f), point.y + Random.Float(3f, 13f))
            }
        }
    }

    companion object {
        @JvmField
        val FACTORY = object : Emitter.Factory() {
            override fun emit(emitter: Emitter, index: Int, x: Float, y: Float) {
                (emitter.recycle(ColdSnowParticles::class.java) as ColdSnowParticles).reset(x, y)
            }
        }
    }
}
