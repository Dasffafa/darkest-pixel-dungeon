package dpdtest

object ScenarioRunner {
    private const val PACKAGE = "dpdtest.scenarios."

    fun run(name: String) {
        try {
            runInner(name)
        } catch (t: Throwable) {
            Result.finish(Result.Verdict.ERROR, "harness scenario thread died: $t", Log.stackOf(t))
        }
    }

    private fun runInner(name: String) {
        val t0 = System.nanoTime()
        while (!HarnessGame.bootReady) {
            Thread.sleep(5)
            if ((System.nanoTime() - t0) / 1_000_000_000L > 90) {
                Result.finish(
                    Result.Verdict.STUCK,
                    "the game did not finish booting within 90 s",
                    "BOOT TIMEOUT\n${Log.dumpAllThreads()}"
                )
            }
        }
        Log.step(String.format(java.util.Locale.ROOT, "boot ok after %.1f s", Log.seconds()))

        val scenario = try {
            val cls = Class.forName(PACKAGE + name)
            cls.getDeclaredConstructor().newInstance() as Scenario
        } catch (t: Throwable) {
            Result.finish(Result.Verdict.ERROR, "cannot load scenario class $PACKAGE$name: $t", Log.stackOf(t))
        }

        Log.step("scenario $name: ${scenario.describe()}")

        val h = H()
        h.noteRenderThread(Launcher.renderThread)
        try {
            scenario.run(h)
            Log.step("scenario body returned normally")
        } catch (f: CheckFailed) {
            Log.step("scenario aborted by a failed check: ${f.message}")
        } catch (e: HarnessError) {
            Result.finish(Result.Verdict.ERROR, "scenario/harness error: ${e.message}", Log.stackOf(e))
        } catch (t: Throwable) {
            Result.finish(Result.Verdict.CRASH, "scenario threw uncaught exception: $t", Log.stackOf(t))
        }

        try {
            h.frames(30)
        } catch (e: HarnessError) {
            Log.warn("settling frames failed: ${e.message}")
        }

        if (Result.hasFailedChecks()) {
            Result.finish(Result.Verdict.FAIL, Result.firstFailedCheck(), null)
        } else if (Result.passedCheckCount() == 0) {
            Result.finish(Result.Verdict.ERROR, "the scenario finished without executing a single check", null)
        } else {
            Result.finish(Result.Verdict.PASS, "all ${Result.passedCheckCount()} checks passed", null)
        }
    }
}
