# Stage 2.8 - Take-home masterclass reference

Date: 2026-09-12

Result: PASS

Starting boundary: `focus-lab/checkpoint-07-personalized` at `08563659e1ca80eb3083fb078497b85cf3983393` (`feat(masterclass): enable runtime personalization (Stage 2.7)`).

## Repository diagnosis

- Branch at start: `stage/2-masterclass-reference`; working tree was clean.
- The checkpoint-07 tag points to the starting HEAD, and all four current student-owned files are byte-identical to `masterclass/checkpoints/07_personalized`.
- Every manifest entry from `01_starter` through `07_personalized` has the complete four-file snapshot expected by the recovery scripts.
- The snapshot and Stage 2.3 commit for `04_progress` exist, but the expected Git tag `focus-lab/checkpoint-04-progress` is absent. This pre-existing metadata discrepancy was recorded rather than silently inventing history during finalization.
- No production source, architecture, dependency, checkpoint snapshot or recovery script required a Stage 2.8 change.

## Automated verification

- PASS - `.\gradlew.bat testDebugUnitTest --offline --rerun-tasks --console=plain`: 51 tests, 0 failures, 0 errors, 0 skipped; 24 tasks executed.
- PASS - `.\gradlew.bat connectedDebugAndroidTest --offline --rerun-tasks --console=plain`: 9 tests on Pixel_7 AVD / Android 16, 0 failures, 0 skipped; 67 tasks executed.
- PASS - `.\gradlew.bat assembleDebug --offline --rerun-tasks --console=plain`: 36 tasks executed; debug APK assembled successfully.
- The wrapper used the already warmed `C:\Users\nemag\.gradle` cache; dependency resolution remained offline and no versions changed.

## Build artifact

- Generated APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Size: 12,419,514 bytes.
- SHA-256: `DF7CE95C42096635312C74278F794157CFFEDC6D690073F997842F0BC574B540`.
- The APK remains an ignored generated artifact and is not committed.

## Final APK acceptance

Environment: `emulator-5554`, model `sdk_gphone64_x86_64` / Pixel_7 AVD, Android 16 / API 36, 1080x2400 at 420 dpi.

- PASS - the instrumentation deployment left the target package absent, so the final APK was fresh-installed with `adb install`; install returned `Success`.
- PASS - explicit cold launch of `com.example.focuslab/.MainActivity` returned `Status: ok`; the process remained alive and MainActivity was resumed.
- PASS - visual/UI hierarchy inspection showed the personalized `Focus Lab — Моя версия` identity, default `Мой фокус`, category editor entry, all four durations, idle timer card, ProgressCard and enabled Start action without clipping or overlap on the approximately 412 dp target.
- PASS - selected `Чтение` and `1 мин`, pressed Start and observed Running at `00:51`; selectors/editor/primary action were disabled while running.
- PASS - force-stop/cold relaunch before the end restored `Чтение`, `1 мин` and the same active session at `00:22` with XP 0 and 0 completed sessions.
- PASS - natural completion produced XP 10 and 1 completed session, then returned to idle/startable state.
- PASS - a second force-stop/cold relaunch preserved `Чтение`, `1 мин`, XP 10 and 1 completed session; no duplicate reward was issued.
- Runtime category CRUD/restart was not repeated after the fresh install. It is covered by the 9-test device suite, repository/codec unit tests and `evidence/masterclass/stage-2.7-runtime-personalization.md`, recorded on this unchanged production commit line.

## Documentation and scope

- README, classroom preflight, known limitations, teacher runbook and student steps now identify starter versus final reference, the reproducible APK command/path, the expected transfer channels and the per-install DataStore boundary.
- FocusRoute, FocusViewModel, FocusRepository, Preferences DataStore and stateless student-facing FocusScreen boundaries are unchanged.
- No feature work, dependency update, binary evidence, APK copy, build directory or local emulator data was added to Git.

## Known unverified checks

- A separate 360 dp final-reference layout run was not repeated; the final smoke used the approximately 412 dp target above.
- CategoryEditor keyboard ergonomics were not manually repeated in Stage 2.8; the existing Stage 2.7 acceptance and current device tests cover the editor flow.
