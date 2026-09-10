# Timer contract

## Durable source of truth

ActiveFocusSession хранит session id, category id, duration, startedAtEpochMillis и absolute endsAtEpochMillis. Remaining seconds не сохраняются.

~~~text
remainingMillis = max(0, endsAtEpochMillis - nowEpochMillis)
~~~

TimeProvider делает расчёты и ViewModel tests детерминированными. Ticker обновляет только derived UI state и не пишет DataStore каждую секунду.

## Start and completion

- Start создаёт session только при валидной category/duration и отсутствии active session.
- Completion принимает expected session id.
- В одной DataStore edit-транзакции начисляются XP, увеличивается completedSessions и очищается activeSession.
- Повторный completion либо mismatched id возвращает no-op и не выдаёт reward второй раз.

## Recovery

- Recreation до endsAt восстанавливает ту же session и вычисляет новое remaining time.
- Recreation после endsAt приводит к completion через repository.
- Одновременные completion signals безопасны благодаря проверке persisted active session id.

Background service, exact alarm и notification не используются. Реальный wall-clock one-minute UI proof относится к Stage 2 после подключения student interaction.
