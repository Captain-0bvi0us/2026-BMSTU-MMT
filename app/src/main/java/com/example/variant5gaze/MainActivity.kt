package com.example.variant5gaze

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.variant5gaze.databinding.ActivityMainBinding
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var faceAnalyzer: FaceAnalyzer? = null

    private var smoothedEulerX = 0f
    private var smoothedEulerY = 0f
    private var smoothedEulerZ = 0f
    private var smoothedGaze = floatArrayOf(0f, 0f, 1f)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Для работы нужен доступ к камере", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        checkCameraPermissionAndStart()
    }

    override fun onDestroy() {
        faceAnalyzer?.release()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun checkCameraPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            faceAnalyzer = FaceAnalyzer { pose ->
                runOnUiThread {
                    if (pose == null) {
                        binding.infoText.text = "Лицо не обнаружено"
                        binding.statusText.text = "Наведите лицо в центр кадра"
                        binding.overlayView.update(null, isFrontCamera = true)
                        return@runOnUiThread
                    }

                    smoothPose(pose)
                    val smoothResult = pose.copy(
                        eulerX = smoothedEulerX,
                        eulerY = smoothedEulerY,
                        eulerZ = smoothedEulerZ,
                        gazeVector3D = smoothedGaze,
                        gazeEndPoint2D = android.graphics.PointF(
                            ((pose.leftEyeCenter.x + pose.rightEyeCenter.x) / 2f) + smoothedGaze[0] * 280f,
                            ((pose.leftEyeCenter.y + pose.rightEyeCenter.y) / 2f) + smoothedGaze[1] * 280f
                        )
                    )

                    binding.overlayView.update(smoothResult, isFrontCamera = true)
                    binding.infoText.text = buildInfoText(smoothResult)
                    binding.statusText.text = buildStatusText(smoothResult)
                }
            }

            analysis.setAnalyzer(cameraExecutor, faceAnalyzer!!)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun smoothPose(pose: FacePoseResult) {
        val alpha = 0.18f
        smoothedEulerX = blend(smoothedEulerX, pose.eulerX, alpha)
        smoothedEulerY = blend(smoothedEulerY, pose.eulerY, alpha)
        smoothedEulerZ = blend(smoothedEulerZ, pose.eulerZ, alpha)
        smoothedGaze = floatArrayOf(
            blend(smoothedGaze[0], pose.gazeVector3D[0], alpha),
            blend(smoothedGaze[1], pose.gazeVector3D[1], alpha),
            blend(smoothedGaze[2], pose.gazeVector3D[2], alpha)
        )
        smoothedGaze = PoseMath.normalize(smoothedGaze)
    }

    private fun buildInfoText(pose: FacePoseResult): String {
        return """
            Углы Эйлера головы (3D):
            Pitch X: ${fmt(pose.eulerX)}°
            Yaw Y: ${fmt(pose.eulerY)}°
            Roll Z: ${fmt(pose.eulerZ)}°

            Вектор взгляда (3D):
            Gx: ${fmt(pose.gazeVector3D[0])}
            Gy: ${fmt(pose.gazeVector3D[1])}
            Gz: ${fmt(pose.gazeVector3D[2])}
        """.trimIndent()
    }

    private fun buildStatusText(pose: FacePoseResult): String {
        val glassesText = if (pose.glassesLikely) {
            "Контуры глаз устойчивы (прозрачные очки поддерживаются)"
        } else {
            "Поверните лицо к камере для более точного трекинга"
        }
        return "Режим: realtime | $glassesText"
    }

    private fun blend(old: Float, value: Float, alpha: Float): Float = old * (1f - alpha) + value * alpha

    private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)
}
