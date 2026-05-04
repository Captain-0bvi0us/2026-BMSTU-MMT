package com.bmstu.iu5.gazetracker.ui

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bmstu.iu5.gazetracker.R
import com.bmstu.iu5.gazetracker.permissions.rememberCameraPermissionState
import com.bmstu.iu5.gazetracker.vision.CameraAnalyzer
import com.bmstu.iu5.gazetracker.vision.FaceLandmarkerHelper
import java.util.concurrent.Executors

/**
 * Главный экран приложения для недели 1:
 *  - запрашивает разрешение CAMERA;
 *  - выводит превью фронтальной камеры на весь экран;
 *  - параллельно прогоняет каждый кадр через MediaPipe Face Landmarker;
 *  - в углу показывает счётчик найденных landmark-точек и FPS детектора.
 *
 * На неделях 2-3 поверх PreviewView мы добавим Compose Canvas с осями
 * системы координат головы и стрелкой направления взгляда.
 */
@Composable
fun CameraScreen() {
    val permission = rememberCameraPermissionState()

    if (!permission.granted) {
        PermissionStub(onGrantClick = permission.requestPermission)
        return
    }

    CameraContent()
}

@Composable
private fun CameraContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ---------- Состояние UI: счётчики, которые перерисовывают overlay ----------
    var landmarkCount by remember { mutableIntStateOf(0) }
    var detectorFps by remember { mutableFloatStateOf(0f) }

    // FPS считаем как скользящее среднее по последним 10 кадрам.
    val fpsTracker = remember { FpsTracker(windowSize = 10) }

    // ---------- MediaPipe-обёртка живёт в remember, чтобы не пересоздавалась ----------
    val faceHelper = remember {
        FaceLandmarkerHelper(
            context = context,
            onResult = { result, _ ->
                landmarkCount = result.faceLandmarks().firstOrNull()?.size ?: 0
                detectorFps = fpsTracker.tick()
            },
        )
    }

    // Закрываем модель, когда экран уходит из дерева, иначе утечёт нативная память.
    DisposableEffect(faceHelper) {
        onDispose { faceHelper.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { previewView ->
                bindCameraUseCases(
                    context = context,
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    onFrame = { bitmap, ts -> faceHelper.detectAsync(bitmap, ts) },
                )
            },
        )

        OverlayHud(
            landmarkCount = landmarkCount,
            detectorFps = detectorFps,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )
    }
}

@Composable
private fun OverlayHud(
    landmarkCount: Int,
    detectorFps: Float,
    modifier: Modifier = Modifier,
) {
    val landmarkText = if (landmarkCount > 0) {
        stringResource(R.string.overlay_landmarks_template, landmarkCount)
    } else {
        stringResource(R.string.overlay_face_not_detected)
    }
    val fpsText = stringResource(R.string.overlay_fps_template, detectorFps)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = landmarkText,
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = fpsText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun PermissionStub(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.permission_required_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
        )
        Text(
            text = stringResource(R.string.permission_required_body),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        Button(onClick = onGrantClick) {
            Text(text = stringResource(R.string.permission_grant_button))
        }
    }
}

/**
 * Конфигурирует CameraX use-case-ы и привязывает их к жизненному циклу.
 *
 * Use-case-ов два:
 *  - [Preview]      — рисует кадры в [PreviewView];
 *  - [ImageAnalysis] — отдаёт каждый кадр в [CameraAnalyzer], а тот в MediaPipe.
 *
 * Камера выбирается фронтальная: для нашей задачи (трекинг взгляда)
 * это единственный осмысленный вариант.
 */
private fun bindCameraUseCases(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onFrame: (android.graphics.Bitmap, Long) -> Unit,
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener(
        {
            val cameraProvider = providerFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analyzer = CameraAnalyzer(onFrame = onFrame)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        analyzer,
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis,
                )
            } catch (t: Throwable) {
                android.util.Log.e(TAG, "Failed to bind camera use-cases", t)
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

private const val TAG = "CameraScreen"

/**
 * Простой счётчик частоты вызовов: возвращает скользящий FPS по последним
 * [windowSize] событиям. Не потокобезопасен — вызываем строго с одного
 * потока (MediaPipe-listener).
 */
private class FpsTracker(private val windowSize: Int) {
    private val timestamps = ArrayDeque<Long>(windowSize + 1)

    fun tick(): Float {
        val now = android.os.SystemClock.uptimeMillis()
        timestamps.addLast(now)
        while (timestamps.size > windowSize) timestamps.removeFirst()
        if (timestamps.size < 2) return 0f
        val durationMs = timestamps.last() - timestamps.first()
        if (durationMs <= 0) return 0f
        return (timestamps.size - 1) * 1000f / durationMs
    }
}
