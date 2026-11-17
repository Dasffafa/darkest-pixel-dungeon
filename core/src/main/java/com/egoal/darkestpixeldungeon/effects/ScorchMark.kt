package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Game
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.utils.Random

class ScorchMark private constructor(cell: Int) : Image(TextureCache.createSolid(0xFF18100C.toInt())) {

    private var timeLeft = DURATION

    init {
        val center = DungeonTilemap.tileCenterToWorld(cell)
        val size = Random.Float(8f, 13f)
        scale.set(size, size * 0.72f)
        origin.set(0.5f, 0.5f)
        x = center.x - 0.5f + Random.Float(-2f, 2f)
        y = center.y - 0.5f + Random.Float(-1f, 3f)
        angle = Random.Float(360f)
        alpha(INITIAL_ALPHA)
    }

    override fun update() {
        super.update()
        timeLeft -= Game.elapsed
        if (timeLeft <= 0f) {
            killAndErase()
        } else {
            alpha(INITIAL_ALPHA * (timeLeft / DURATION))
        }
    }

    companion object {
        private const val DURATION = 2.6f
        private const val INITIAL_ALPHA = 0.38f

        @JvmStatic
        fun place(group: Group?, cell: Int) {
            if (group == null || !Dungeon.level.insideMap(cell)) return
            group.addToBack(ScorchMark(cell))
        }
    }
}
