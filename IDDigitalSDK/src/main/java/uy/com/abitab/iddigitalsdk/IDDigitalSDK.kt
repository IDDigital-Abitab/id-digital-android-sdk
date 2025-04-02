package uy.com.abitab.iddigitalsdk

import android.content.Context
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.mp.KoinPlatform.getKoin
import uy.com.abitab.iddigitalsdk.di.sdkModule
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateAndLaunchLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateAndLaunchPinChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher

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
                startKoinIfNeeded(context)
                instance = IDDigitalSDK(apiKey)
                AmplifyInitializer.initialize(context)
                registerPermissionLauncher(context)
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

    fun associateDevice(
        context: Context,
        document: Document,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)
        // TODO
        //        val intent = DeviceAssociationActivity.createIntent(context, document)
        //        context.startActivity(intent)
    }

    suspend fun startLiveness(
        document: Document,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val createAndLaunchLivenessChallengeUseCase: CreateAndLaunchLivenessChallengeUseCase = getKoin().get()
        createAndLaunchLivenessChallengeUseCase(document)
    }

    suspend fun requestPin(
        document: Document,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val createAndLaunchPinChallengeUseCase: CreateAndLaunchPinChallengeUseCase = getKoin().get()
        createAndLaunchPinChallengeUseCase(document)
    }
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