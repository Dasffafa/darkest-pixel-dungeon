package com.egoal.darkestpixeldungeon.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

object BundleGuard {

    private val ALLOWED_PREFIXES = listOf(
            "com.egoal.darkestpixeldungeon.items.",
            "com.egoal.darkestpixeldungeon.actors.hero.perks.")

    private val HERO_CLASSES = setOf(
            "WARRIOR", "MAGE", "ROGUE", "HUNTRESS", "SORCERESS", "EXILE")

    fun isSafe(data: ByteArray): Boolean {
        return try {
            isSafeNode(Json.parseToJsonElement(String(decompress(data), Charsets.UTF_8)))
        } catch (e: Exception) {
            false
        }
    }

    private fun decompress(data: ByteArray): ByteArray {
        return if (data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()) {
            GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
        } else {
            data
        }
    }

    private fun isSafeNode(node: JsonElement): Boolean {
        return when (node) {
            is JsonObject -> {
                val classNameElement = node["__className"]
                if (classNameElement != null) {
                    val className = classNameElement.asString() ?: return false
                    if (ALLOWED_PREFIXES.none { className.startsWith(it) }) return false
                }
                val heroClassElement = node["class"]
                if (heroClassElement != null) {
                    val heroClass = heroClassElement.asString() ?: return false
                    if (heroClass !in HERO_CLASSES) return false
                }
                node.values.all { isSafeNode(it) }
            }
            is JsonArray -> node.all { isSafeNode(it) }
            else -> true
        }
    }

    private fun JsonElement.asString(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content
}
