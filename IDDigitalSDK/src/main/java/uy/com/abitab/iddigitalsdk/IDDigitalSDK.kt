package uy.com.abitab.iddigitalsdk

import android.content.Context
import android.util.Log
import getDeviceAssociation
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin
import removeDeviceAssociation
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.di.sdkModule
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.usecases.CheckCanAssociateUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateValidationSessionUseCase
// import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.RemoveAssociationUseCase
// import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateChallengeUseCase
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.DeviceAssociationActivity
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.ValidationSessionActivity
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher
import uy.com.abitab.iddigitalsdk.utils.UserCannotBeAssociatedError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class IDDigitalSDK private constructor() {
    private var checkCanAssociateUseCase: CheckCanAssociateUseCase
    private var removeAssociationUseCase: RemoveAssociationUseCase
    private var pinDataStoreManager: PinDataStoreManager
    private var createValidationSessionUseCase: CreateValidationSessionUseCase
//    private var executeChallengeUseCase: ExecuteChallengeUseCase
//    private var validateChallengeUseCase: ValidateChallengeUseCase

    private val koin by lazy { GlobalContext.get() }

    init {
        checkCanAssociateUseCase = koin.get()
        removeAssociationUseCase = koin.get()
        pinDataStoreManager = koin.get()
        createValidationSessionUseCase = koin.get()
//        executeChallengeUseCase = koin.get()
//        validateChallengeUseCase = koin.get()
    }


    companion object {
        private var instance: IDDigitalSDK? = null
        private var isKoinStarted = false
        private lateinit var applicationContext: Context
        private lateinit var koinInstance: Koin

        fun initialize(
            context: Context,
            apiKey: String,
            environment: IDDigitalSDKEnvironment,
            onError: (IDDigitalError) -> Unit,
            onCompleted: (String) -> Unit
        ): IDDigitalSDK {
            if (instance == null) {
                applicationContext = context.applicationContext
                if (!isKoinStarted) {
                    val koinApp = startKoin {
                        androidContext(context.applicationContext)
                        modules(sdkModule())
                        properties(
                            mapOf(
                                "apiKey" to apiKey,
                                "environment" to environment.name
                            )
                        )
                    }
                    koinInstance = koinApp.koin
                    isKoinStarted = true
                }
                instance = IDDigitalSDK()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AmplifyInitializer.initialize(context)
                        onCompleted("IDDigitalSDK initialized successfully")
                    } catch (e: Throwable) {
                        onError(e.toIDDigitalError())
                    }
                }

                registerPermissionLauncher(context)

            }
            return instance!!
        }
    }

    suspend fun associate(
        context: Context,
        document: Document,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        try {
            val canAssociate = checkCanAssociateUseCase(document)
            if (!canAssociate) {
                onError(UserCannotBeAssociatedError())
                return
            }
            val intent = DeviceAssociationActivity.createIntent(context, document)
            context.startActivity(intent)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }

    }

    suspend fun canAssociate(
        document: Document,
        onError: (IDDigitalError) -> Unit,
    ): Boolean {
        try {
            return checkCanAssociateUseCase(document)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
            return false
        }

    }

    suspend fun isAssociated(): Boolean {
        val context = applicationContext
        val retrievedAssociation = context.getDeviceAssociation().firstOrNull()
        return retrievedAssociation != null
    }

    suspend fun getDeviceAssociation(
        onError: (IDDigitalError) -> Unit,
        onSuccess: (DeviceAssociation?) -> Unit
    ) {
        try {
            val context = applicationContext
            val association = context.getDeviceAssociation().firstOrNull()
            onSuccess(association)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }
    }

    suspend fun removeAssociation() {
        val context = applicationContext
        try {
            removeAssociationUseCase()
        } catch (e: Throwable) {
            Log.e("IDDigitalSDK", "Error removing association", e)
        }
        runBlocking {
            context.removeDeviceAssociation()
            pinDataStoreManager.clearPinAndBiometricPreference()
        }
    }

    fun createValidationSession(
        context: Context,
        type: ChallengeType,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val intent = ValidationSessionActivity.createIntent(context, type)
        context.startActivity(intent)
    }

//    suspend fun executeChallenge(
//        challengeId: String,
//        data: Record,
//        onError: (IDDigitalError) -> Unit,
//        onCompleted: () -> Unit
//    ) {
//        try {
//            executeChallengeUseCase(challengeId, data)
//            onCompleted()
//        } catch (e: Throwable) {
//            onError(e.toIDDigitalError())
//        }
//    }

//    suspend fun validateChallenge(
//        challengeId: String,
//        data: Record,
//        onError: (IDDigitalError) -> Unit,
//        onResult: (Boolean) -> Unit
//    ) {
//        try {
//            val isValid = validateChallengeUseCase(challengeId, data)
//            onResult(isValid)
//        } catch (e: Throwable) {
//            onError(e.toIDDigitalError())
//        }
//    }

    /**
     * @param sdkToken OIDC ID token (JWT) from `DeviceAssociation.idToken`, sent to Keycloak as `sdk_token`.
     */
    suspend fun sendToKeycloak(
        tabId: String,
        sessionCode: String,
        clientId: String,
        realm: String,
        sdkToken: String,
        onError: (IDDigitalError) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        try {
            val keycloakService: uy.com.abitab.iddigitalsdk.data.network.KeycloakService = koin.get()
            val response = keycloakService.sendAuthenticationData(tabId, sessionCode, clientId, realm, sdkToken)
            onSuccess(response)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }
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

    fun onCompleted(value: String) {
        onCompletedHandler?.invoke(value)
    }
}