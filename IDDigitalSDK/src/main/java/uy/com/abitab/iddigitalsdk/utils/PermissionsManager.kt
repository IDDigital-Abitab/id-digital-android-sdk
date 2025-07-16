package uy.com.abitab.iddigitalsdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

object PermissionsManager: PermissionsManagerInterface {

    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var permissionDeferred: CompletableDeferred<Boolean>? = null

    fun registerPermissionLauncher(context: Context) {
        val activity = context as? ComponentActivity ?: return

        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionDeferred?.complete(isGranted)
            permissionDeferred = null
        }
    }

    override fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestCameraPermission(context: Context): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        permissionDeferred = deferred
        cameraPermissionLauncher.launch(CAMERA_PERMISSION)
        return deferred.await()
    }
}
