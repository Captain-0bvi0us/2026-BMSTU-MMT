# 2026-BMSTU-MMT — GazeTracker

Домашнее задание по предмету «Технология Мультимедиа», МГТУ им. Баумана,
группа ИУ5-63Б, **Козлов А.А.**, **вариант 5**.

> Определение, отслеживание и визуализация значений углов Эйлера в 3D и
> отображение (визуализация) 3D вектора пространственной направленности
> взгляда глаз человека с учетом пространственного положения головы (углов
> Эйлера в 3D) и плюс с учётом возможного наличия / отсутствия прозрачных
> очков в кадрах видеопотока с видеокамеры в реальном масштабе времени.

Платформа: **Android 7.0+ (API 24)**, реализация на Kotlin + Jetpack Compose +
CameraX + MediaPipe Tasks Vision (Face Landmarker).

---

## Структура репозитория

| Папка / файл                                | Что внутри                                                  |
|---------------------------------------------|-------------------------------------------------------------|
| `app/src/main/kotlin/...`                   | Исходный код приложения                                     |
| `app/src/main/res/`                         | Ресурсы Android: строки, темы, иконки                       |
| `app/src/main/assets/face_landmarker.task`  | Модель MediaPipe (3.7 МБ, скачивается скриптом)             |
| `gradle/`, `gradlew`, `gradlew.bat`         | Gradle Wrapper — собирает проект без отдельной установки    |
| `scripts/download-model.ps1` / `.sh`        | Скачать модель MediaPipe в assets                           |
| `docs/WEEK1_SETUP.md`                       | Установка Android Studio, AVD, первый запуск                |
| `docs/KOTLIN_QUICK_START.md`                | Шпаргалка по Kotlin для тех, кто пишет на нём впервые       |

---

## Быстрый старт

1. Прочитай и выполни шаги из [`docs/WEEK1_SETUP.md`](docs/WEEK1_SETUP.md).
2. Если модели MediaPipe нет в `app/src/main/assets/face_landmarker.task` —
   запусти `powershell -ExecutionPolicy Bypass -File scripts/download-model.ps1`
   (Windows) или `bash scripts/download-model.sh` (macOS/Linux).
3. Открой проект в Android Studio, дождись Gradle Sync.
4. Запусти `app` на эмуляторе с Webcam0 в качестве фронтальной камеры.

---

## Прогресс по неделям

- [x] **Неделя 1.** Окружение, превью камеры, прогон MediaPipe Face Landmarker,
      счётчик landmark-точек и FPS на экране.
- [ ] **Неделя 2.** Извлечение углов Эйлера (pitch/yaw/roll) из
      `facialTransformationMatrixes`, вектор взгляда из iris-точек,
      Compose Canvas с осями и стрелкой.
- [ ] **Неделя 3.** Эвристика обнаружения очков, сглаживание (EMA),
      финальный UI, тестирование, отчёт.

---

## Стек

- **Язык:** Kotlin 1.9.22, JVM target 17
- **AGP:** 8.2.2, Gradle 8.4
- **UI:** Jetpack Compose (BOM 2024.02.00), Material 3
- **Камера:** CameraX 1.3.1
- **CV:** MediaPipe Tasks Vision 0.10.26+ (нужна для совместимости с 16 KB page size на Android 15+)
- **Min SDK:** 24, Target SDK: 34
