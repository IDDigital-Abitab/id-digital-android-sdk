package uy.com.abitab.iddigitalsdk

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.LivenessActivity
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.di.sdkModule
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.PinActivity
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError

class IDDigitalSDK private constructor(
    private val apiKey: String
) {

    companion object {
        private var instance: IDDigitalSDK? = null
        private var isKoinStarted = false
        private lateinit var applicationContext: Context

        fun initialize(context: Context, apiKey: String): IDDigitalSDK {
            if (instance == null) {
                applicationContext = context.applicationContext
                instance = IDDigitalSDK(apiKey)
                AmplifyInitializer.initialize(context)
                registerPermissionLauncher(context)
                startKoinIfNeeded(context)
            }
            return instance!!
        }

        private fun startKoinIfNeeded(context: Context) {
            if (isKoinStarted) return
            startKoin {
                androidContext(context.applicationContext)
                modules(sdkModule())
            }
            isKoinStarted = true
        }

        internal fun getApiKey(): String {
            return instance?.apiKey
                ?: throw IllegalStateException("IDDigitalSDK has not been initialized. Call initialize() first.")

        }
    }

    fun startLiveness(
        context: Context,
        document: Document,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val intent = LivenessActivity.createIntent(context, document)
        context.startActivity(intent)
    }

    fun requestPin(
        context: Context,
        document: Document,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val intent = PinActivity.createIntent(context, document)
        context.startActivity(intent)
    }
}


const val GENERIC_ERROR_MESSAGE = "Ha ocurrido un error"



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