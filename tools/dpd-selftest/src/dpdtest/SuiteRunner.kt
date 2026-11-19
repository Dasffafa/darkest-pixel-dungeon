package dpdtest

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object SuiteRunner {
    data class Outcome(
        val name: String,
        val verdict: Result.Verdict,
        val expectedVerdict: Result.Verdict,
        val passed: Boolean,
        val elapsedSec: Double,
        val reason: String,
        val runDir: Path
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val selector = args.firstOrNull()?.takeIf { it.isNotBlank() } ?: "all"
        val manifest = loadManifest()
        val allNames = manifest.getProperty("scenarios", "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (selector == "list") {
            allNames.forEach { println(it) }
            return
        }

        val selected = selectScenarios(allNames, selector)
        if (selected.isEmpty()) {
            System.err.println("dpd-selftest: no scenario matches '$selector'. Available: ${allNames.joinToString(", ")}")
            exitProcess(2)
        }

        val outRoot = Paths.get(System.getProperty("dpd.output", "tools/dpd-selftest/build/dpd-selftest"))
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val suiteDir = outRoot.resolve("run-$stamp")
        Files.createDirectories(suiteDir)

        val outcomes = selected.map { name ->
            runOne(name, manifest, suiteDir.resolve(name))
        }

        writeJUnitXml(suiteDir.resolve("TEST-e2e.xml"), outcomes)

        println("==================== dpd-selftest results ====================")
        for (o in outcomes) {
            val tag = if (o.passed) "PASS" else o.verdict.name
            println(String.format("[%-6s] %-36s %5.1fs  %s", tag, o.name, o.elapsedSec, o.reason))
        }
        val failedCount = outcomes.count { !it.passed }
        println("TOTAL: ${outcomes.size}   PASSED=${outcomes.size - failedCount}   FAILED=$failedCount")
        println("RUN_DIR: $suiteDir")

        if (failedCount > 0) {
            exitProcess(1)
        }
    }

    private fun runOne(name: String, manifest: Properties, runDir: Path): Outcome {
        Files.createDirectories(runDir)
        val expectedStr = manifest.getProperty("$name.expected", "PASS")
        val expected = runCatching { Result.Verdict.valueOf(expectedStr) }.getOrDefault(Result.Verdict.PASS)
        val timeoutSec = manifest.getProperty("$name.timeout", "180").toLongOrNull() ?: 180L

        val javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        val cp = System.getProperty("java.class.path")
        val cmd = listOf(
            javaBin,
            "-Xmx1024m",
            "-Djava.io.tmpdir=${runDir.resolve("sandbox/tmp").toAbsolutePath()}",
            "-Duser.home=${runDir.resolve("sandbox/home").toAbsolutePath()}",
            "-Ddpd.scenario=$name",
            "-Ddpd.rundir=${runDir.toAbsolutePath()}",
            "-Ddpd.timeout=$timeoutSec",
            "-Ddpd.versionName=${System.getProperty("dpd.versionName", "0.8.1b")}",
            "-Ddpd.versionCode=${System.getProperty("dpd.versionCode", "57")}",
            "-cp", cp,
            "dpdtest.Launcher"
        )
        Files.createDirectories(runDir.resolve("sandbox/tmp"))
        Files.createDirectories(runDir.resolve("sandbox/home"))

        val t0 = System.nanoTime()
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .redirectOutput(runDir.resolve("stdout.log").toFile())
            .start()

        val finished = process.waitFor(timeoutSec + 30L, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            val elapsed = (System.nanoTime() - t0) / 1_000_000_000.0
            return Outcome(name, Result.Verdict.KILLED, expected, expected == Result.Verdict.KILLED, elapsed, "process killed after timeout", runDir)
        }

        val elapsed = (System.nanoTime() - t0) / 1_000_000_000.0
        val exit = process.exitValue()
        val verdict = Result.Verdict.values().firstOrNull { it.exitCode == exit } ?: Result.Verdict.ERROR
        val summaryFile = runDir.resolve("summary.txt").toFile()
        val reason = if (summaryFile.exists()) {
            summaryFile.readLines().firstOrNull { it.startsWith("REASON: ") }?.removePrefix("REASON: ") ?: "exit $exit"
        } else {
            "exit $exit (no summary.txt; see stdout.log)"
        }
        return Outcome(name, verdict, expected, verdict == expected, elapsed, reason, runDir)
    }

    private fun loadManifest(): Properties {
        val props = Properties()
        SuiteRunner::class.java.getResourceAsStream("/scenarios.properties")?.use {
            props.load(it)
        }
        return props
    }

    private fun selectScenarios(all: List<String>, selector: String): List<String> {
        if (selector == "all") return all
        val tokens = selector.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val result = LinkedHashSet<String>()
        for (token in tokens) {
            val regex = token.replace(".", "\\.").replace("*", ".*").replace("?", ".").toRegex()
            for (name in all) {
                if (regex.matches(name)) result.add(name)
            }
        }
        return result.toList()
    }

    private fun writeJUnitXml(outFile: Path, outcomes: List<Outcome>) {
        val failures = outcomes.count { !it.passed }
        val xml = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<testsuite name="dpd-selftest" tests="${outcomes.size}" failures="$failures">""")
            for (o in outcomes) {
                appendLine("""  <testcase name="${escapeXml(o.name)}" time="${String.format(java.util.Locale.ROOT, "%.3f", o.elapsedSec)}">""")
                if (!o.passed) {
                    appendLine("""    <failure message="${escapeXml("${o.verdict}: ${o.reason}")}"/>""")
                }
                appendLine("  </testcase>")
            }
            appendLine("</testsuite>")
        }
        Files.write(outFile, xml.toByteArray(Charsets.UTF_8))
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
