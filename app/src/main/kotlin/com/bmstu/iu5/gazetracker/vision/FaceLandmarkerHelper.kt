package com.bmstu.iu5.gazetracker.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Тонкая обёртка над MediaPipe Tasks Face Landmarker.
 *
 * Что делает:
 *  - инициализирует модель из assets/face_landmarker.task;
 *  - запускает её в режиме LIVE_STREAM (асинхронный, под видеопоток);
 *  - возвращает результат в коллбэк [onResult] на каждом кадре, где модель
 *    что-то нашла.
 *
 * На неделе 1 нам достаточно убедиться, что MediaPipe работает: счётчик
 * landmark-точек должен прыгать в районе 478 при наличии лица в кадре.
 *
 * @param context нужен только при создании модели, дальше не удерживается.
 * @param onResult колбэк с результатом MediaPipe + временем обработки в мс.
 * @param onError колбэк ошибок (по умолчанию — лог).
 */
class FaceLandmarkerHelper(
    context: Context,
    private val onResult: (FaceLandmarkerResult, inferenceTimeMs: Long) -> Unit,
    private val onError: (String) -> Unit = { Log.e(TAG, it) },
) {

    private val faceLandmarker: FaceLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_PATH)
            // CPU стабильнее на эмуляторах. Для физического телефона можно
            // переключить на Delegate.GPU и сэкономить ~5-10 мс на кадре.
            .setDelegate(Delegate.CPU)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(MIN_FACE_DETECTION_CONFIDENCE)
            .setMinFacePresenceConfidence(MIN_FACE_PRESENCE_CONFIDENCE)
            .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
            // На неделе 2 это нам понадобится для углов Эйлера.
            .setOutputFacialTransformationMatrixes(true)
            .setOutputFaceBlendshapes(false)
            .setResultListener { result, input ->
                val inferenceTime = SystemClock.uptimeMillis() - input.timestampMs
                onResult(result, inferenceTime)
            }
            .setErrorListener { e -> onError(e.message ?: "Unknown MediaPipe error") }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    /**
     * Подаёт кадр в модель.
     *
     * Метод неблокирующий — результат прилетит позже в [onResult] на потоке
     * MediaPipe. Кадр должен быть уже повёрнут в вертикальную ориентацию
     * (см. [com.bmstu.iu5.gazetracker.vision.CameraAnalyzer]).
     *
     * @param bitmap RGBA-битмап кадра.
     * @param timestampMs монотонное время кадра в миллисекундах. ВАЖНО: для
     *   LIVE_STREAM моды timestamp каждого следующего кадра обязан строго
     *   возрастать, иначе MediaPipe бросит исключение.
     */
    fun detectAsync(bitmap: Bitmap, timestampMs: Long) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        faceLandmarker.detectAsync(mpImage, timestampMs)
    }

    fun close() {
        faceLandmarker.close()
    }

    companion object {
        private const val TAG = "FaceLandmarkerHelper"

        private const val MODEL_PATH = "face_landmarker.task"

        private const val MIN_FACE_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_FACE_PRESENCE_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
    }
}
