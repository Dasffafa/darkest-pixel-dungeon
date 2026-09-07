package com.egoal.darkestpixeldungeon

import com.badlogic.gdx.Gdx
import com.watabou.utils.Bundle
import java.io.IOException

class TopExceptionHandler : Thread.UncaughtExceptionHandler {
    private val defaultUEH = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        WriteErrorFile(e)
        defaultUEH?.uncaughtException(t, e) ?: Gdx.app.exit()
    }

    companion object {
        //todo: may rework this 
        fun WriteErrorFile(tr: Throwable) {
            val strError = mutableListOf<String>()

            tr.message?.let {
                strError.add("MESSAGE: ")
                strError.add(it)
            }

            // stack
            strError.add("STACK: ")
            for (stk in tr.stackTrace.map { it.toString().trim { c -> c != '(' && c != ')' } }) {
                strError.add(stk)
                if (strError.size >= 20) break
            }

            val bundle = Bundle().apply {
                put(STR_ERROR, strError.toTypedArray()) // Log.getStackTraceString(tr))
            }
            try {
                com.watabou.utils.FileUtils.bundleToFile(ERROR_FILE, bundle)
            } catch (ignored: IOException) {
            }

            Gdx.app.error("dpd", "Unhandled exception", tr)
        }

        fun LoadErrorString(): String? = try {
            com.watabou.utils.FileUtils.bundleFromFile(ERROR_FILE).getString(STR_ERROR)
        } catch (e: IOException) {
            null
        }

        fun LoadErrorStrings(): Array<String>? = try {
            com.watabou.utils.FileUtils.bundleFromFile(ERROR_FILE).getStringArray(STR_ERROR)
        } catch (e: IOException) {
            null
        }

        fun HasErrorFile(): Boolean = com.watabou.utils.FileUtils.fileExists(ERROR_FILE)

        fun DeleteErrorFile() {
            com.watabou.utils.FileUtils.deleteFile(ERROR_FILE)
        }

        private const val ERROR_FILE = "error.dat"
        private const val STR_ERROR = "error"
    }
}
