package dpdtest

import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.egoal.darkestpixeldungeon.desktop.DesktopPlatformSupport
import com.watabou.noosa.Game
import java.io.File
import kotlin.concurrent.thread

object Launcher {
    @Volatile
    @JvmField
    var renderThread: Thread? = null

    private fun prop(key: String, def: String? = null): String? {
        val v = System.getProperty(key)
        return if (v.isNullOrEmpty()) def else v
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val scenario = prop("dpd.scenario")
        val runDirProp = prop("dpd.rundir")
        if (scenario == null || runDirProp == null) {
            System.err.println("dpd-selftest: -Ddpd.scenario and -Ddpd.rundir are required (use SuiteRunner)")
            Runtime.getRuntime().halt(Result.Verdict.ERROR.exitCode)
            return
        }

        val runDir = File(runDirProp).absoluteFile
        val sandbox = File(runDir, "sandbox")
        val home = File(sandbox, "home")
        val prefs = File(sandbox, "prefs")
        runDir.mkdirs()
        home.mkdirs()
        prefs.mkdirs()

        try {
            Log.init(File(runDir, "game.log"))
        } catch (e: Exception) {
            System.err.println("dpd-selftest: cannot open log file in $runDir: $e")
            Runtime.getRuntime().halt(Result.Verdict.ERROR.exitCode)
            return
        }

        Result.scenarioName = scenario
        Result.runDir = runDir
        renderThread = Thread.currentThread()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Result.crash("thread \"${t.name}\"", e)
        }

        val timeScale = prop("dpd.timescale", "8")!!.toFloat()
        val fps = prop("dpd.fps", "120")!!.toInt()
        val timeoutSec = prop("dpd.timeout", "180")!!.toLong()
        val width = prop("dpd.width", "1280")!!.toInt()
        val height = prop("dpd.height", "720")!!.toInt()
        val versionName = prop("dpd.versionName", "0.8.1b")!!
        val versionCode = prop("dpd.versionCode", "57")!!.toInt()

        Log.info("dpd-selftest launcher: scenario=$scenario rundir=$runDir")
        startWatchdog(timeoutSec)

        Game.version = "$versionName-SELFTEST"
        Game.versionCode = versionCode
        HarnessGame.timeScale = timeScale

        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("dpd-selftest (hidden)")
            setInitialVisible(false)
            disableAudio(true)
            setWindowedMode(width, height)
            setWindowPosition(-20000, -20000)
            setDecorated(false)
            setResizable(false)
            setAutoIconify(false)
            setForegroundFPS(fps)
            setPreferencesConfig(prefs.absolutePath.replace('\\', '/') + "/", Files.FileType.Absolute)
        }

        thread(isDaemon = true, name = "DPD-Selftest-Scenario") {
            ScenarioRunner.run(scenario)
        }

        Lwjgl3Application(HarnessGame(DesktopPlatformSupport()), config)
    }

    private fun startWatchdog(timeoutSec: Long) {
        thread(isDaemon = true, name = "DPD-Selftest-Watchdog") {
            Thread.sleep(timeoutSec * 1000L)
            Result.finish(
                Result.Verdict.STUCK,
                "scenario exceeded timeout of ${timeoutSec}s",
                Log.dumpAllThreads()
            )
        }
    }
}
