# Known limitations

Focus Lab v1 намеренно остаётся компактным учебным приложением. В scope нет:

- background service и second-by-second background execution;
- exact alarms и notifications;
- pause и resume session;
- session history, calendar, Todo и daily streak;
- retention pressure и расширенной RPG/gamification;
- login, cloud sync, Firebase, Room и Retrofit;
- Hilt и Navigation ради демонстрации;
- Play Store release/signing;
- сложных тем, color picker и continuous animations.

Debug APK содержит код и персонализированные source defaults, но не экспортирует DataStore установленного приложения. Runtime categories, selection и progress принадлежат конкретной установке и не переносятся на другое устройство простым копированием APK.

## Timer semantics

Активная сессия сохраняет absolute endsAtEpochMillis. После recreation оставшееся время вычисляется из endsAt и текущего wall-clock. Приложение не обещает выполнение coroutine каждую секунду в background и не использует service/alarm. При следующем наблюдении persisted session выполняется reconcile; завершение атомарно и идемпотентно.

## Starter limitation

Tag focus-lab/starter-v1 содержит намеренно неполный FocusScreen и ProgressCard stub. Отсутствие interaction на начальном экране - учебная граница, а не дефект foundation.

Завершённая эталонная реализация находится в `focus-lab/masterclass-v1`; её нельзя путать со starter при подготовке ученических AVD.
