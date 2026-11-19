# dpd-selftest

Real-engine, hidden-window E2E regression test harness for Darkest Pixel Dungeon (adapted from `rpd-selftest`).

## Commands

```bash
./gradlew :e2e-tests:listScenarios
./gradlew :e2e-tests:e2eTest
./gradlew :e2e-tests:e2eTest -Pscenario=SettingsKeyboardTabRegression
./gradlew :e2e-tests:e2eTest -Pscenario=Smoke,HoldToMoveRegression
```

Each scenario runs in an isolated child JVM with a hidden LWJGL3 window, disabled audio, and a sandboxed preferences/home directory. Results and JUnit XML (`TEST-e2e.xml`) are written to `tools/dpd-selftest/build/dpd-selftest/run-*/`.
