# CULTUS OS (Android, Kotlin, Jetpack Compose)

Premium landscape cockpit for a 2010 Suzuki Cultus (G10B EFI, manual, original ECU). Designed for Galaxy S10 landscape.

## Open and run
1. Open this folder in Android Studio (Koala or newer, JDK 17). Let Gradle sync; it downloads the wrapper.
2. Run on a device or emulator in landscape.
3. Your car: delete `app/src/main/res/drawable/cultus_render.xml` and add `app/src/main/res/drawable-nodpi/cultus_render.png` (transparent background).

Status: written to compile against AGP 8.6 / Kotlin 2.0.20 / Compose BOM 2024.09.03, but it has NOT been built or run yet. Expect to fix small compile errors on first sync.

## Structure
- `ecu/`    Independent ECU module. `EcuLink` interface, `EcuSnapshot`, `Elm327BluetoothLink` (candidate, unverified).
- `data/`   Room: `fuel_log`, `service_record`, `reminder`. Offline-first, no network.
- `domain/` `FuelMath` (total distance / total fuel, windows 50 to 3,000 km, daily/weekly/monthly/overall) and `TripTracker`.
- `ui/`     Theme tokens, glass components, Cockpit / Fuel / Vehicle screens, sliding glass nav.

## Rules baked in
- No fuel-level gauge, no remaining fuel, no range.
- Nothing is simulated. Missing ECU values stay null and show as dashes.
- Distance is logged only when fuel rate is known for the same instant, so average km/L is never skewed.
- Average km/L = sum(distance) / sum(fuel).

## Open items (need your car)
- The ELM327 link uses standard OBD-II PIDs (010C RPM, 010D speed, 0110 MAF). The original Suzuki ECU may not answer these. Capture what your adapter returns, then adapt `Elm327BluetoothLink` or add a Suzuki-specific link behind `EcuLink`.
- Fuel rate comes from MAF (petrol, AFR 14.7, 745 g/L). If the ECU has no MAF PID, another source (injector pulse width and injector flow rate) is needed.
- Service reminders count logged distance since the last record of that type, not the odometer.
