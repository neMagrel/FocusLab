# Teacher runbook

## Перед стартом

- Использовать canonical focus-lab/starter-v1.
- Ученические AVD очистить и после clear НЕ запускать приложение.
- Teacher demo проводить на отдельном AVD/device.
- Держать доступными 01_starter restore и debug APK output directory.

## 60 минут

| Время | Действие |
| --- | --- |
| 0-3 | Введение и цель приложения |
| 3-6 | Демонстрация final результата на teacher device |
| 6-9 | Идея Android, Kotlin и Compose |
| 9-12 | Открыть starter, NO RUN |
| 12-16 | Изменить app_name и одну default category |
| 16-18 | Первый classroom Run |
| 18-26 | Собрать FocusScreen из prepared components |
| 26-31 | Реализовать ProgressCard |
| 31-36 | Подключить selection wiring |
| 36-39 | Подключить Start callback |
| 39-42 | Реальная 1-minute session |
| 42-47 | Проверить XP и restart persistence |
| 47-52 | Добавить runtime categories |
| 52-56 | Собрать и передать APK |
| 56-60 | Итоги и ссылка на курс |

## Recovery rules

- На локальную ошибку выделять 60-90 секунд.
- Затем восстанавливать ближайший valid checkpoint, а не задерживать группу долгой индивидуальной отладкой.
- К 36-й минуте selection должна работать.
- Restore всегда сохраняет текущие student files под .recovery/.

При нехватке времени приоритет:

~~~text
working screen
-> selection + Start
-> real one-minute session
-> XP + persistence
-> runtime personalization
-> APK
-> bonus
~~~

Checkpoints 02-07 появляются только вместе с Stage 2 reference states. Не выдавать ученику готовый final source из будущего checkpoint раньше соответствующего шага.
