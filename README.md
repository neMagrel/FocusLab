# Focus Lab

Focus Lab - одностраничное Android-приложение на Kotlin, Jetpack Compose и Material 3 для фокус-сессий, прогресса и пользовательских категорий.

Текущее состояние - classroom starter. Инфраструктура приложения готова и протестирована, но видимый экран намеренно не завершён: ученик собирает Compose UI, реализует ProgressCard и подключает selection/Start callbacks на мастер-классе.

## Требования

- Android Studio с совместимым JDK и Android SDK;
- Android SDK Platform 37 для компиляции; targetSdk проекта - 36;
- Gradle wrapper из репозитория;
- ADB и один явно выбранный emulator/device для reset и launch checks;
- заранее прогретый Gradle cache для offline-сборки;
- Python 3 только для cross-platform restore-checkpoint.sh; Windows PowerShell restore не требует Python.

Версии AGP, Kotlin, Compose и Gradle зафиксированы в репозитории. Не обновляйте их перед занятием без отдельного QA.

## Быстрая проверка starter

Windows:

~~~powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug --offline
~~~

macOS/Linux:

~~~bash
./gradlew testDebugUnitTest
./gradlew assembleDebug --offline
~~~

Debug APK создаётся в app/build/outputs/apk/debug/app-debug.apk.

## Критическое правило первого запуска

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

Checkpoints 02-07 добавляются только вместе с соответствующими Stage 2 reference states. Готовые решения ученика в starter не включены.

## Документация

- [Preflight](docs/preflight.md)
- [Architecture](docs/architecture.md)
- [Timer contract](docs/timer-contract.md)
- [Known limitations](docs/known-limitations.md)
- [Teacher runbook](masterclass/teacher-runbook.md)
- [Student steps](masterclass/student-steps.md)
- [Checkpoint map](masterclass/checkpoints/manifest.json)
- [Starter QA evidence](evidence/starter/qa-report.md)
