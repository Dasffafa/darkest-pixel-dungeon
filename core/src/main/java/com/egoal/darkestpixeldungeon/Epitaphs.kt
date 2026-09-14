package com.egoal.darkestpixeldungeon

import com.egoal.darkestpixeldungeon.messages.M
import com.egoal.darkestpixeldungeon.network.SpiritServer
import com.egoal.darkestpixeldungeon.windows.InputDialog
import com.watabou.utils.Bundle
import com.watabou.utils.FileUtils
import com.watabou.utils.Random
import java.io.IOException

/**
 * The local pool of epitaphs that can be carved on graves. Entries come from two
 * sources: the player's own deaths (weighted higher) and batches downloaded from
 * the server.
 */
object Epitaphs {
    private const val FILE = "epitaphs.dat"
    private const val COUNT = "count"
    private const val NAME = "name"
    private const val TEXT = "text"
    private const val OWN = "own"

    private const val MAX_POOL = 500
    private const val OWN_WEIGHT = 3f

    class Entry(var name: String = "", var text: String = "", var own: Boolean = false)

    /** Texts already carved during the current run, so a run never repeats one. */
    private val used = HashSet<String>()

    fun startNewRun() {
        used.clear()
    }

    fun deathTitle(): String = M.L(Epitaphs::class.java, "death_title")

    fun victoryTitle(): String = M.L(Epitaphs::class.java, "victory_title")

    fun victoryDefault(): String = M.L(Epitaphs::class.java, "victory_default")

    fun rejectedNotice(id: String, text: String): String =
            M.L(Epitaphs::class.java, "rejected", id, text)

    fun graveLabel(name: String): String = M.L(Epitaphs::class.java, "grave_label", name)

    /** Asks the winner for a few words; the speech is only stored, never carved on a grave. */
    fun promptVictory(userName: String) {
        val default = victoryDefault()
        InputDialog.GetString(victoryTitle(), default) { text ->
            SpiritServer.uploadVictory(userName, text.ifBlank { default })
        }
    }

    fun load(): MutableList<Entry> {
        val entries = mutableListOf<Entry>()
        try {
            val bundle = FileUtils.bundleFromFile(FILE)
            for (i in 0 until bundle.getInt(COUNT)) {
                val key = "entry$i"
                if (!bundle.contains(key)) continue
                val e = bundle.getBundle(key)
                entries.add(Entry(
                        name = e.getString(NAME),
                        text = e.getString(TEXT),
                        own = if (e.contains(OWN)) e.getBoolean(OWN) else false))
            }
        } catch (e: IOException) {
            entries.clear()
        } catch (e: Exception) {
            DarkestPixelDungeon.reportException(e)
            entries.clear()
        }
        return entries
    }

    fun addOwn(name: String, text: String) {
        if (text.isBlank()) return

        val entries = load()
        if (entries.any { it.text == text }) return

        entries.add(Entry(name, text, true))
        trim(entries)
        save(entries)
    }

    fun mergeDownloaded(downloaded: List<Entry>) {
        val entries = load()
        var changed = false
        for (e in downloaded) {
            if (e.text.isBlank()) continue
            if (entries.any { it.text == e.text }) continue
            entries.add(Entry(e.name, e.text, false))
            changed = true
        }
        if (!changed) return

        trim(entries)
        save(entries)
    }

    fun take(): Entry? {
        val entries = load().filter { it.text.isNotBlank() && it.text !in used }
        if (entries.isEmpty()) return null

        val weights = FloatArray(entries.size) { if (entries[it].own) OWN_WEIGHT else 1f }
        val chosen = entries[Random.chances(weights)]
        used.add(chosen.text)
        return chosen
    }

    private fun trim(entries: MutableList<Entry>) {
        while (entries.size > MAX_POOL) {
            val remote = entries.indexOfFirst { !it.own }
            if (remote >= 0) entries.removeAt(remote) else entries.removeAt(0)
        }
    }

    private fun save(entries: List<Entry>) {
        val bundle = Bundle()
        bundle.put(COUNT, entries.size)
        entries.forEachIndexed { i, e ->
            val entry = Bundle()
            entry.put(NAME, e.name)
            entry.put(TEXT, e.text)
            entry.put(OWN, e.own)
            bundle.put("entry$i", entry)
        }

        try {
            FileUtils.bundleToFile(FILE, bundle)
        } catch (e: IOException) {
            DarkestPixelDungeon.reportException(e)
        }
    }
}
