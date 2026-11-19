package com.egoal.darkestpixeldungeon.input

import com.badlogic.gdx.Input
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon

object DPDKeyBindings {
    private var bindings: LinkedHashMap<Int, DPDAction>? = null

    private fun ensureLoaded(): LinkedHashMap<Int, DPDAction> {
        bindings?.let { return it }
        val loaded = LinkedHashMap<Int, DPDAction>()
        val raw = DarkestPixelDungeon.keyBindings()
        if (raw.isEmpty()) {
            loaded.putAll(DPDAction.defaultBindings())
        } else if (raw != "NONE") {
            for (entry in raw.split(';')) {
                if (entry.isEmpty()) continue
                val sep = entry.indexOf(':')
                if (sep <= 0) continue
                val code = entry.substring(0, sep).toIntOrNull() ?: continue
                val action = runCatching { DPDAction.valueOf(entry.substring(sep + 1)) }.getOrNull() ?: continue
                loaded[code] = action
            }
        }
        bindings = loaded
        return loaded
    }

    private fun save() {
        val map = ensureLoaded()
        val encoded = if (map.isEmpty()) {
            "NONE"
        } else {
            map.entries.joinToString(";") { "${it.key}:${it.value.name}" }
        }
        DarkestPixelDungeon.keyBindings(encoded)
    }

    @JvmStatic
    fun actionForKey(keyCode: Int): DPDAction? = ensureLoaded()[keyCode]

    @JvmStatic
    fun keysForAction(action: DPDAction): List<Int> =
        ensureLoaded().entries.filter { it.value == action }.map { it.key }

    @JvmStatic
    fun bindKey(action: DPDAction, slotIndex: Int, keyCode: Int) {
        val map = ensureLoaded()
        val currentSlots = keysForAction(action)
        val oldKeyInSlot = currentSlots.getOrNull(slotIndex)

        // Rebuild action's slots cleanly (max 2 slots), keeping the other slot intact
        val otherKey = when (slotIndex) {
            0 -> currentSlots.getOrNull(1)
            else -> currentSlots.getOrNull(0)
        }

        if (keyCode > 0) {
            // Auto-overwrite any existing binding on keyCode
            map.remove(keyCode)
        }
        if (oldKeyInSlot != null) {
            map.remove(oldKeyInSlot)
        }
        if (otherKey != null) {
            map.remove(otherKey)
        }

        if (slotIndex == 0) {
            if (keyCode > 0) map[keyCode] = action
            if (otherKey != null && otherKey != keyCode) map[otherKey] = action
        } else {
            if (otherKey != null && otherKey != keyCode) map[otherKey] = action
            if (keyCode > 0) map[keyCode] = action
        }

        save()
    }

    @JvmStatic
    fun resetToDefaults() {
        bindings = DPDAction.defaultBindings()
        save()
    }

    @JvmStatic
    fun keyName(keyCode: Int): String {
        if (keyCode <= 0) return "-"
        return when (keyCode) {
            Input.Keys.GRAVE -> "`"
            Input.Keys.PERIOD -> "."
            Input.Keys.COMMA -> ","
            Input.Keys.EQUALS -> "="
            Input.Keys.MINUS -> "-"
            Input.Keys.PLUS -> "+"
            Input.Keys.SPACE -> "Space"
            Input.Keys.ENTER -> "Enter"
            Input.Keys.TAB -> "Tab"
            Input.Keys.UP -> "Up"
            Input.Keys.DOWN -> "Down"
            Input.Keys.LEFT -> "Left"
            Input.Keys.RIGHT -> "Right"
            else -> {
                val raw = Input.Keys.toString(keyCode) ?: "?"
                raw.replace("Numpad ", "Num")
            }
        }
    }
}
