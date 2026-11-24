package com.egoal.darkestpixeldungeon.effects

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.levels.lighting.LightSource
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.utils.Random

/**
 * 移植自 Wayward Souls `paladinRevive` / `cultistRevive` / `shadowFountainShock`：
 * 通用色系冲击能量团 + 多道错开扩张光环 + 瞬时动态光。
 */
class ShockBurstEffect private constructor(
    cell: Int,
    private val color: Int
) : Group() {

    private val c = DungeonTilemap.tileCenterToWorld(cell)
    private val cx = c.x
    private val cy = c.y + DungeonTilemap.SIZE / 2f - 3f
    private var elapsed = 0f
    private var ringsSpawned = 1

    init {
        for (i in 0 until 16) {
            (recycle(UndeadEmergeEffect.ShadowShockParticle::class.java) as UndeadEmergeEffect.ShadowShockParticle)
                .reset(cx, cy, color)
        }
        add(UndeadEmergeEffect.RingVisual(cx, cy, color, 18f, 0.42f))
    }

    override fun update() {
        super.update()
        elapsed += Game.elapsed
        if (ringsSpawned < 3 && elapsed >= ringsSpawned * 0.11f) {
            ringsSpawned++
            add(UndeadEmergeEffect.RingVisual(cx, cy, color, Random.Float(13f, 20f), Random.Float(0.32f, 0.48f)))
        }
        if (ringsSpawned >= 3 && countLiving() == 0) {
            killAndErase()
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

    companion object {
        @JvmStatic
        @JvmOverloads
        fun play(cell: Int, color: Int = 0xFFD24A) {
            Game.runOnRenderThread {
                val parent = CellEmitter.get(cell).parent ?: return@runOnRenderThread
                parent.add(ShockBurstEffect(cell, color))
                Dungeon.level.addTransientLight(
                    LightSource.fromColor(cell, color, 3f, 0.75f, 0.5f)
                )
            }
        }
    }
}
