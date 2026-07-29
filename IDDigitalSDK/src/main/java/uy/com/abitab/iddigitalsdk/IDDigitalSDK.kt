package uy.com.abitab.iddigitalsdk

import android.content.Context
import android.net.Uri
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
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteTransactionUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateValidationSessionUseCase
// import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.RemoveAssociationUseCase
// import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateChallengeUseCase
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.DeviceAssociationActivity
import uy.com.abitab.iddigitalsdk.presentation.qr_association.ui.QrAssociationActivity
import uy.com.abitab.iddigitalsdk.presentation.qr_validation.ui.QrValidationActivity
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.ValidationSessionActivity
import uy.com.abitab.iddigitalsdk.utils.ActiveTransactionPoller
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.DeviceNotAssociatedError
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

/**
 * Punto de entrada de la SDK de ID Digital.
 *
 * La aplicación debe obtener una única instancia mediante [initialize] y conservarla durante
 * su ciclo de vida. Los métodos que presentan un desafío abren interfaces propias de la SDK y
 * comunican el resultado mediante callbacks.
 */
class IDDigitalSDK private constructor() {
    private var removeAssociationUseCase: RemoveAssociationUseCase
    private var pinDataStoreManager: PinDataStoreManager
    private var createValidationSessionUseCase: CreateValidationSessionUseCase
    private var completeTransactionUseCase: CompleteTransactionUseCase
    private var activeTransactionPoller: ActiveTransactionPoller
//    private var executeChallengeUseCase: ExecuteChallengeUseCase
//    private var validateChallengeUseCase: ValidateChallengeUseCase

    private val koin by lazy { GlobalContext.get() }

    init {
        removeAssociationUseCase = koin.get()
        pinDataStoreManager = koin.get()
        createValidationSessionUseCase = koin.get()
        completeTransactionUseCase = koin.get()
        activeTransactionPoller = koin.get()
//        executeChallengeUseCase = koin.get()
//        validateChallengeUseCase = koin.get()
    }


    /** Crea y conserva la instancia compartida de la SDK. */
    companion object {
        private var instance: IDDigitalSDK? = null
        private var isKoinStarted = false
        private lateinit var applicationContext: Context
        private lateinit var koinInstance: Koin

        /**
         * Inicializa la SDK y devuelve su instancia compartida.
         *
         * Debe invocarse una sola vez al iniciar la aplicación. Las invocaciones posteriores
         * devuelven la misma instancia y no repiten la inicialización.
         *
         * @param context contexto de la aplicación o de una actividad.
         * @param apiKey credencial de la integración entregada por ID Digital.
         * @param environment ambiente de ID Digital contra el que operará la SDK.
         * @param onError se invoca si falla la inicialización de los servicios requeridos.
         * @param onCompleted se invoca cuando la SDK queda lista para operar.
         * @param baseUrl URL base alternativa reservada para desarrollo y pruebas. En una
         * integración normal debe permanecer en `null`.
         * @return la instancia compartida de [IDDigitalSDK].
         */
        fun initialize(
            context: Context,
            apiKey: String,
            environment: IDDigitalSDKEnvironment,
            onError: (IDDigitalError) -> Unit,
            onCompleted: (String) -> Unit,
            baseUrl: String? = null
        ): IDDigitalSDK {
            if (instance == null) {
                applicationContext = context.applicationContext
                if (!isKoinStarted) {
                    val koinApp = startKoin {
                        androidContext(context.applicationContext)
                        modules(sdkModule())
                        properties(
                            buildMap {
                                put("apiKey", apiKey)
                                put("environment", environment.name)
                                if (!baseUrl.isNullOrBlank()) {
                                    // Override para desarrollo/testing contra un backend propio
                                    // (ver BaseService.kt); nunca usado por STAGING/PRODUCTION.
                                    put("customBaseUrl", baseUrl)
                                }
                            }
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

        /**
         * Extrae el identificador de transacción de un deep link de autenticación.
         *
         * La aplicación puede probar este método con cualquier URI entrante. Si la URI no
         * pertenece al flujo de ID Digital, por ejemplo si es el retorno propio de Keycloak,
         * el método devuelve `null`.
         *
         * @param uri URI recibida por la aplicación.
         * @return el `transactionId` del parámetro de consulta, o `null` si no está presente.
         */
        fun parseAuthenticationLink(uri: Uri): String? = uri.getQueryParameter("transactionId")
    }

    /**
     * Asocia el dispositivo con el ciudadano de una transacción pendiente.
     *
     * La SDK presenta el flujo de asociación y sus desafíos. El backend identifica al
     * ciudadano a partir de [transactionId], por lo que la aplicación no debe solicitar ni
     * enviar su documento.
     *
     * @param context contexto desde el que se abrirá la interfaz de asociación.
     * @param transactionId identificador recibido por push o mediante
     * [parseAuthenticationLink].
     * @param onError se invoca si la asociación no puede iniciarse o completarse.
     * @param onCompleted se invoca con el `idToken` de la asociación y el identificador de
     * la sesión validada. Este último debe pasarse a [completeTransaction] para cerrar el
     * login web pendiente.
     */
    suspend fun associate(
        context: Context,
        transactionId: String,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (idToken: String, validationSessionId: String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnAssociationCompletedHandler(onCompleted)

        try {
            val intent = DeviceAssociationActivity.createIntent(context, transactionId)
            context.startActivity(intent)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }

    }

    /**
     * Asocia el dispositivo escaneando un QR mostrado en otro dispositivo.
     *
     * La SDK abre su cámara, obtiene del QR una transacción firmada y ejecuta internamente
     * la asociación, los desafíos y el cierre de la transacción. La aplicación no debe
     * invocar [completeTransaction] después de este método.
     *
     * Este flujo siempre es cross-device. La SDK no abre el `finishUrl`, porque el navegador
     * del otro dispositivo completa el login mediante polling.
     *
     * @param context contexto desde el que se abrirá la cámara.
     * @param onError se invoca si el escaneo o la asociación no pueden completarse.
     * @param onCompleted se invoca con el `finishUrl` resultante, si existe, únicamente para
     * observabilidad de la aplicación.
     */
    suspend fun associateViaQrScan(
        context: Context,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (finishUrl: String?) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnQrAssociationCompletedHandler(onCompleted)

        try {
            val intent = QrAssociationActivity.createIntent(context)
            context.startActivity(intent)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }
    }

    /**
     * Valida una transacción escaneando un QR mostrado en otro dispositivo.
     *
     * Debe utilizarse cuando [isAssociated] devuelve `true`. La SDK abre su cámara, ejecuta
     * el desafío seleccionado y cierra internamente la transacción. Si el dispositivo no
     * está asociado, informa [DeviceNotAssociatedError].
     *
     * @param context contexto desde el que se abrirá la cámara.
     * @param type desafío que completará el ciudadano después del escaneo.
     * @param onError se invoca si el dispositivo no está asociado o el flujo no puede
     * completarse.
     * @param onCompleted se invoca con el `finishUrl` resultante, si existe, únicamente para
     * observabilidad de la aplicación.
     */
    suspend fun validateViaQrScan(
        context: Context,
        type: ChallengeType,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (finishUrl: String?) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnQrAssociationCompletedHandler(onCompleted)

        try {
            if (!isAssociated()) {
                onError(DeviceNotAssociatedError())
                return
            }
            val intent = QrValidationActivity.createIntent(context, type)
            context.startActivity(intent)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }
    }

    /**
     * Indica si el dispositivo conserva una asociación local.
     *
     * No realiza llamadas de red. El resultado permite elegir entre [associate] y
     * [createValidationSession], o entre [associateViaQrScan] y [validateViaQrScan].
     *
     * @return `true` si existe una asociación almacenada localmente.
     */
    suspend fun isAssociated(): Boolean {
        val context = applicationContext
        val retrievedAssociation = context.getDeviceAssociation().firstOrNull()
        return retrievedAssociation != null
    }

    /**
     * Obtiene la asociación almacenada localmente.
     *
     * @param onError se invoca si no puede leerse el almacenamiento seguro.
     * @param onSuccess se invoca con la asociación existente, o con `null` si el dispositivo
     * no está asociado.
     */
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

    /**
     * Elimina del backend y del dispositivo la asociación actual.
     *
     * La limpieza local se ejecuta incluso si el backend no puede eliminar la asociación.
     * Este método no propaga errores; un fallo remoto queda registrado en el log de Android.
     */
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

    /**
     * Crea y completa una sesión de validación para un dispositivo asociado.
     *
     * La SDK presenta la interfaz del desafío seleccionado. Después de completarlo, la
     * aplicación debe usar el identificador recibido para invocar [completeTransaction].
     *
     * @param context contexto desde el que se abrirá la interfaz de validación.
     * @param type desafío que completará el ciudadano.
     * @param onError se invoca si la sesión no puede crearse o validarse.
     * @param onCompleted se invoca con el identificador de la sesión validada.
     */
    fun createValidationSession(
        context: Context,
        type: ChallengeType,
        onError: (IDDigitalError) -> Unit,
        onCompleted: (validationSessionId: String) -> Unit
    ) {
        CallbackHandler.setOnErrorHandler(onError)
        CallbackHandler.setOnCompletedHandler(onCompleted)

        val intent = ValidationSessionActivity.createIntent(context, type)
        context.startActivity(intent)
    }

    /**
     * Inicia la detección periódica de transacciones de validación pendientes.
     *
     * Es un canal redundante al push y solo funciona en dispositivos asociados. El polling
     * se pausa cuando la aplicación queda en segundo plano y se detiene después de detectar
     * la primera transacción, evitando entregar repetidamente el mismo identificador. La
     * aplicación debe volver a iniciarlo después de resolver esa transacción.
     *
     * @param intervalMs intervalo entre consultas mientras la aplicación está en primer
     * plano. El valor predeterminado es 10 segundos.
     * @param onTransactionDetected se invoca con el `transactionId` pendiente más antiguo.
     * Debe resolverse con [createValidationSession] y [completeTransaction].
     */
    fun startActiveTransactionPolling(
        intervalMs: Long = ActiveTransactionPoller.DEFAULT_INTERVAL_MS,
        onTransactionDetected: (transactionId: String) -> Unit
    ) {
        activeTransactionPoller.start(intervalMs, onTransactionDetected)
    }

    /** Detiene el polling iniciado por [startActiveTransactionPolling], si está activo. */
    fun stopActiveTransactionPolling() {
        activeTransactionPoller.stop()
    }

    /**
     * Cierra una transacción OIDC utilizando una sesión de validación completada.
     *
     * En un flujo same-device la aplicación debe abrir el `finishUrl` con el sistema. En un
     * flujo iniciado por push o QR cross-device no debe abrirlo, porque el navegador original
     * completa el login por polling.
     *
     * @param transactionId identificador recibido por push o mediante
     * [parseAuthenticationLink].
     * @param validationSessionId identificador devuelto por [associate] o
     * [createValidationSession] para el mismo ciudadano.
     * @param onError se invoca si la transacción o la sesión no son válidas o no pueden
     * completarse.
     * @param onSuccess se invoca con el `finishUrl` generado, o con `null` cuando el navegador
     * debe completar el flujo exclusivamente mediante polling.
     */
    suspend fun completeTransaction(
        transactionId: String,
        validationSessionId: String,
        onError: (IDDigitalError) -> Unit,
        onSuccess: (finishUrl: String?) -> Unit
    ) {
        try {
            val result = completeTransactionUseCase(transactionId, validationSessionId)
            onSuccess(result.finishUrl)
        } catch (e: Throwable) {
            onError(e.toIDDigitalError())
        }
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
     * Envía a Keycloak el token de una asociación para continuar el flujo de autenticación.
     *
     * @param tabId identificador de la pestaña de autenticación de Keycloak.
     * @param sessionCode código de la sesión de autenticación.
     * @param clientId identificador del cliente configurado en Keycloak.
     * @param realm realm de Keycloak.
     * @param sdkToken token OIDC obtenido de [DeviceAssociation.idToken].
     * @param onError se invoca si Keycloak rechaza la solicitud o no puede alcanzarse.
     * @param onSuccess se invoca con la respuesta devuelta por Keycloak.
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

/**
 * Mecanismo interno para comunicar resultados desde las Activities de la SDK.
 *
 * @suppress
 */
object CallbackHandler {
    private var onErrorHandler: ((IDDigitalError) -> Unit)? = null
    private var onCompletedHandler: ((String) -> Unit)? = null
    private var onAssociationCompletedHandler: ((idToken: String, validationSessionId: String) -> Unit)? = null
    private var onQrAssociationCompletedHandler: ((finishUrl: String?) -> Unit)? = null

    fun setOnErrorHandler(handler: (IDDigitalError) -> Unit) {
        onErrorHandler = handler
    }

    fun setOnCompletedHandler(handler: (String) -> Unit) {
        onCompletedHandler = handler
    }

    fun setOnAssociationCompletedHandler(handler: (idToken: String, validationSessionId: String) -> Unit) {
        onAssociationCompletedHandler = handler
    }

    fun setOnQrAssociationCompletedHandler(handler: (finishUrl: String?) -> Unit) {
        onQrAssociationCompletedHandler = handler
    }

    fun onError(error: IDDigitalError) {
        onErrorHandler?.invoke(error)
    }

    fun onCompleted(value: String) {
        onCompletedHandler?.invoke(value)
    }

    fun onAssociationCompleted(idToken: String, validationSessionId: String) {
        onAssociationCompletedHandler?.invoke(idToken, validationSessionId)
    }

    fun onQrAssociationCompleted(finishUrl: String?) {
        onQrAssociationCompletedHandler?.invoke(finishUrl)
    }
}