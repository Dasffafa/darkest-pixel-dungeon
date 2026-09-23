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

    const val DEFAULT_BASE_URL = "https://example.com"

    val baseUrl: String
        get() {
            val custom = DarkestPixelDungeon.spiritServerUrl()
            return if (custom.isNotBlank()) custom else DEFAULT_BASE_URL
        }

    private const val BATCH_SIZE = 10
    private const val EPITAPH_MIN = 100
    private const val CONNECT_TIMEOUT = 8000
    private const val READ_TIMEOUT = 15000

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && baseUrl != "https://example.com"

    @Volatile
    private var startupDone = false

    val shouldPrompt: Boolean
        get() = isConfigured && !DarkestPixelDungeon.spiritSyncDecided()

    fun setConsent(granted: Boolean) {
        DarkestPixelDungeon.spiritSync(granted)
        DarkestPixelDungeon.epitaphSync(granted)
        DarkestPixelDungeon.spiritSyncDecided(true)
    }

    fun onStartup() {
        if (startupDone || !isConfigured) return

        val syncSpirits = DarkestPixelDungeon.spiritSync()
        val syncEpitaphs = DarkestPixelDungeon.epitaphSync()
        if (!syncSpirits && !syncEpitaphs) return

        startupDone = true
        if (syncSpirits) {
            downloadSpirits { success ->
                if (!success) startupDone = false
            }
        }
        if (syncEpitaphs) downloadEpitaphs()
    }

    fun upload(record: SpiritRecord) {
        if (!DarkestPixelDungeon.spiritSync() || !isConfigured) return

        val body = DarkSpirit.EncodeRecord(record)
        background {
            val resp = request("POST", "$baseUrl/api/v1/spirits", body)
            if (isRejected(resp)) {
                Gdx.app?.postRunnable {
                    DarkSpirit.RemoveRecordByUuid(record.uuid)
                }
            }
        }
    }

    fun uploadEpitaph(userName: String, text: String) {
        if (!DarkestPixelDungeon.epitaphSync() || !isConfigured) return
        if (text.isBlank()) return

        val body = buildJsonObject {
            put("name", userName)
            put("text", text)
        }.toString().toByteArray(Charsets.UTF_8)

        background {
            val resp = request("POST", "$baseUrl/api/v1/epitaphs", body, json = true)
            handleUploadResult(resp, userName, text)
        }
    }

    fun uploadVictory(userName: String, speech: String) {
        if (!DarkestPixelDungeon.epitaphSync() || !isConfigured) return

        val body = buildJsonObject {
            put("username", userName)
            put("speech", speech)
        }.toString().toByteArray(Charsets.UTF_8)

        background {
            val resp = request("POST", "$baseUrl/api/v1/victory", body, json = true)
            handleUploadResult(resp, userName, speech)
        }
    }

    private fun downloadSpirits(onComplete: ((Boolean) -> Unit)? = null) {
        background {
            val resp = request("GET", "$baseUrl/api/v1/spirits/batch?n=$BATCH_SIZE", null)
            if (!resp.isSuccessful || resp.data == null) {
                onComplete?.invoke(false)
                return@background
            }
            Gdx.app?.postRunnable { DarkSpirit.ImportRecords(resp.data) }
            onComplete?.invoke(true)
        }
    }

    private fun downloadEpitaphs() {
        val n = maxOf(Epitaphs.load().size * 2, EPITAPH_MIN)
        background {
            val resp = request("GET", "$baseUrl/api/v1/epitaphs/random?n=$n", null)
            if (!resp.isSuccessful || resp.data == null) return@background
            val entries = parseEpitaphs(resp.data)
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

    private fun isRejected(resp: Response): Boolean {
        val body = resp.data?.toString(Charsets.UTF_8) ?: return false
        val reason = try {
            val obj = Json.parseToJsonElement(body) as? JsonObject
            (obj?.get("reason") as? JsonPrimitive)?.content
        } catch (e: Exception) {
            null
        }
        return reason == "rejected"
    }

    private fun handleUploadResult(resp: Response, userName: String, text: String): Boolean {
        if (!isRejected(resp)) return false

        Gdx.app?.postRunnable {
            DarkestPixelDungeon.epitaphNotice(Epitaphs.rejectedNotice(userName, text))
            Epitaphs.removeOwn(text)
        }
        return true
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

    class Response(val code: Int, val data: ByteArray?) {
        val isSuccessful get() = code in 200..299
    }

    private fun request(method: String, url: String, body: ByteArray?, json: Boolean = false): Response {
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

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val data = stream?.use { it.readBytes() }
            return Response(code, data)
        } catch (e: Exception) {
            return Response(-1, null)
        } finally {
            conn.disconnect()
        }
    }
}
