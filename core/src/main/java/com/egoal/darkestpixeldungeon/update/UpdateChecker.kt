package com.egoal.darkestpixeldungeon.update

import com.badlogic.gdx.Gdx
import com.egoal.darkestpixeldungeon.network.SpiritServer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val PATH = "/api/v1/version"
    private const val CONNECT_TIMEOUT = 8000
    private const val READ_TIMEOUT = 15000
    private const val PLACEHOLDER = "https://example.com"

    private val json = Json { ignoreUnknownKeys = true }

    fun fetch(onResult: (UpdateInfo?) -> Unit) {
        val base = SpiritServer.baseUrl
        if (base.isBlank() || base == PLACEHOLDER) {
            onResult(null)
            return
        }

        Thread {
            var result: UpdateInfo? = null
            try {
                val conn = URL(base + PATH).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = CONNECT_TIMEOUT
                    conn.readTimeout = READ_TIMEOUT
                    conn.setRequestProperty("Accept", "application/json")
                    val code = conn.responseCode
                    if (code in 200..299) {
                        val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                        result = json.decodeFromString(UpdateInfo.serializer(), body)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Gdx.app?.log("dpd", "update check failed", e)
            }
            Gdx.app?.postRunnable { onResult(result) }
        }.apply { isDaemon = true }.start()
    }
}
