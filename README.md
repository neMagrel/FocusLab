# Focus Lab

Focus Lab - одностраничное Android-приложение на Kotlin, Jetpack Compose и Material 3 для фокус-сессий, прогресса и пользовательских категорий.

Текущее состояние `main` расширяет завершённую эталонную реализацию мастер-класса стабильной отменой активной focus-session. Канонический classroom starter остаётся доступен по tag `focus-lab/starter-v1`, а происходящий от него immutable reference до последующих расширений - по tag `focus-lab/masterclass-v1`.

Во время countdown кнопка `Отменить фокус` атомарно очищает активную session без начисления XP. Выбранные category/duration и уже накопленный progress сохраняются, поэтому сразу после отмены можно исправить выбор и запустить новый таймер.

## Требования

- Android Studio с совместимым JDK и Android SDK;
- Android SDK Platform 37 для компиляции; targetSdk проекта - 36;
- Gradle wrapper из репозитория;
- ADB и один явно выбранный emulator/device для reset и launch checks;
- заранее прогретый Gradle cache для offline-сборки;
- Python 3 только для cross-platform restore-checkpoint.sh; Windows PowerShell restore не требует Python.

Версии AGP, Kotlin, Compose и Gradle зафиксированы в репозитории. Не обновляйте их перед занятием без отдельного QA.

## Сборка final reference

Windows:

~~~powershell
.\gradlew.bat testDebugUnitTest --offline
.\gradlew.bat assembleDebug --offline
~~~

macOS/Linux:

~~~bash
./gradlew testDebugUnitTest --offline
./gradlew assembleDebug --offline
~~~

Debug APK создаётся в app/build/outputs/apk/debug/app-debug.apk.

Перед выдачей преподаватель устанавливает именно этот файл на контрольный AVD/device и проверяет launch, персональную identity, основные UI-блоки и Start. Для передачи используется заранее проверенный classroom channel: USB-накопитель, локальная общая папка либо утверждённый LMS/messenger. APK остаётся generated build artifact и не хранится в Git.

Копирование APK не переносит DataStore конкретной установки. На новом устройстве приложение получает персонализированные source defaults; runtime categories, progress и selection создаются и сохраняются отдельно на этом устройстве.

## Classroom starter и критическое правило первого запуска

Перед подготовкой ученического состояния переключитесь на `focus-lab/starter-v1`. Starter намеренно содержит partial FocusScreen и ProgressCard stub; final reference содержит полный экран, selection/Start wiring и runtime CategoryEditor flow.

На ученическом AVD сначала очистите данные приложения и после очистки НЕ запускайте starter. Ученик должен изменить app_name и одну default category до первого classroom Run, иначе DataStore уже сохранит исходные defaults.

~~~powershell
.\masterclass\scripts\reset-student-state.ps1 -DeviceSerial emulator-5554
~~~

Если serial не указан, script работает только когда ADB видит ровно одно готовое устройство.

## Student-owned files

- app/src/main/res/values/strings.xml
- app/src/main/java/com/example/focuslab/focus/model/DefaultFocusCategories.kt
- app/src/main/java/com/example/focuslab/focus/FocusScreen.kt
- app/src/main/java/com/example/focuslab/focus/ProgressCard.kt

## Восстановление checkpoint

Перед восстановлением script сохраняет текущие student files под .recovery/, восстанавливает всю согласованную snapshot-группу и запускает offline debug build.

~~~powershell
.\masterclass\scripts\restore-checkpoint.ps1 01_starter
~~~

~~~bash
./masterclass/scripts/restore-checkpoint.sh 01_starter
~~~

Checkpoints 02-07 содержат согласованные snapshots student-owned files. Готовые решения ученика в starter не включены; final source не следует выдавать раньше соответствующего шага.

## Документация

- [Preflight](docs/preflight.md)
- [Architecture](docs/architecture.md)
- [Timer contract](docs/timer-contract.md)
- [Known limitations](docs/known-limitations.md)
- [Teacher runbook](masterclass/teacher-runbook.md)
- [Student steps](masterclass/student-steps.md)
- [Checkpoint map](masterclass/checkpoints/manifest.json)
- [Starter QA evidence](evidence/starter/qa-report.md)
- [Final masterclass evidence](evidence/masterclass-final/stage-2.8-take-home.md)
