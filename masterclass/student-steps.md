# Student steps

1. До первого Run изменить app_name.
2. Изменить одну запись в DefaultFocusCategories.
3. Запустить приложение и увидеть персональную identity.
4. Собрать FocusScreen из prepared Compose components.
5. Реализовать body ProgressCard для XP, level и completed sessions.
6. Подключить selected category/duration и соответствующие callbacks.
7. Подключить FocusPrimaryButton к Start callback.
8. Выбрать одну минуту и пройти реальную focus session.
9. Проверить reward и сохранение состояния после restart.
10. Добавить собственную runtime category через editor.
11. Собрать debug APK командой `.\gradlew.bat assembleDebug --offline` (macOS/Linux: `./gradlew assembleDebug --offline`).
12. Найти файл `app/build/outputs/apk/debug/app-debug.apk`, установить/открыть его на контрольном устройстве и передать через classroom channel, указанный преподавателем.

APK переносит приложение и персонализированные source defaults, но не runtime categories, progress и selection из DataStore текущего эмулятора.

Если код перестал собираться и локальная проверка не помогает за 60-90 секунд, попросите преподавателя восстановить ближайший checkpoint. Restore сначала сохранит текущую работу под .recovery/.
