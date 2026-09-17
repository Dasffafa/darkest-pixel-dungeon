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

import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.network.SpiritServer

class WndSpiritSyncConsent : WndOptions(
        M.L(WndSpiritSyncConsent::class.java, "title"),
        M.L(WndSpiritSyncConsent::class.java, "message"),
        M.L(WndSpiritSyncConsent::class.java, "accept"),
        M.L(WndSpiritSyncConsent::class.java, "decline")) {

    override fun onSelect(index: Int) {
        SpiritServer.setConsent(index == 0)
        SpiritServer.onStartup()
    }
}
