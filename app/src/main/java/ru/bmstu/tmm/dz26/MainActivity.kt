package ru.bmstu.tmm.dz26

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import ru.bmstu.tmm.dz26.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Вариант 5 ДЗ 26: углы Эйлера головы (facial transformation matrix MediaPipe)
 * и оценка направления взгляда по положению радужки; учёт ориентации головы через R.
 * Прозрачные очки: landmarks радужки обычно остаются доступными той же моделью.
 */
class MainActivity : AppCompatActivity(), FaceLandmarkerHelper.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var faceLandmarkerHelper: FaceLandmarkerHelper? = null

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startCamera() else {
                Toast.makeText(this, "Нужно разрешение камеры", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceLandmarkerHelper = FaceLandmarkerHelper(this, this).also { it.setup() }
        binding.overlayView.mirrorX = false

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceLandmarkerHelper?.release()
        faceLandmarkerHelper = null
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(imageProxy)
            }

            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, analysis)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Камера: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val bitmapBuffer = imageProxyToBitmapBuffer(imageProxy)
            val w = bitmapBuffer.width
            val h = bitmapBuffer.height
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, 1f, w.toFloat(), h.toFloat())
            }
            val bm = Bitmap.createBitmap(bitmapBuffer, 0, 0, w, h, matrix, true)
            if (!bm.sameAs(bitmapBuffer)) {
                bitmapBuffer.recycle()
            }
            val mpImage = BitmapImageBuilder(bm).build()
            faceLandmarkerHelper?.detectAsync(mpImage, SystemClock.uptimeMillis())
        } finally {
            imageProxy.close()
        }
    }

    override fun onResults(result: FaceLandmarkerResult) {
        val faces = result.faceLandmarks()
        val matOpt = result.facialTransformationMatrixes()
        if (faces.isEmpty()) {
            runOnUiThread { showEmpty() }
            return
        }
        val landmarks = faces[0]
        val matrix = if (matOpt.isPresent && matOpt.get().isNotEmpty()) {
            val row = matOpt.get()[0]
            FloatArray(16) { i -> row[i] }
        } else {
            null
        }

        val euler = matrix?.let { eulerZYXDegreesFromColumnMajor4x4(it) }
        val gazeCam = computeGazeInCameraFrame(landmarks, matrix)

        val nose = landmarks[NOSE_TIP]
        val yaw = euler?.first ?: Float.NaN
        val pitch = euler?.second ?: Float.NaN
        val roll = euler?.third ?: Float.NaN

        val text = buildString {
            appendLine("ТММ ДЗ26, вариант 5 — голова + взгляд (очки/без очков)")
            appendLine()
            appendLine("Углы Эйлера (ZYX, градус), ориентация головы:")
            appendLine("  yaw   = ${fmt(yaw)}")
            appendLine("  pitch = ${fmt(pitch)}")
            appendLine("  roll  = ${fmt(roll)}")
            appendLine()
            appendLine("Вектор взгляда (СК камеры, нормализованный):")
            appendLine(
                "  gx = ${fmt(gazeCam[0])}  gy = ${fmt(gazeCam[1])}  gz = ${fmt(gazeCam[2])}"
            )
            appendLine()
            appendLine("Оси на кадре: X красный, Y зелёный, Z синий.")
            appendLine("Жёлтая стрелка — взгляд; пурпурная — ось «вперёд» головы.")
        }

        runOnUiThread {
            binding.tvStats.text = text
            binding.overlayView.updateFrame(
                OverlayView.Frame(
                    yawDeg = yaw,
                    pitchDeg = pitch,
                    rollDeg = roll,
                    gazeCamera = gazeCam,
                    noseNormX = nose.x(),
                    noseNormY = nose.y(),
                    rotation4x4ColumnMajor = matrix
                )
            )
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            binding.tvStats.text = message
        }
    }

    private fun showEmpty() {
        binding.tvStats.text =
            "Лицо не найдено — встаньте в кадр (освещение, без сильного засвета)."
        binding.overlayView.updateFrame(null)
    }

    private fun fmt(v: Float): String =
        if (v.isNaN()) "—" else String.format(java.util.Locale.US, "%.2f", v)

    private fun imageProxyToBitmapBuffer(image: ImageProxy): Bitmap {
        val plane = image.planes[0]
        val buf = plane.buffer.duplicate()
        buf.rewind()
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val w = image.width + rowPadding / pixelStride.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buf)
        return if (rowPadding == 0) {
            bitmap
        } else {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            bitmap.recycle()
            cropped
        }
    }

    companion object {
        private const val NOSE_TIP = 1
        private val LEFT_EYE = intArrayOf(
            33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246
        )
        private val RIGHT_EYE = intArrayOf(
            362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388,
            387, 386, 385, 384, 398
        )
        private val LEFT_IRIS = intArrayOf(468, 469, 470, 471, 472)
        private val RIGHT_IRIS = intArrayOf(473, 474, 475, 476, 477)

        /** 3×3 из верхнего левого блока, **column-major** 4×4: R[i,j] = m[j*4+i]. */
        fun mulRotUpper3(m: FloatArray, v: FloatArray): FloatArray {
            val x = v[0]
            val y = v[1]
            val z = v[2]
            return floatArrayOf(
                m[0] * x + m[4] * y + m[8] * z,
                m[1] * x + m[5] * y + m[9] * z,
                m[2] * x + m[6] * y + m[10] * z
            )
        }

        /** Tait–Bryan ZYX (градусы); матрица вращения column-major в m. */
        fun eulerZYXDegreesFromColumnMajor4x4(m: FloatArray): Triple<Float, Float, Float> {
            val r00 = m[0].toDouble()
            val r10 = m[1].toDouble()
            val r20 = m[2].toDouble()
            val r21 = m[6].toDouble()
            val r22 = m[10].toDouble()
            val yaw = atan2(r10, r00)
            val pitch = atan2(-r20, hypot(r21, r22))
            val roll = atan2(r21, r22)
            return Triple(
                Math.toDegrees(yaw).toFloat(),
                Math.toDegrees(pitch).toFloat(),
                Math.toDegrees(roll).toFloat()
            )
        }

        fun avg(landmarks: List<NormalizedLandmark>, idx: IntArray, out: FloatArray) {
            var x = 0f
            var y = 0f
            var z = 0f
            for (i in idx) {
                val l = landmarks[i]
                x += l.x()
                y += l.y()
                z += l.z()
            }
            val n = idx.size.toFloat()
            out[0] = x / n
            out[1] = y / n
            out[2] = z / n
        }

        fun sub(a: FloatArray, b: FloatArray, out: FloatArray) {
            out[0] = a[0] - b[0]
            out[1] = a[1] - b[1]
            out[2] = a[2] - b[2]
        }

        fun normInPlace(v: FloatArray) {
            val n = hypot(hypot(v[0].toDouble(), v[1].toDouble()), v[2].toDouble()).toFloat()
                .coerceAtLeast(1e-6f)
            v[0] /= n
            v[1] /= n
            v[2] /= n
        }

        fun computeGazeInCameraFrame(
            landmarks: List<NormalizedLandmark>,
            m4: FloatArray?
        ): FloatArray {
            if (landmarks.size < 478) {
                return floatArrayOf(0f, 0f, -1f)
            }
            val leye = FloatArray(3)
            val liris = FloatArray(3)
            val reye = FloatArray(3)
            val riris = FloatArray(3)
            avg(landmarks, LEFT_EYE, leye)
            avg(landmarks, LEFT_IRIS, liris)
            avg(landmarks, RIGHT_EYE, reye)
            avg(landmarks, RIGHT_IRIS, riris)
            val gL = FloatArray(3)
            val gR = FloatArray(3)
            sub(liris, leye, gL)
            sub(riris, reye, gR)
            normInPlace(gL)
            normInPlace(gR)
            val gx = gL[0] + gR[0]
            val gy = gL[1] + gR[1]
            val gz = gL[2] + gR[2]
            val glen = hypot(hypot(gx.toDouble(), gy.toDouble()), gz.toDouble()).toFloat()
                .coerceAtLeast(1e-6f)
            val gazeLocal = floatArrayOf(gx / glen, gy / glen, gz / glen)
            return if (m4 != null) {
                val r = mulRotUpper3(m4, gazeLocal)
                val rl = hypot(hypot(r[0].toDouble(), r[1].toDouble()), r[2].toDouble()).toFloat()
                    .coerceAtLeast(1e-6f)
                floatArrayOf(r[0] / rl, r[1] / rl, r[2] / rl)
            } else {
                gazeLocal
            }
        }
    }
}
