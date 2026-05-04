package com.bmstu.iu5.gazetracker.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Простейшая обёртка над runtime-разрешением CAMERA для Compose.
 *
 * @property granted true, если пользователь уже выдал разрешение.
 * @property requestPermission лямбда, которую можно вызвать из UI, чтобы
 *  показать системный диалог запроса.
 */
data class CameraPermissionState(
    val granted: Boolean,
    val requestPermission: () -> Unit,
)

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
    }

    // Автоматически запрашиваем разрешение при первом входе в экран.
    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    return CameraPermissionState(
        granted = granted,
        requestPermission = { launcher.launch(Manifest.permission.CAMERA) },
    )
}
