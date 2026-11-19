package dpdtest

import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class CheckFailed(message: String) : RuntimeException(message)
class HarnessError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object Result {
    enum class Verdict(val exitCode: Int) {
        PASS(0),
        FAIL(10),
        CRASH(11),
        STUCK(12),
        UNSAFE(13),
        ERROR(14),
        KILLED(15);
    }

    @Volatile
    var scenarioName: String = "unknown"

    @Volatile
    var runDir: File? = null

    private val finished = AtomicBoolean(false)
    private var passedChecks = 0
    private val failedChecks = Collections.synchronizedList(ArrayList<String>())
    private val notes = Collections.synchronizedList(ArrayList<String>())

    @Synchronized
    fun recordPass(desc: String) {
        passedChecks++
        Log.info("CHECK PASS: $desc")
    }

    @Synchronized
    fun recordFail(desc: String) {
        failedChecks.add(desc)
        Log.error("CHECK FAIL: $desc")
    }

    fun note(msg: String) {
        notes.add(msg)
        Log.info("NOTE: $msg")
    }

    fun passedCheckCount(): Int = passedChecks
    fun hasFailedChecks(): Boolean = failedChecks.isNotEmpty()
    fun firstFailedCheck(): String = failedChecks.firstOrNull() ?: "unknown check failed"

    fun crash(where: String, t: Throwable): Nothing {
        finish(Verdict.CRASH, "uncaught exception in $where: $t", Log.stackOf(t))
    }

    fun finish(verdict: Verdict, reason: String, detail: String?): Nothing {
        if (finished.compareAndSet(false, true)) {
            val summary = buildString {
                appendLine("VERDICT: ${verdict.name}")
                appendLine("SCENARIO: $scenarioName")
                appendLine(String.format(Locale.ROOT, "ELAPSED: %.2fs", Log.seconds()))
                appendLine("REASON: $reason")
                appendLine("CHECKS: passed=$passedChecks failed=${failedChecks.size}")
                for (f in failedChecks) appendLine("  FAIL: $f")
                for (n in notes) appendLine("  NOTE: $n")
                if (!detail.isNullOrBlank()) {
                    appendLine("--- DETAIL ---")
                    appendLine(detail)
                }
            }
            Log.info("FINISHING ${verdict.name}: $reason")
            runDir?.let { dir ->
                runCatching {
                    dir.mkdirs()
                    File(dir, "summary.txt").writeText(summary, Charsets.UTF_8)
                }
            }
        }
        Runtime.getRuntime().halt(verdict.exitCode)
        throw AssertionError("unreachable")
    }
}
