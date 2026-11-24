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
 * 移植自 Wayward Souls `earthStomp1` / `earthStomp2`：
 * 棕黄色 `earth0Shock` 冲击波 + 扩散冲击环 + `tremorDust` / `tremorRumble` 尘雾 + `burrowrock` 8x8 弹跳碎石。
 */
class EarthStompEffect private constructor(cell: Int, color: Int) : Group() {

    private val dustGroup = Group()
    private val shockGroup = AdditiveGroup()

    init {
        val c = DungeonTilemap.tileCenterToWorld(cell)
        val cx = c.x
        val cy = c.y + DungeonTilemap.SIZE / 2f - 3f

        add(dustGroup)
        add(shockGroup)

        for (i in 0 until 10) {
            (dustGroup.recycle(UndeadEmergeEffect.TremorDustParticle::class.java) as UndeadEmergeEffect.TremorDustParticle)
                .reset(cx, cy)
        }
        for (i in 0 until 7) {
            (dustGroup.recycle(UndeadEmergeEffect.TremorRumbleParticle::class.java) as UndeadEmergeEffect.TremorRumbleParticle)
                .reset(cx, cy)
        }
        for (i in 0 until 9) {
            (dustGroup.recycle(UndeadEmergeEffect.BurrowRockParticle::class.java) as UndeadEmergeEffect.BurrowRockParticle)
                .reset(cx, cy)
        }

        for (i in 0 until 18) {
            (shockGroup.recycle(UndeadEmergeEffect.ShadowShockParticle::class.java) as UndeadEmergeEffect.ShadowShockParticle)
                .reset(cx, cy, color)
        }
        shockGroup.add(UndeadEmergeEffect.RingVisual(cx, cy, color, 20f, 0.36f))
        shockGroup.add(UndeadEmergeEffect.RingVisual(cx, cy, 0xFFBB55, 14f, 0.28f))
    }

    override fun update() {
        super.update()
        if (dustGroup.countLiving() == 0 && shockGroup.countLiving() == 0) {
            killAndErase()
        }
    }

    private class AdditiveGroup : Group() {
        override fun draw() {
            Gdx.gl.glBlendFuncSeparate(
                GL20.GL_SRC_ALPHA, GL20.GL_ONE,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA
            )
            super.draw()
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun play(cell: Int, color: Int = 0xCC8833) {
            Game.runOnRenderThread {
                val parent = CellEmitter.get(cell).parent ?: return@runOnRenderThread
                parent.add(EarthStompEffect(cell, color))
                Dungeon.level.addTransientLight(
                    LightSource.fromColor(cell, color, 3f, 0.65f, 0.45f)
                )
            }
        }
    }
}
