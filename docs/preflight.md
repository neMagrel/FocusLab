# Classroom preflight

## За день

1. Убедиться, что версии не менялись: Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01.
2. На каждой classroom machine выполнить testDebugUnitTest и assembleDebug --offline.
3. Выполнить connectedDebugAndroidTest --offline на контрольном AVD: тесты покрывают reusable UI и ввод в CategoryEditor.
4. Проверить repository/storage/timer tests: seed once, CRUD, persistence, recovery до/после endsAt и idempotent completion.
5. Проверить starter launch и intentional partial UI примерно на 360 dp и 412 dp.
6. Проверить AVD boot, snapshot, hardware acceleration и экранную клавиатуру.
7. Проверить install debug APK из app/build/outputs/apk/debug/app-debug.apk.
8. Проверить restore 01_starter и наличие backup под .recovery/.
9. Проверить способ передачи APK ученикам без использования сети во время занятия.

Stage 2 reference и tag focus-lab/masterclass-v1 ещё не являются результатом Stage 1. Поэтому полный final UI, реальная минутная сессия через student UI и take-home rehearsal остаются будущими Stage 2/final checks.

Stop condition: если starter не собирается offline на конкретной машине, она не готова к занятию. Не планировать загрузку зависимостей во время урока.

## За 30 минут

1. Открыть проект и завершить Gradle sync без сети.
2. Загрузить ученический AVD.
3. Выполнить быстрый assembleDebug --offline.
4. Проверить команду restore-checkpoint для 01_starter.
5. Убедиться, что APK output directory доступен.
6. Проверить projector scaling и системный размер шрифта.
7. Подготовить отдельный teacher demo AVD/device.

## За 5 минут

1. Закрыть Focus Lab на ученическом AVD.
2. Выполнить reset-student-state.ps1 с явным serial либо восстановить проверенный clean AVD snapshot.
3. После clear НЕ запускать starter.
4. Оставить Android Studio с открытым проектом.
5. Оставить AVD загруженным.
6. Не использовать ученический AVD для teacher demo.

Это сохраняет first-run invariant: ученик сначала меняет app_name и default category, а DataStore seed выполняется только при последующем первом Run.
