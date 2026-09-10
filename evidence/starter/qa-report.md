# Starter QA report

Date: 2026-09-10

Verified revision: the commit referenced by annotated tag focus-lab/starter-v1. Resolve it with git rev-list -n 1 focus-lab/starter-v1 after release finalization.

Starting revision: 1e38521 (chore(starter): prepare student-facing scaffold).

Working-tree exception: .idea/deviceManager.xml was already staged and .idea/misc.xml was already modified before Stage 1.9. They were preserved and excluded from the release commit. The canonical tagged commit contains only repository state, not those local edits.

## Environment

- Windows host; PowerShell 5.1.26100.9444 and PowerShell 7.6.5.
- OpenJDK 21.0.10, Gradle wrapper 9.4.1 and warmed user Gradle cache.
- Pixel_7 Android Virtual Device, Android 16 / API 36, 420 dpi.
- Git Bash and the bundled Python runtime were available for the shell counterpart test.

## Automated verification

- PASS - .\gradlew.bat testDebugUnitTest --offline --rerun-tasks: 51 tests, 0 failures, 0 errors, 0 skipped.
- PASS - .\gradlew.bat testDebugUnitTest assembleDebug --offline: Gradle completed successfully and produced app/build/outputs/apk/debug/app-debug.apk.
- PASS - .\gradlew.bat connectedDebugAndroidTest --offline: 7 tests on Pixel_7(AVD), 0 failed. The suite includes reusable components and CategoryEditor text input.

## Recovery verification

- PASS - restore-checkpoint.ps1 01_starter: a controlled FocusScreen mutation was backed up with all four student files, the complete snapshot was restored byte-for-byte and assembleDebug --offline passed.
- PASS - restore-checkpoint.sh 01_starter under Git Bash: a controlled ProgressCard mutation was backed up with all four student files, source equality was restored and the offline build passed. The initial Windows test found a CRLF metadata-path issue before any copy; the script now strips CR and the rerun passed.
- PASS - both restore scripts rejected an unknown checkpoint with exit code 1 before creating a backup or copying files.
- PASS - .recovery/ is ignored by Git.
- PASS - reset-student-state.ps1 -DeviceSerial emulator-5554 cleared com.example.focuslab and did not relaunch it; pidof returned no process. An invalid explicit serial was rejected with exit code 1.
- REVIEWED - when DeviceSerial is omitted, reset-student-state.ps1 requires exactly one ready ADB target; multi-device execution was not simulated because one emulator was connected.

## Manual checks

- PASS - clean install and cold launch on Pixel_7 AVD; no AndroidRuntime crash was reported.
- PASS - screenshots were inspected at the physical 1080 px / 420 dpi width (approximately 412 dp) and a temporary 945 px / 420 dpi width (360 dp). The minimal identity scaffold remained centered and unclipped. Screenshots are local QA artifacts and are not committed.
- PASS - source review confirmed a partial FocusScreen, visible STUDENT markers, no prepared component assembly, unused student callbacks and an incomplete ProgressCard body.
- NOT RUN - manual CategoryEditor keyboard/IME interaction from the installed starter, because the editor trigger is intentionally left for student composition. The existing instrumented test did perform text input and passed; full manual editor rehearsal remains a classroom-machine preflight item.

## Deferred to Stage 2/final rehearsal

- Full student-composed FocusScreen.
- Real one-minute loop through student UI.
- Final personalized APK and transfer rehearsal.
- Checkpoint snapshots 02-07.
