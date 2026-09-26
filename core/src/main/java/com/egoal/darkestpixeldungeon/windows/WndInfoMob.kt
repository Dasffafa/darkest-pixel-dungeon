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
package com.egoal.darkestpixeldungeon.windows

import com.egoal.darkestpixeldungeon.actors.mobs.Mob
import com.egoal.darkestpixeldungeon.items.Catalog
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.scenes.PixelScene
import com.egoal.darkestpixeldungeon.sprites.ItemSprite
import com.egoal.darkestpixeldungeon.sprites.ItemSpriteSheet
import com.egoal.darkestpixeldungeon.ui.BuffIndicator
import com.egoal.darkestpixeldungeon.ui.HealthBar
import com.egoal.darkestpixeldungeon.ui.ResistanceIndicator
import com.watabou.noosa.Image
import com.watabou.noosa.RenderedText
import com.watabou.noosa.ui.Component
import com.watabou.utils.Log

class WndInfoMob(mob: Mob) : WndTitledMessage(MobTitle(mob),
        mob.description() + "\n\n" + mob.state.status()) {

    init {
        Catalog.SetSeen(mob)
    }

    class MobTitle(mob: Mob) : Component() {
        private val image: Image
        private val name: RenderedText
        private val health: HealthBar
        private val buffs: BuffIndicator
        private val resistances: ResistanceIndicator
        override fun layout() {
            image.x = 0f
            image.y = Math.max(0f, name.height() + GAP + health.height() - image.height)
            name.x = image.width + GAP
            name.y = image.height - health.height() - GAP - name.baseLine()
            val w = width - image.width - GAP
            health.setRect(image.width + GAP, image.height - health.height(), w,
                    health.height())
            buffs.setPos(
                    name.x + name.width() + GAP - 1,
                    name.y + name.baseLine() - BuffIndicator.SIZE - 2)
            resistances.setRect(3f, image.y + image.height() + 3f, width - 6f, 0f)
            height = resistances.bottom()
        }

        companion object {
            private const val GAP = 2

            // the catalog opens with no run, where a sprite may need state that
            // only exists in game: fall back to a placeholder icon
            private fun spriteOf(mob: Mob): Image = try {
                mob.sprite()
            } catch (e: Exception) {
                Log.w("dpd", "mob sprite failed: " + mob.javaClass.simpleName + ": " + e)
                ItemSprite(ItemSpriteSheet.SOMETHING, null)
            }
        }

        init {
            name = PixelScene.renderText(M.T(mob.name), 9)
            name.hardlight(TITLE_COLOR)
            add(name)
            image = spriteOf(mob)
            add(image)
            health = HealthBar()
            health.level(mob)
            add(health)
            buffs = BuffIndicator(mob)
            add(buffs)
            resistances = ResistanceIndicator(mob)
            add(resistances)
        }
    }
}