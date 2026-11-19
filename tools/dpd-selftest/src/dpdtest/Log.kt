package dpdtest

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale

object Log {
    @Volatile
    @JvmField
    var frame: Long = 0

    private val startNanos = System.nanoTime()
    private var writer: PrintWriter? = null

    @Synchronized
    fun init(logFile: File) {
        logFile.parentFile?.mkdirs()
        writer = PrintWriter(FileWriter(logFile, false), true)
    }

    fun seconds(): Double = (System.nanoTime() - startNanos) / 1_000_000_000.0

    fun info(msg: String) = write("INFO", msg)
    fun step(msg: String) = write("STEP", msg)
    fun warn(msg: String) = write("WARN", msg)
    fun error(msg: String) = write("ERR ", msg)

    @Synchronized
    private fun write(level: String, msg: String) {
        val line = String.format(Locale.ROOT, "[%7.2fs f=%-5d %s] %s", seconds(), frame, level, msg)
        writer?.println(line)
        println(line)
    }

    fun stackOf(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    fun dumpAllThreads(): String {
        val sb = StringBuilder()
        for ((thread, stack) in Thread.getAllStackTraces()) {
            sb.append("--- ").append(thread.name).append(" (").append(thread.state).append(") ---\n")
            for (el in stack) {
                sb.append("  at ").append(el).append('\n')
            }
        }
        return sb.toString()
    }
}
