package com.egoal.darkestpixeldungeon.items

import com.egoal.darkestpixeldungeon.items.potions.Potion
import com.egoal.darkestpixeldungeon.items.rings.Ring
import com.egoal.darkestpixeldungeon.items.scrolls.Scroll

/**
 * The catalog's own appearances for the items whose look is dealt out at
 * random: potions, scrolls and rings. Their look is a run's business - the
 * hero's bottle of `crimson` is this run's crimson and nothing else - but a
 * catalog line stands for a whole class, so borrowing a run's look would make
 * the tab depend on a save and hide the classes the run happened to miss.
 *
 * The catalog therefore deals out a full assignment of its own, once per open,
 * so every tab shows one complete, distinct set of looks and closing and
 * reopening the window re-deals it. Nothing here is saved.
 */
object CatalogAppearance {

    private var images: Map<Class<*>, Int> = emptyMap()

    /** re-deals a fresh look for every randomized item; called once per open */
    fun reroll() {
        val rolled = HashMap<Class<*>, Int>()

        for (handler in listOf(
                Potion.catalogLabels(),
                Scroll.catalogLabels(),
                Ring.catalogLabels()))
            rolled.putAll(handler.images())

        images = rolled
    }

    /** the catalog's look for a class, or null for items with a fixed sprite */
    fun image(cls: Class<*>): Int? = images[cls]
}
