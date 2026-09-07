package com.egoal.darkestpixeldungeon.actors.mobs.npcs

import com.egoal.darkestpixeldungeon.Assets
import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.sprites.SimpleMobSprite
import com.egoal.darkestpixeldungeon.windows.WndDialogue
import com.watabou.noosa.Game

class Seeker : NPC.Unbreakable() {
    init {
        spriteClass = Sprite::class.java

        properties.add(Property.IMMOVABLE)
    }

    override fun interact(): Boolean {
        WndDialogue.Show(this, M.L(this, "greetings"), M.L(this, "desc_yourself")) {
            WndDialogue.Show(this, M.L(this, "dungeons"), M.L(this, "check_info")) {
                WndDialogue.Show(this, M.L(this, "desc_state"), M.L(this, "info_dpd"), M.L(this, "info_all"), M.L(this, "maybe_nexttime")) {
                    if (it == 0) {
                        Game.platform.openURI(WIKI_DPD)
                    } else if (it == 1) {
                        Game.platform.openURI(WIKI_IDX)
                    }
                }
            }
        }
        return false
    }

    class Sprite : SimpleMobSprite(Assets.SEEKER)

    companion object {
        private const val WIKI_DPD = "https://pixeldungeon.fandom.com/wiki/Mod-Darkest_Pixel_Dungeon"
        private const val WIKI_IDX = "https://pixeldungeon.fandom.com/wiki/Main_Page"
    }
}
