package com.egoal.darkestpixeldungeon.messages

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.Dungeon
import com.egoal.darkestpixeldungeon.actors.hero.HeroClass
import com.egoal.darkestpixeldungeon.items.Item

/**
 * 中国风格(林冲 / EXILE): 卷轴作符纸、铁头棍作水火棍等。
 *
 * 文本键位于 items_zh / items_zh_TW (windows_zh 等) 的 `###chinese style` 分区。
 */
object ChineseStyle : MessageStyle {

    override val name: String = "chinese_style"

    override fun isActive(): Boolean = !Dungeon.isHeroNull
            && DarkestPixelDungeon.specialStyleText()
            && HeroClass.isChineseHero(Dungeon.hero)

    override fun itemsSheet(): String = Assets.DPD_ITEMS_CHINESE

    override fun spriteFor(item: Item): Int? = SPRITE_OVERRIDES[item.javaClass]

    // item class -> items_chinese_style.png 中的 tile 索引; 美术就位后在此登记。
    private val SPRITE_OVERRIDES: Map<Class<out Item>, Int> = mapOf()

}
