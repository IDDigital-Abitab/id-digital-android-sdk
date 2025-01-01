package uy.com.abitab.iddigitalsdk

import android.content.Context
import androidx.activity.ComponentActivity
import uy.com.abitab.iddigitalsdk.PermissionsManager.registerPermissionLauncher
import uy.com.abitab.iddigitalsdk.activities.LivenessActivity
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import java.io.Serializable

class IDDigitalSDK private constructor() {
    private lateinit var accessToken: String

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
        registerPermissionLauncher(context)
    }

    fun startLiveness(context: Context, document: Document, onError: (IDDigitalError) -> Unit, onCompleted: (String) -> Unit) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val intent = LivenessActivity.createIntent(context, accessToken, document)
        context.startActivity(intent)
    }
}


data class Document(
    val number: String,
    val type: String? = null,
    val country: String? = null
) : Serializable

const val GENERIC_ERROR_MESSAGE = "Ha ocurrido un error"


sealed class IDDigitalError(open val message: String, open val exception: Throwable? = null) : Serializable {
    data class UnknownError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class NetworkError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class CameraPermissionError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class WrongDataError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
}

object CallbackHandler {
    private var onErrorHandler: ((IDDigitalError) -> Unit)? = null
    private var onCompletedHandler: ((String) -> Unit)? = null

    fun setOnErrorHandler(handler: (IDDigitalError) -> Unit) {
        onErrorHandler = handler
    }

    fun setOnCompletedHandler(handler: (String) -> Unit) {
        onCompletedHandler = handler
    }

    fun onError(error: IDDigitalError) {
        onErrorHandler?.invoke(error)
    }

    fun onCompleted(challengeId: String) {
        onCompletedHandler?.invoke(challengeId)
    }
}