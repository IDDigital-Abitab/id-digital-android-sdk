package uy.com.abitab.iddigitalsdk

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import uy.com.abitab.iddigitalsdk.activities.LivenessActivity
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import java.io.Serializable

class IDDigitalSDK private constructor() {

    private lateinit var accessToken: String
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    companion object {
        private var instance: IDDigitalSDK? = null

        fun getInstance(): IDDigitalSDK {
            if (instance == null) {
                instance = IDDigitalSDK()
            }
            return instance!!
        }
    }

    fun initialize(context: ComponentActivity, accessToken: String) {
        this.accessToken = accessToken

        AmplifyInitializer.initialize(context)

        cameraPermissionLauncher = context.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Log.e("IDDigitalSDK", "Permiso de cámara denegado.")
            }
        }
    }

    fun startLiveness(context: Context, document: Document) {
        if (!PermissionsManager.hasCameraPermission(context)) {
            Log.d("IDDigitalSDK", "Solicitando permiso de cámara...")
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            return
        }

        val intent = LivenessActivity.createIntent(context, accessToken, document)
        context.startActivity(intent)
    }
}

data class Document(
    val number: String,
    val type: String? = null,
    val country: String? = null
): Serializable
