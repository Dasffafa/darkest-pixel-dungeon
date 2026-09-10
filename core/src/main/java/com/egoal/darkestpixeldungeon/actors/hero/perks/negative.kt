package com.egoal.darkestpixeldungeon.actors.hero.perks

class RavenousAppetite: Perk(){
    override val isNegative: Boolean = true

    override fun image(): Int = PerkImageSheet.APPETITE_RAVENOUS
}