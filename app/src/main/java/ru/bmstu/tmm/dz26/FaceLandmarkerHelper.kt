package ru.bmstu.tmm.dz26

import android.content.Context
import android.os.SystemClock
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Обёртка над MediaPipe Face Landmarker (поток LIVE_STREAM):
 * матрица преобразования лица + радужки для оценки направления взгляда.
 */
class FaceLandmarkerHelper(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onResults(result: FaceLandmarkerResult)
        fun onError(message: String)
    }

    private var faceLandmarker: FaceLandmarker? = null
    private val isClosed = AtomicBoolean(false)

    fun setup() {
        if (isClosed.get()) return
        try {
            val baseOptions =
                BaseOptions.builder()
                    .setDelegate(Delegate.CPU)
                    .setModelAssetPath(MP_FACE_LANDMARKER_TASK)
                    .build()
            val options =
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.4f)
                    .setMinFacePresenceConfidence(0.4f)
                    .setMinTrackingConfidence(0.4f)
                    .setOutputFaceBlendshapes(false)
                    .setOutputFacialTransformationMatrixes(true)
                    .setResultListener { res, _ -> listener.onResults(res) }
                    .setErrorListener { e: RuntimeException ->
                        listener.onError(e.message ?: e.toString())
                    }
                    .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            listener.onError("Инициализация FaceLandmarker: ${e.message}")
        }
    }

    fun detectAsync(image: MPImage, timestampMs: Long) {
        val lm = faceLandmarker ?: return
        if (isClosed.get()) return
        try {
            lm.detectAsync(image, timestampMs)
        } catch (e: Exception) {
            listener.onError("detectAsync: ${e.message}")
        }
    }

    fun detectAsync(image: MPImage) {
        detectAsync(image, SystemClock.uptimeMillis())
    }

    fun release() {
        if (isClosed.getAndSet(true)) return
        try {
            faceLandmarker?.close()
        } catch (_: Exception) {
        }
        faceLandmarker = null
    }

    companion object {
        const val MP_FACE_LANDMARKER_TASK = "face_landmarker.task"
    }
}
