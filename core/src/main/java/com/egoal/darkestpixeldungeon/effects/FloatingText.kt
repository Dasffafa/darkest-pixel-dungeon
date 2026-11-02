/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.egoal.darkestpixeldungeon.effects

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.DungeonTilemap
import com.egoal.darkestpixeldungeon.scenes.GameScene
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.ui.RenderedTextBlock
import com.watabou.noosa.Camera
import com.watabou.noosa.Game
import com.watabou.noosa.Image
import com.watabou.noosa.TextureFilm
import com.watabou.noosa.ui.Component
import com.watabou.utils.SparseArray
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A single floating damage/status line.
 *
 * It can hold several [Entry] segments rendered side by side (e.g. physical
 * damage plus elemental damage), each with its own icon and colour. The whole
 * line shares one log-scaled font size based on the total value.
 */
class FloatingText : Component() {

    data class Entry(
            val text: String,
            val color: Int,
            val icon: Int,
            val value: Int,
            val status: Boolean = false,
            val elementIcon: Boolean = false)

    private class Part(val block: RenderedTextBlock, val icon: Image?)

    private val parts = ArrayList<Part>()

    private var scale = 1f
    private var timeLeft = LIFESPAN
    private var key = -1

    override fun update() {
        super.update()
        // >= (not >) so a text nudged to exactly 0 by push() still expires
        // instead of freezing on screen forever.
        if (timeLeft >= 0f) {
            timeLeft -= Game.elapsed
            if (timeLeft <= 0f) {
                kill()
                return
            }
            val p = timeLeft / LIFESPAN
            val a = if (p > 0.5f) 1f else p * 2f
            val yMove = DISTANCE / LIFESPAN * Game.elapsed
            y -= yMove
            for (part in parts) {
                part.block.alpha(a)
                part.block.shift(-yMove)
                part.icon?.let {
                    it.alpha(a)
                    it.y -= yMove
                }
            }
        }
    }

    override fun kill() {
        if (key != -1) {
            stacks[key]?.remove(this)
            key = -1
        }
        super.kill()
    }

    override fun destroy() {
        kill()
        super.destroy()
    }

    override fun layout() {
        super.layout()
        var x = left()
        var maxHeight = 0f
        for (part in parts) {
            val block = part.block
            val lineHeight = block.height()
            part.icon?.let {
                it.x = x + ICON_OFFSET_X * scale
                it.y = top()
                PixelScene.align(it)
                x += it.width() + ICON_GAP * scale
            }
            block.setPos(x, top())
            x += block.width() + SEGMENT_GAP * scale
            if (lineHeight > maxHeight) maxHeight = lineHeight
        }
        if (parts.isNotEmpty()) x -= SEGMENT_GAP * scale
        width = x - left()
        height = maxHeight
    }

    private fun reset(x: Float, y: Float, entries: List<Entry>, total: Int, baseSize: Int) {
        revive()

        clear()
        parts.clear()

        val camZoom = Camera.main.zoom
        val factor = scaleFactor(total)
        scale = factor
        val fontPx = (baseSize * camZoom * factor).roundToInt()
        val wordScale = 1f / camZoom

        for (entry in entries) {
            if (!entry.status && entry.value <= 0) continue

            val block = RenderedTextBlock(fontPx)
            block.zoom(wordScale)
            block.text(entry.text)
            block.hardlight(entry.color)

            val icon = makeIcon(entry, factor)

            add(block)
            if (icon != null) add(icon)
            parts.add(Part(block, icon))
        }

        if (parts.isEmpty()) {
            kill()
            return
        }

        layout()
        setPos(
            PixelScene.align(Camera.main, x - width() / 2f),
            PixelScene.align(Camera.main, y - height())
        )

        timeLeft = LIFESPAN
    }

    private fun makeIcon(entry: Entry, factor: Float): Image? {
        if (entry.icon == NO_ICON) return null

        val icon = if (entry.elementIcon) {
            Image(Assets.DPD_CONS_ICONS).apply {
                frame(ELEM_ICON_SIZE * entry.icon, ELEM_ICON_ROW, ELEM_ICON_SIZE, ELEM_ICON_SIZE)
            }
        } else {
            Image(Assets.TEXT_ICONS).apply { frame(iconFilm.get(entry.icon)) }
        }
        icon.scale.set(factor)
        return icon
    }

    companion object {
        private const val LIFESPAN = 1f
        private val DISTANCE = DungeonTilemap.SIZE.toFloat()

        /** Base font size for plain text status lines. */
        const val SIZE_STATUS = 9

        /** Base font size for numeric damage/heal lines, matching the 7px icons. */
        const val SIZE_NUMERIC = 7

        private const val ICON_SIZE = 7
        private const val ICON_GAP = 1f
        private const val SEGMENT_GAP = 3f
        // nudge all icons one base-scale cell to the right
        private const val ICON_OFFSET_X = 1f
        private const val SCALE_THRESHOLD = 100

        private val stacks = SparseArray<ArrayList<FloatingText>>()

        private val iconFilm: TextureFilm by lazy {
            TextureFilm(Assets.TEXT_ICONS, ICON_SIZE, ICON_SIZE)
        }

        const val NO_ICON = -1

        // physical / magical
        const val PHYS_DMG = 0
        const val PHYS_DMG_NO_ARMOR = 1
        const val MAGIC_DMG = 2

        // elements are drawn from the DPD_CONS_ICONS element row, the same
        // icons the resistance panel uses; Entry.icon is then the element
        // ordinal (Damage.Element order) and Entry.elementIcon is true.
        private const val ELEM_ICON_SIZE = 8
        private const val ELEM_ICON_ROW = 16

        // debuff / damage-over-time
        const val BLEEDING = 10
        const val OOZE = 14

        // special sources
        const val HUNGER = 5

        // positive
        const val HEALING = 18

        // critical
        const val CRIT = 25
        const val CRIT_NO_ARMOR = 26

        /**
         * Log scaling for large numbers. Values at or below [SCALE_THRESHOLD]
         * render at 1x; above it the factor grows smoothly from 1x
         * (100: 1.0x, 1000: 1.5x, 10000: 2.0x), uncapped.
         */
        fun scaleFactor(value: Int): Float =
            if (value > SCALE_THRESHOLD) {
                1f + 0.5f * log10(value / SCALE_THRESHOLD.toFloat())
            } else 1f

        fun showDmg(
            x: Float,
            y: Float,
            key: Int,
            entries: List<Entry>,
            total: Int,
            shake: Float,
            shakeDuration: Float,
            baseSize: Int
        ) {
            if (entries.none { it.status || it.value > 0 }) return
            Game.runOnRenderThread {
                val txt = GameScene.status() ?: return@runOnRenderThread
                txt.reset(x, y, entries, total, baseSize)
                if (key != -1) push(txt, key)
                if (shake > 0f) Camera.main.shake(shake, shakeDuration)
            }
        }

        private fun push(txt: FloatingText, key: Int) {
            txt.key = key
            var stack = stacks[key]
            if (stack == null) {
                stack = ArrayList()
                stacks.put(key, stack)
            }
            if (stack.isNotEmpty()) {
                var below = txt
                var aboveIndex = stack.size - 1
                var numBelow = 0
                while (aboveIndex >= 0) {
                    numBelow++
                    val above = stack[aboveIndex]
                    if (above.bottom() + 4f > below.top()) {
                        above.setPos(above.left(), below.top() - above.height() - 4f)
                        above.timeLeft = minOf(above.timeLeft, LIFESPAN - numBelow / 5f)
                        above.timeLeft = max(above.timeLeft, 0f)
                        below = above
                        aboveIndex--
                    } else {
                        break
                    }
                }
            }
            stack.add(txt)
        }
    }
}
