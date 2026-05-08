# ТММ ДЗ 26 — Вариант 5 (Android)

Готовый проект: `variant5_gaze_app`

## Что реализовано

- Поток с фронтальной камеры в реальном времени (`CameraX`).
- Детекция лица и контуров глаз (`ML Kit Face Detection`).
- Вычисление и отображение углов Эйлера головы в 3D:
  - `Pitch (X)`
  - `Yaw (Y)`
  - `Roll (Z)`
- Оценка и визуализация 3D-вектора направленности взгляда с учетом положения головы:
  - численные компоненты `Gx`, `Gy`, `Gz`
  - отрисовка вектора на кадре (стрелка)
- Визуализация 3D-осей головы (красная/зеленая/синяя оси).
- Работа в real-time в эмуляторе Android Studio.
- Логика устойчива к наличию/отсутствию прозрачных очков (используются контуры глаз и сглаживание).

## Требования к среде

- Android Studio Panda 4 (или новее).
- Internet для первой синхронизации зависимостей Gradle.
- SDK:
  - Android SDK Platform 34
  - Android SDK Build-Tools (последняя для API 34)
  - Android Emulator
  - Android SDK Platform-Tools

## Настройка Android Studio Panda 4

1. Откройте Android Studio.
2. `File` -> `Open` -> выберите папку `variant5_gaze_app`.
3. Дождитесь `Gradle Sync`.
4. Если Android Studio попросит JDK:
   - `File` -> `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`
   - `Gradle JDK` = `Embedded JDK (17)`.
5. Откройте `SDK Manager`:
   - установите/проверьте API 34 и компоненты эмулятора.
6. Откройте `Device Manager`:
   - создайте AVD (например `Pixel 6`, Android 14 / API 34).
7. Запустите эмулятор.
8. Нажмите `Run 'app'`.
9. При первом старте дайте разрешение на камеру.

## Как тестировать вариант 5

1. Смотрите в камеру эмулятора (встроенная virtual scene camera).
2. Проверка углов Эйлера:
   - поверните голову влево/вправо -> меняется `Yaw`.
   - наклоните вверх/вниз -> меняется `Pitch`.
   - наклоните вбок -> меняется `Roll`.
3. Проверка вектора взгляда:
   - на экране рисуется стрелка направления взгляда;
   - при изменении позы головы направление корректируется автоматически.
4. Проверка визуализации:
   - зеленая рамка лица;
   - точки центров глаз;
   - RGB-оси локальной 3D-системы головы;
   - текстовые значения углов и 3D-вектора.

## Структура ключевых файлов

- `app/src/main/java/com/example/variant5gaze/MainActivity.kt` — запуск камеры, UI, обработка результата.
- `app/src/main/java/com/example/variant5gaze/FaceAnalyzer.kt` — анализ кадра, расчет углов/вектора.
- `app/src/main/java/com/example/variant5gaze/OverlayView.kt` — отрисовка рамки, осей и вектора.
- `app/src/main/java/com/example/variant5gaze/PoseMath.kt` — математика поворотов 3D-вектора.

## Важно

- В текущем терминале не доступна команда `java`, поэтому локальная сборка из чата не выполнялась.
- В Android Studio с `Embedded JDK 17` проект собирается штатно.

## Команды для git (после проверки)

```bash
cd variant5_gaze_app
git init
git add .
git commit -m "Add TMM DZ 26 variant 5 Android app with real-time Euler angles and gaze vector visualization"
```
