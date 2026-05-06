# Неделя 1 — установка и первый запуск

К концу этого документа у тебя на эмуляторе должно быть запущено приложение
`GazeTracker`, которое:

1. Запрашивает разрешение на камеру.
2. Показывает фронтальную камеру на весь экран.
3. В левом верхнем углу выводит счётчик `Точек лица: 478` и FPS детектора.

Если все три пункта работают — Неделя 1 закрыта, переходим к Неделе 2
(углы Эйлера).

---

## 0. Что у нас уже есть в репозитории

Скелет проекта уже создан агентом. Конкретно:

```
2026-BMSTU-MMT/
├── settings.gradle.kts        ← регистрирует :app
├── build.gradle.kts           ← версии Android Gradle Plugin и Kotlin
├── gradle.properties
├── gradle/wrapper/            ← gradle-wrapper.jar и properties
├── gradlew, gradlew.bat       ← запуск Gradle без отдельной установки
├── app/
│   ├── build.gradle.kts       ← зависимости CameraX + MediaPipe + Compose
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/face_landmarker.task   ← модель MediaPipe (3.7 МБ)
│       ├── kotlin/com/bmstu/iu5/gazetracker/
│       │   ├── MainActivity.kt
│       │   ├── permissions/CameraPermissionState.kt
│       │   ├── ui/CameraScreen.kt
│       │   ├── ui/theme/Theme.kt
│       │   └── vision/
│       │       ├── CameraAnalyzer.kt
│       │       └── FaceLandmarkerHelper.kt
│       └── res/   ← иконки, темы, строки
└── scripts/download-model.ps1 ← скрипт перекачки модели, если потеряется
```

---

## 1. Установка Android Studio

1. Скачай **Android Studio Hedgehog (2023.1.1)** или новее с
   [developer.android.com/studio](https://developer.android.com/studio).
2. Запусти инсталлер, выбери **Standard installation**, дождись окончания
   (это занимает ~20 минут — Studio тянет с собой Android SDK ~3 ГБ).
3. На последнем шаге не запускай Studio ещё.

### Установка SDK API 34

После первого запуска Studio:

1. Welcome-экран → `More Actions` → `SDK Manager`.
2. Вкладка **SDK Platforms**: поставь галку напротив **Android 14.0 ("UpsideDownCake")**, API Level 34. Apply.
3. Вкладка **SDK Tools**: убедись что включены
   - **Android SDK Build-Tools 34.0.0**
   - **Android Emulator**
   - **Android SDK Platform-Tools** (это `adb`)
   - **Intel x86 Emulator Accelerator (HAXM)** — только для Intel CPU
   - **Android Emulator Hypervisor Driver for AMD Processors** — только для AMD CPU
   Apply.
4. Дождись окончания скачивания (~1 ГБ).

---

## 2. Создание AVD (виртуального устройства) с веб-камерой

Это критический шаг — у нас нет физического Android-телефона, поэтому
эмулятор должен пробрасывать камеру ноутбука как фронтальную.

1. Welcome-экран → `More Actions` → `Virtual Device Manager` →
   `Create device`.
2. **Phone** → **Pixel 7** → Next.
3. **System Image**:
   - Вкладка **Recommended** → выбираем **API 34 / Android 14.0 / Google APIs / x86_64**.
   - Если нет — вкладка **x86 Images** → скачать `UpsideDownCake / x86_64 / Google APIs`.
   - Иконка скачивания справа от названия — нажми, дождись окончания.
   - Next.
4. Финальный экран — нажми **Show Advanced Settings** (внизу).
5. Найди секцию **Camera**:
   - **Front Camera**: выбери **Webcam0** (это веб-камера твоего ноутбука).
   - **Back Camera**: можно оставить **Emulated**.
6. **Finish**.

> **Если в списке нет `Webcam0`** — это значит, что либо у ноута нет встроенной
> вебки, либо она занята другим приложением (Zoom, Skype, OBS). Закрой их и
> пересоздай AVD. Также проверь, что в Windows в **Параметры → Конфиденциальность → Камера** включён доступ для приложений.

---

## 3. Открытие проекта в Android Studio

1. **Welcome → Open**.
2. Выбери папку `A:\Archive\3_course_6_semester\ТММ\ДЗ\2026-BMSTU-MMT`.
3. Studio спросит, доверяешь ли ты проекту — да, доверяешь.
4. Сразу запустится **Gradle Sync**. В нижней панели (Build Output) ты увидишь
   процесс «Downloading dependencies…». Это займёт ~5-10 минут на первом
   запуске — Studio качает Android Gradle Plugin, Kotlin compiler, AndroidX,
   Compose BOM, CameraX, MediaPipe (~300 МБ суммарно).
5. После окончания sync в панели **Project** (слева сверху) выбери
   режим отображения **Android** — структура станет понятнее.

### Если sync падает с ошибкой

Самые частые причины и решения:

| Ошибка                                                  | Решение                                                            |
|---------------------------------------------------------|--------------------------------------------------------------------|
| `Could not resolve com.android.tools.build:gradle:8.2.2` | Студия слишком старая. Help → Check for Updates → обновить.        |
| `Unsupported Java version`                              | File → Settings → Build → Gradle → **Gradle JDK**: выбрать **17**. |
| `SDK location not found`                                | File → Project Structure → SDK Location → выбрать путь к SDK.      |
| Сеть/прокси                                             | File → Settings → Appearance → System Settings → HTTP Proxy.       |

---

## 4. Запуск на эмуляторе

1. Сверху в тулбаре в выпадашке `Running devices` выбери созданный AVD
   (например, `Pixel 7 API 34`).
2. Нажми зелёную кнопку **Run 'app'** (или Shift+F10).
3. Студия скомпилирует APK (~30 секунд первый раз) и зальёт его на эмулятор.
4. Эмулятор запустится, на нём появится приложение **GazeTracker**.
5. Появится диалог запроса разрешения на камеру → **Allow / Разрешить**.
6. На экране должна появиться картинка с твоей веб-камеры. В левом верхнем
   углу — счётчик `Точек лица: 478` и FPS детектора (10–20 fps на эмуляторе
   это норма).

> **Если экран чёрный** и нет картинки с веб-камеры — открой на эмуляторе
> приложение **Camera** (свайп по home, иконка камеры). Проверь, видит ли
> само приложение Camera твоё лицо. Если нет — проблема в AVD-конфигурации
> Webcam0, см. шаг 2.

---

## 5. Где смотреть логи (Logcat)

В Android Studio внизу есть вкладка **Logcat**.

1. В фильтре сверху выбери `Show only selected application` (по умолчанию).
2. В строке поиска впиши `FaceLandmarker` или `GazeTracker` —
   увидишь, что наш `FaceLandmarkerHelper` пишет.

Полезные теги для фильтра:
- `FaceLandmarkerHelper` — ошибки модели MediaPipe
- `CameraAnalyzer` — ошибки конвертации кадра
- `CameraScreen` — ошибки биндинга CameraX

---

## 5.1. Приложение сразу закрывается + предупреждение про 16 KB

Если Android Studio пишет, что APK **не совместим с 16 KB devices**, и в списке
фигурирует `libimage_processing_util_jni.so` или `libmediapipe_tasks_vision_jni.so`,
а после установки приложение **мгновенно вылетает** — это типичная проблема
**старых AAR MediaPipe**: нативные библиотеки были собраны без выравнивания под
размер страницы памяти **16 KB** (так работают некоторые устройства с Android 15+
и часть эмуляторов).

**Что делать:** в проекте зависимость должна быть **`com.google.mediapipe:tasks-vision:0.10.26`**
или новее (в этом репозитории уже так). После обновления сделай **File → Sync Project
with Gradle Files**, затем **Build → Clean Project** и снова **Run**.

Если после обновления всё равно вылет — открой **Logcat**, фильтр `FATAL` или тег
`AndroidRuntime`, и пришли полный стектрейс (там может быть другая причина: модель
не найдена, камера и т.д.).

---

## 6. Альтернатива: запуск через Cursor + командную строку

Если хочется собирать APK без Studio (например, после правки в Cursor):

```powershell
# Из корня репозитория
.\gradlew.bat assembleDebug
# APK появится в app\build\outputs\apk\debug\app-debug.apk
```

Установить на запущенный эмулятор:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.bmstu.iu5.gazetracker/.MainActivity
```

Но для просмотра Logcat и отладки удобнее всё-таки Studio.

---

## 7. Чек-лист «Неделя 1 закрыта»

- [ ] Android Studio установлена и открывает проект без ошибок sync.
- [ ] AVD `Pixel 7 API 34` создан, веб-камера видна как `Webcam0`.
- [ ] Приложение собирается и запускается на эмуляторе.
- [ ] Картинка с веб-камеры показывается на экране эмулятора.
- [ ] В углу экрана отображается `Точек лица: 478` (когда лицо в кадре) и FPS.
- [ ] При закрытии приложения нет крэшей в Logcat.

Когда всё выше — true, пиши в чат «Неделя 1 готова», переходим к Неделе 2.

---

## 8. Что мы по факту собрали (короткое объяснение архитектуры)

```
                      ┌──────────────────┐
                      │  PreviewView     │  (Android View внутри AndroidView)
                      │  (картинка)      │
   ┌──────────┐       └──────────────────┘
   │ Frontend │              ▲
   │ camera   │──────────────┘
   │ (CameraX)│
   │          │       ┌──────────────────┐    ┌────────────────────┐
   │          │──────▶│ CameraAnalyzer   │───▶│ FaceLandmarkerHelper│
   │          │  YUV  │ (YUV→Bitmap,     │MP  │ (MediaPipe Tasks)  │
   └──────────┘       │  поворот)        │Image│                    │
                      └──────────────────┘    └────────────────────┘
                                                     │
                                                     │ FaceLandmarkerResult
                                                     ▼
                                              ┌──────────────────┐
                                              │ CameraScreen     │
                                              │ (Compose Overlay)│
                                              │ Текст + FPS      │
                                              └──────────────────┘
```

- **CameraX** — стандартная Google-библиотека для работы с камерой. Мы
  заводим два *use-case-а*: `Preview` (рисует кадр на экран) и
  `ImageAnalysis` (отдаёт кадр нам).
- **CameraAnalyzer** реализует интерфейс `ImageAnalysis.Analyzer`. На каждом
  кадре конвертирует YUV→Bitmap, поворачивает, передаёт в обёртку MediaPipe.
- **FaceLandmarkerHelper** загружает модель из `assets/face_landmarker.task`
  и работает в режиме `LIVE_STREAM`. Каждый результат прилетает асинхронно
  в колбэк, мы обновляем `landmarkCount` через `mutableIntStateOf`, Compose
  сам перерисовывает overlay.
- **CameraScreen** — Compose-функция, склеивает всё воедино: разрешения,
  PreviewView через `AndroidView`, overlay-текст в `Box`.

На неделе 2 в `CameraScreen.kt` поверх `PreviewView` добавится Compose `Canvas`,
а в `FaceLandmarkerHelper` мы начнём вытаскивать `facialTransformationMatrixes()`
и считать из них pitch/yaw/roll.
