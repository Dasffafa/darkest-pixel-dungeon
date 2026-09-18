package com.egoal.darkestpixeldungeon.ui

import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.watabou.noosa.RenderedText
import com.watabou.noosa.ui.Component

/**
 * A lightweight, Kotlin port of Shattered Pixel Dungeon's RenderedTextBlock.
 *
 * Splits a string into [RenderedText] words so they can be scaled, tinted and
 * aligned as a block. Word wrapping and inline highlighting are intentionally
 * omitted: damage numbers and short status strings are single-line.
 */
open class RenderedTextBlock(private var textSize: Int) : Component() {

    var nLines = 1
        private set

    var text: String? = null
        private set

    protected val words = ArrayList<RenderedText>()

    private var zoom = 1f
    private var color = -1
    private var alignment = LEFT_ALIGN

    fun text(text: String?) {
        this.text = text
        build()
    }

    fun size(size: Int) {
        if (textSize != size) {
            textSize = size
            if (text != null) build()
        }
    }

    protected fun build() {
        clear()
        words.clear()
        val source = text
        if (source.isNullOrEmpty()) {
            width = 0f
            height = 0f
            return
        }

        var maxHeight = 0f
        for (token in source.split(" ")) {
            if (token.isEmpty()) {
                words.add(SPACE)
            } else {
                val word = RenderedText(token, textSize)
                if (color != -1) word.hardlight(color)
                word.scale.set(zoom)
                words.add(word)
                add(word)
                if (word.height() > maxHeight) maxHeight = word.height()
            }
        }
        height = maxHeight
        layout()
    }

    fun zoom(zoom: Float) {
        this.zoom = zoom
        for (word in words) word.scale.set(zoom)
        layout()
    }

    fun hardlight(color: Int) {
        this.color = color
        for (word in words) word.hardlight(color)
    }

    fun alpha(value: Float) {
        for (word in words) {
            if (word !== SPACE && word !== NEWLINE) word.alpha(value)
        }
    }

    fun align(align: Int) {
        alignment = align
        layout()
    }

    /** Moves every rendered word vertically, ignoring the shared sentinels. */
    fun shiftWords(dy: Float) {
        for (word in words) {
            if (word !== SPACE && word !== NEWLINE) word.y += dy
        }
    }

    /** Moves the whole block (origin + words) vertically. */
    fun shift(dy: Float) {
        y += dy
        shiftWords(dy)
    }

    override fun layout() {
        super.layout()
        var x = this.x
        var y = this.y
        var lineHeight = 0f
        nLines = 1
        var maxWidth = 0f

        // inclusive start / exclusive end of each rendered line
        val lines = ArrayList<IntArray>()
        var lineStart = 0

        for (i in words.indices) {
            val word = words[i]
            if (word === SPACE) {
                x += 1.667f
            } else if (word === NEWLINE) {
                lines.add(intArrayOf(lineStart, i))
                lineStart = i + 1
                y += lineHeight + 2f
                x = this.x
                nLines++
                lineHeight = 0f
            } else {
                if (word.height() > lineHeight) lineHeight = word.height()
                word.x = x
                word.y = y
                PixelScene.align(word)
                x += word.width()
                if (x - this.x > maxWidth) maxWidth = x - this.x
                x -= 0.667f
            }
        }
        lines.add(intArrayOf(lineStart, words.size))
        width = maxWidth
        height = (y - this.y) + lineHeight

        if (alignment != LEFT_ALIGN) {
            for (line in lines) {
                var lineWidth = 0f
                for (i in line[0] until line[1]) {
                    val word = words[i]
                    if (word !== SPACE) lineWidth = word.x + word.width() - this.x
                }
                if (lineWidth <= 0f) continue
                val shift = when (alignment) {
                    CENTER_ALIGN -> (width() - lineWidth) / 2f
                    RIGHT_ALIGN -> width() - lineWidth
                    else -> 0f
                }
                for (i in line[0] until line[1]) {
                    val word = words[i]
                    if (word !== SPACE) {
                        word.x += shift
                        PixelScene.align(word)
                    }
                }
            }
        }
    }

    companion object {
        private val SPACE = RenderedText()
        private val NEWLINE = RenderedText()

        const val LEFT_ALIGN = 1
        const val CENTER_ALIGN = 2
        const val RIGHT_ALIGN = 3
    }
}
