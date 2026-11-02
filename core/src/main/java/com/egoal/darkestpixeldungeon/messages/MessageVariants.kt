package com.egoal.darkestpixeldungeon.messages

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.items.Item

/**
 * 一种风格: 决定文本变体与物品贴图的替换。
 *
 * 文本变体在各语言 properties 里写成 `<基键>.<name>=新值`; 风格激活时
 * [Messages.get] 会优先尝试这些键, 未命中回退基键。
 */
interface MessageStyle {

    /** 风格名, 同时是 properties 里的变体后缀 */
    val name: String

    fun isActive(): Boolean

    /** 该风格下物品使用的整张 sheet */
    fun itemsSheet(): String

    /** 物品在 [itemsSheet] 中的 tile 索引; 只登记给出新贴图的物品, 未登记返回 null 表示沿用原图 */
    fun spriteFor(item: Item): Int?
}

/**
 * 风格注册表, 有序(先注册者优先), 每项按当前游戏状态判定是否激活。
 *
 * 新增风格: 实现 [MessageStyle], 在 [register] 处登记, 再补齐各语言 properties 的变体键。
 */
object MessageVariants {

    private val styles = LinkedHashMap<String, MessageStyle>()

    init {
        register(ChineseStyle)
    }

    @JvmStatic
    fun register(style: MessageStyle) {
        styles[style.name] = style
    }

    @JvmStatic
    fun isActive(name: String): Boolean = styles[name]?.isActive() ?: false

    /** 风格生效时返回其后缀名(如 "chinese_style"), 否则 null */
    @JvmStatic
    fun getMessageVariant(name: String): String? = if (isActive(name)) name else null

    /** 当前生效的风格, 按注册顺序 */
    @JvmStatic
    fun activeStyles(): List<MessageStyle> = styles.values.filter { it.isActive() }

    /** [key] 在当前生效风格下的候选变体键, 按注册顺序; 无生效风格时返回空表 */
    @JvmStatic
    fun variantKeys(key: String): List<String> {
        var keys: MutableList<String>? = null
        for (style in styles.values) {
            if (!style.isActive()) continue
            if (keys == null) keys = ArrayList(styles.size)
            keys.add("$key.${style.name}")
        }
        return keys ?: emptyList()
    }

    @JvmStatic
    fun itemsSheet(): String {
        for (style in styles.values) {
            if (style.isActive()) return style.itemsSheet()
        }
        return Assets.DPD_ITEMS
    }

    @JvmStatic
    fun spriteFor(item: Item): Int? {
        for (style in styles.values) {
            if (!style.isActive()) continue
            style.spriteFor(item)?.let { return it }
        }
        return null
    }

}
