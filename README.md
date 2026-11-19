# BENCH

Utility toolbox for Android: calculators, converters, sensor labs, with optional Shizuku data.

Slice 1: home (static tiles, no live data), fillet calculator, slope solver (tilt sensor, registered only while that tool is open).

- Kotlin + Jetpack Compose, minSdk 33 (AGSL shaders), package `com.bench`
- Build: `gradle :app:assembleDebug` (CI workflow included)
- Type is the system monospace for now; swap `BenchType.family` in `ui/Theme.kt` for the real typeface.
