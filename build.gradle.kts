// Корневой build-файл — здесь объявляем плагины с версиями,
// а подключаем их уже в app/build.gradle.kts
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
