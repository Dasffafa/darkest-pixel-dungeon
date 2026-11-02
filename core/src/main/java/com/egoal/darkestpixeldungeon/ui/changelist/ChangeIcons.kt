package com.egoal.darkestpixeldungeon.ui.changelist

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.actors.buffs.Buff
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.ui.BuffIndicator
import com.egoal.darkestpixeldungeon.ui.Icons
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.Image
import com.watabou.noosa.TextureFilm

/**
 * The art the changelist draws its icons from: the game's own item sprites,
 * interface icons, and buff icons.
 */
object ChangeIcons {

    /** every actor sheet is cut into squares of this size */
    private const val SPRITE_SIZE = 16

    /** the last frame of the spinner's death: the spider squashed flat */
    private const val DEAD_SPINNER = 9

    /** the floating-text sheet is cut into squares of this size */
    private const val TEXT_ICON_SIZE = 7

    fun item(image: Int): Image = ItemSprite(image, null)

    fun icon(type: Icons): Image = Icons.get(type)

    /** the squashed cave spider, the icon of a bug-fix entry */
    fun bugfix(): Image {
        val icon = Image(TextureCache.get(Assets.SPINNER))
        icon.frame(TextureFilm(icon.texture, SPRITE_SIZE, SPRITE_SIZE).get(DEAD_SPINNER))
        return icon
    }

    /** a buff's own icon, taken from the same sheet the buff indicator uses */
    fun buff(buff: Buff): Image {
        val icon = Image(TextureCache.get(Assets.BUFFS_SMALL))
        icon.frame(TextureFilm(icon.texture, BuffIndicator.SIZE, BuffIndicator.SIZE).get(buff.icon()))
        return icon
    }

    /** a damage/status glyph from the floating-text sheet, drawn at twice its size */
    fun textIcon(index: Int, scale: Float = 2f): Image {
        val icon = Image(TextureCache.get(Assets.TEXT_ICONS))
        icon.frame(TextureFilm(icon.texture, TEXT_ICON_SIZE, TEXT_ICON_SIZE).get(index))
        icon.scale.set(scale)
        return icon
    }
}
