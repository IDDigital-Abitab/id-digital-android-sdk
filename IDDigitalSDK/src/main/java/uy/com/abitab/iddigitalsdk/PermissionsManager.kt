package uy.com.abitab.iddigitalsdk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object PermissionsManager {

    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /**
     * Verifica si el permiso de cámara ha sido concedido.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita el permiso de cámara usando un launcher de permisos.
     */
    fun requestCameraPermission(
        launcher: ActivityResultLauncher<String>
    ) {
        launcher.launch(CAMERA_PERMISSION)
    }
}
