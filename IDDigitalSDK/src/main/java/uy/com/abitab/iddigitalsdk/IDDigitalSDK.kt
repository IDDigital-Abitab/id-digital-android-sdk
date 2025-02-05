package uy.com.abitab.iddigitalsdk

import android.content.Context
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher
import uy.com.abitab.iddigitalsdk.presentation.ui.activities.LivenessActivity
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.internal.sdkModule
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import java.io.Serializable

class IDDigitalSDK private constructor() {
    lateinit var apiKey: String

    companion object {
        private var instance: IDDigitalSDK? = null

        fun getInstance(): IDDigitalSDK {
            if (instance == null) {
                instance = IDDigitalSDK()
            }
            return instance!!
        }
    }



    fun initialize(context: Context, apiKey: String) {
        this.apiKey = apiKey
        AmplifyInitializer.initialize(context)
        registerPermissionLauncher(context)

        startKoin {
            androidContext(context.applicationContext)
            modules(sdkModule)
        }
    }

    fun startLiveness(context: Context, document: Document, onError: (IDDigitalError) -> Unit, onCompleted: (String) -> Unit) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val intent = LivenessActivity.createIntent(context, apiKey, document)
        context.startActivity(intent)
    }
}


const val GENERIC_ERROR_MESSAGE = "Ha ocurrido un error"


sealed class IDDigitalError(open val message: String, open val exception: Throwable? = null) : Serializable {
    data class UnknownError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class NetworkError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class CameraPermissionError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class WrongDataError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
    data class UserCancelledError(override val message: String, override val exception: Throwable? = null) : IDDigitalError(message, exception)
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