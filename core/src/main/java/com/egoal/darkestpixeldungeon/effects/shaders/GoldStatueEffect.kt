package com.egoal.darkestpixeldungeon.effects.shaders

import com.egoal.darkestpixeldungeon.actors.Char
import com.egoal.darkestpixeldungeon.sprites.CharSprite
import com.watabou.glscripts.Script
import com.watabou.noosa.Game
import com.watabou.noosa.NoosaScript
import kotlin.math.max
import kotlin.math.min

class GoldStatueScript : ResourceNoosaScript("gold_statue") {
    private val uFrame = uniform("uFrame")
    private val uAlpha = uniform("uAlpha")

    fun prepare(left: Float, top: Float, width: Float, height: Float, progress: Float) {
        uFrame.value4f(left, top, width, height)
        uAlpha.value1f(progress)
    }
}

class GoldStatueEffect(
    private val duration: Float = 0.7f,
    private val onComplete: (() -> Unit)? = null
) : SpriteShaderEffect {

    private var elapsed = 0f
    private var progress = 0f

    override fun update(sprite: CharSprite, elapsed: Float) {
        this.elapsed += elapsed
        progress = min(this.elapsed / max(duration, 0.001f), 1f)
        if (progress >= 1f) {
            sprite.finishShaderDeath(this)
            onComplete?.invoke()
        }
    }

    override fun prepare(sprite: CharSprite): NoosaScript {
        val frame = sprite.frame()
        return Script.use(GoldStatueScript::class.java).also {
            it.prepare(frame.left, frame.top, frame.width(), frame.height(), progress)
        }
    }

    companion object {
        fun play(target: Char, duration: Float = 0.7f, onComplete: (() -> Unit)? = null): Boolean {
            if (!target.hasSprite || target.sprite.parent == null) return false
            Game.runOnRenderThreadAndWait {
                if (target.sprite.parent != null) {
                    target.sprite.beginShaderDeath(GoldStatueEffect(duration, onComplete))
                }
            }
            return target.sprite.hasPendingShaderDeath
        }
    }
}
