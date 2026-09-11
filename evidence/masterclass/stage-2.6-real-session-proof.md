# Stage 2.6 - Real session proof

Date: 2026-09-11

Result: PASS

Starting boundary: `focus-lab/checkpoint-06-interactive` at `bd49d72140c8dae4dccd8d68bdfab7f27460d675` (`feat(masterclass): wire start interaction (Stage 2.5)`).

## Environment

- Windows host.
- `emulator-5554`, model `sdk_gphone64_x86_64`.
- Android 16 / API 36.
- Physical display 1080x2400 at 420 dpi.
- Debug APK installed with `adb install -r`; existing application data was preserved.

## Automated verification

- PASS - `& 'C:\Users\nemag\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat' -g 'C:\Users\nemag\.gradle' testDebugUnitTest --offline`: 51 tests, 0 failures, 0 errors, 0 skipped.
- PASS - `& 'C:\Users\nemag\.gradle\wrapper\dists\gradle-9.4.1-bin\arn2x92ynaizyzdaamcbpbhtj\gradle-9.4.1\bin\gradle.bat' -g 'C:\Users\nemag\.gradle' assembleDebug --offline`: debug APK assembled successfully.

The Gradle wrapper distribution was not available in the sandbox user's cache. The same Gradle 9.4.1 distribution was run directly from the warmed user cache, with dependency resolution kept offline.

## Real one-minute acceptance

- Device state was allowed to settle through the existing timer/reconciliation flow; DataStore, `endsAt`, supported durations and reward rules were not edited.
- Selected category before start: `Чтение`.
- Selected duration before start: `1 мин`, selected through the normal UI.
- Baseline: XP 70, completed sessions 4, derived level 1.
- Start invoked through the visible primary button at 22:07:12 +05:00.
- PASS - UI entered Running and displayed the selected category.
- PASS - the real countdown decreased through observed values `00:58`, `00:55`, `00:51`, `00:48`, `00:45`, `00:41`, `00:38`, `00:35`, `00:31`, `00:28`, `00:25`, `00:21`, `00:18`, `00:15`, `00:11`, `00:08`, `00:05`, `00:01`.
- PASS - selectors and the primary action were disabled while Running.
- PASS - no negative countdown value appeared.
- PASS - automatic completion was observed at 22:08:16 +05:00.
- PASS - completion feedback displayed `Фокус завершён` and `Чтение · +10 XP`.
- After completion: XP 80, completed sessions 5, derived level 1.
- Actual deltas: XP +10, completed sessions +1.
- PASS - the active session was cleared and `Начать фокус` became available again.

## Reopen and idempotency

- At 22:09:12 +05:00 the app was force-stopped and relaunched normally without clearing application data.
- After reopen: XP 80, completed sessions 5, derived level 1.
- Selected category remained `Чтение`; selected duration remained `1 мин`.
- UI reopened in Idle with `Готовы начать?` and `Начать фокус`; the completed session did not return as Running.
- Actual reopen deltas: XP +0, completed sessions +0.
- PASS - progress and selection persisted.
- PASS - the completed session was not rewarded a second time.

## Scope

- No production source code was changed for Stage 2.6.
- No checkpoint snapshot or checkpoint tag was added.
- Runtime XML and PNG inspection artifacts were intentionally not committed.
- Stage 2.7 runtime personalization and Stage 2.8 take-home work remain deferred.
