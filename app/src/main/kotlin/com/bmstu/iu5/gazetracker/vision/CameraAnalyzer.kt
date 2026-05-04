package com.bmstu.iu5.gazetracker.vision

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * CameraX-аналайзер: получает каждый кадр от камеры и кормит его в
 * [FaceLandmarkerHelper].
 *
 * Особенности:
 *  - Кадры от CameraX приходят в YUV_420_888. Метод [ImageProxy.toBitmap]
 *    (доступен с camera-core 1.3.0) делает за нас конвертацию в Bitmap.
 *  - Изображение нужно повернуть на rotationDegrees, иначе модель будет
 *    видеть лицо «лёжа на боку» и работать плохо.
 *  - Front-камера даёт зеркальный кадр (mirrored). MediaPipe сам справляется,
 *    зеркалить вручную не нужно — главное правильно отобразить overlay.
 *  - Используем стратегию [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]: если
 *    инференс не успевает за камерой, новые кадры просто перетирают старые.
 *    Так мы не накапливаем очередь и не отстаём.
 *
 * @param onFrame колбэк, в который мы перекладываем готовый Bitmap +
 *   монотонный timestamp. UI-слой сам решает, что с ним сделать —
 *   обычно вызвать `faceHelper.detectAsync(bitmap, ts)`.
 */
class CameraAnalyzer(
    private val onFrame: (bitmap: Bitmap, timestampMs: Long) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotated = bitmap.rotated(imageProxy.imageInfo.rotationDegrees)
            // Используем uptimeMillis: монотонное и в той же шкале, что и
            // timestamps от MediaPipe.
            onFrame(rotated, android.os.SystemClock.uptimeMillis())
        } catch (t: Throwable) {
            // Никогда не даём исключению из аналайзера прибить процесс камеры
            android.util.Log.e(TAG, "Frame analysis failed", t)
        } finally {
            imageProxy.close()
        }
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    companion object {
        private const val TAG = "CameraAnalyzer"
    }
}
