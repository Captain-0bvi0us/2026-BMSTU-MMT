# MediaPipe Tasks использует внутренние JNI-библиотеки.
# Сохраняем все классы tasks-vision от обфускации, иначе ломается загрузка модели.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }

# CameraX часть — стандартные правила
-keep class androidx.camera.** { *; }
