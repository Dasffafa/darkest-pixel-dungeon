package com.egoal.darkestpixeldungeon.network

import com.badlogic.gdx.Gdx
import com.egoal.darkestpixeldungeon.DarkestPixelDungeon
import com.egoal.darkestpixeldungeon.Epitaphs
import com.egoal.darkestpixeldungeon.actors.mobs.DarkSpirit
import com.egoal.darkestpixeldungeon.actors.mobs.SpiritRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

object SpiritServer {

    const val BASE_URL = "https://example.com"

    private const val BATCH_SIZE = 10
    private const val EPITAPH_MIN = 100
    private const val CONNECT_TIMEOUT = 8000
    private const val READ_TIMEOUT = 15000

    val isConfigured: Boolean
        get() = BASE_URL.isNotBlank() && BASE_URL != "https://example.com"

    @Volatile
    private var startupDone = false

    val shouldPrompt: Boolean
        get() = isConfigured && !DarkestPixelDungeon.spiritSyncDecided()

    fun setConsent(granted: Boolean) {
        DarkestPixelDungeon.uploadSpirits(granted)
        DarkestPixelDungeon.downloadSpirits(granted)
        DarkestPixelDungeon.spiritSyncDecided(true)
    }

    fun onStartup() {
        if (startupDone) return
        if (!isConfigured || !DarkestPixelDungeon.downloadSpirits()) return
        startupDone = true
        downloadSpirits()
        downloadEpitaphs()
    }

    fun upload(record: SpiritRecord) {
        if (!DarkestPixelDungeon.uploadSpirits()) return

        val body = DarkSpirit.EncodeRecord(record)
        background {
            val result = request("POST", "$BASE_URL/api/v1/spirits", body)
            handleUploadResult(result, record.userName, record.epitaph)
        }
    }

    fun uploadVictory(userName: String, speech: String) {
        if (!DarkestPixelDungeon.uploadSpirits()) return

        val body = buildJsonObject {
            put("username", userName)
            put("speech", speech)
        }.toString().toByteArray(Charsets.UTF_8)

        background {
            val result = request("POST", "$BASE_URL/api/v1/victory", body, json = true)
            handleUploadResult(result, userName, speech)
        }
    }

    private fun downloadSpirits() {
        background {
            val data = request("GET", "$BASE_URL/api/v1/spirits/batch?n=$BATCH_SIZE", null)
                    ?: return@background
            Gdx.app?.postRunnable { DarkSpirit.ImportRecords(data) }
        }
    }

    private fun downloadEpitaphs() {
        val n = maxOf(Epitaphs.load().size * 2, EPITAPH_MIN)
        background {
            val data = request("GET", "$BASE_URL/api/v1/epitaphs/random?n=$n", null)
                    ?: return@background
            val entries = parseEpitaphs(data)
            if (entries.isNotEmpty()) Gdx.app?.postRunnable { Epitaphs.mergeDownloaded(entries) }
        }
    }

    private fun parseEpitaphs(data: ByteArray): List<Epitaphs.Entry> {
        return try {
            val root = Json.parseToJsonElement(String(data, Charsets.UTF_8)) as? JsonObject
                    ?: return emptyList()
            val array = root["epitaphs"] as? JsonArray ?: return emptyList()
            array.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                Epitaphs.Entry(
                        name = (obj["name"] as? JsonPrimitive)?.content ?: "",
                        text = (obj["text"] as? JsonPrimitive)?.content ?: "",
                        own = false)
            }.filter { it.text.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun handleUploadResult(result: ByteArray?, userName: String, text: String) {
        val body = result?.toString(Charsets.UTF_8) ?: return
        val reason = try {
            val obj = Json.parseToJsonElement(body) as? JsonObject
            (obj?.get("reason") as? JsonPrimitive)?.content
        } catch (e: Exception) {
            null
        }
        if (reason == "rejected") {
            Gdx.app?.postRunnable {
                DarkestPixelDungeon.epitaphNotice(Epitaphs.rejectedNotice(userName, text))
            }
        }
    }

    private fun background(block: () -> Unit) {
        Thread {
            try {
                block()
            } catch (e: Exception) {
                Gdx.app?.log("dpd", "spirit sync failed", e)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun request(method: String, url: String, body: ByteArray?, json: Boolean = false): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("X-Device-Id", DarkestPixelDungeon.deviceId())
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty(
                        "Content-Type",
                        if (json) "application/json" else "application/octet-stream")
                conn.outputStream.use { it.write(body) }
            }

            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            return stream?.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
