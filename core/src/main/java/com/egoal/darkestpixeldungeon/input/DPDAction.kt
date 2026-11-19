/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.egoal.darkestpixeldungeon.input

import com.badlogic.gdx.Input

/** Desktop defaults adapted from Shattered Pixel Dungeon's SPDAction. */
enum class DPDAction {
    NORTH, WEST, SOUTH, EAST, NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST,
    PAN_UP, PAN_LEFT, PAN_DOWN, PAN_RIGHT,
    WAIT_OR_PICKUP, INVENTORY, EXAMINE, REST,
    QUICK_SLOT_1, QUICK_SLOT_2, QUICK_SLOT_3, QUICK_SLOT_4, QUICK_SLOT_5, QUICK_SLOT_6, QUICK_SLOT_7, QUICK_SLOT_8,
    SWITCH_QUICKSLOT_TAB,
    STAIRS_DOWN, STAIRS_UP,
    TAG_ATTACK, TAG_ACTION, TAG_LOOT, TAG_RESUME, CYCLE_TARGET,
    HERO_INFO, JOURNAL, TORCH, PERK, ZOOM_IN, ZOOM_OUT;

    fun isMovement(): Boolean = when (this) {
        NORTH, WEST, SOUTH, EAST, NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST -> true
        else -> false
    }

    fun isPan(): Boolean = when (this) {
        PAN_UP, PAN_LEFT, PAN_DOWN, PAN_RIGHT -> true
        else -> false
    }

    companion object {
        private val defaults: LinkedHashMap<Int, DPDAction> = linkedMapOf(
                Input.Keys.UP to NORTH, Input.Keys.NUMPAD_8 to NORTH,
                Input.Keys.LEFT to WEST, Input.Keys.NUMPAD_4 to WEST,
                Input.Keys.DOWN to SOUTH, Input.Keys.NUMPAD_2 to SOUTH,
                Input.Keys.RIGHT to EAST, Input.Keys.NUMPAD_6 to EAST,
                Input.Keys.NUMPAD_7 to NORTH_WEST,
                Input.Keys.NUMPAD_9 to NORTH_EAST,
                Input.Keys.NUMPAD_1 to SOUTH_WEST,
                Input.Keys.NUMPAD_3 to SOUTH_EAST,
                Input.Keys.W to PAN_UP,
                Input.Keys.A to PAN_LEFT,
                Input.Keys.S to PAN_DOWN,
                Input.Keys.D to PAN_RIGHT,
                Input.Keys.SPACE to WAIT_OR_PICKUP, Input.Keys.NUMPAD_5 to WAIT_OR_PICKUP,
                Input.Keys.F to INVENTORY, Input.Keys.I to INVENTORY,
                Input.Keys.E to EXAMINE,
                Input.Keys.NUM_1 to QUICK_SLOT_1, Input.Keys.NUM_2 to QUICK_SLOT_2,
                Input.Keys.NUM_3 to QUICK_SLOT_3, Input.Keys.NUM_4 to QUICK_SLOT_4,
                Input.Keys.NUM_5 to QUICK_SLOT_5, Input.Keys.NUM_6 to QUICK_SLOT_6,
                Input.Keys.NUM_7 to QUICK_SLOT_7, Input.Keys.NUM_8 to QUICK_SLOT_8,
                Input.Keys.GRAVE to SWITCH_QUICKSLOT_TAB,
                Input.Keys.PERIOD to STAIRS_DOWN,
                Input.Keys.COMMA to STAIRS_UP,
                Input.Keys.Q to TAG_ATTACK,
                Input.Keys.C to TAG_LOOT, Input.Keys.ENTER to TAG_LOOT,
                Input.Keys.R to TAG_RESUME,
                Input.Keys.TAB to CYCLE_TARGET,
                Input.Keys.H to HERO_INFO,
                Input.Keys.J to JOURNAL,
                Input.Keys.L to TORCH,
                Input.Keys.U to PERK,
                Input.Keys.EQUALS to ZOOM_IN, Input.Keys.Z to ZOOM_IN,
                Input.Keys.MINUS to ZOOM_OUT, Input.Keys.X to ZOOM_OUT
        )

        @JvmStatic
        fun defaultBindings(): LinkedHashMap<Int, DPDAction> = LinkedHashMap(defaults)

        @JvmStatic
        fun fromKey(keyCode: Int): DPDAction? = defaults[keyCode]
    }
}
