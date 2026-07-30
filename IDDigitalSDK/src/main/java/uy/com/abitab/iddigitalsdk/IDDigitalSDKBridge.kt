package uy.com.abitab.iddigitalsdk

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.NotInitializedError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

/**
 * Entrada Java a las capacidades públicas de [IDDigitalSDK].
 *
 * Los métodos suspendidos de la API Kotlin se ejecutan en un scope de IO y entregan sus
 * resultados mediante listeners compatibles con Java.
 */
object IDDigitalSDKJavaWrapper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sdk: IDDigitalSDK? = null

    /** Recibe errores controlados de la SDK. */
    @FunctionalInterface
    interface OnErrorListener {
        /** @param error error producido por la operación. */
        fun onError(error: IDDigitalError)
    }

    /** Recibe un resultado de texto no nullable. */
    @FunctionalInterface
    interface OnCompletedListener {
        /** @param value resultado producido por la operación. */
        fun onCompleted(value: String)
    }

    /** Recibe el resultado de una asociación completada. */
    @FunctionalInterface
    interface OnAssociationCompletedListener {
        /**
         * @param idToken token OIDC emitido para la asociación.
         * @param validationSessionId sesión validada que permite cerrar la transacción.
         */
        fun onCompleted(idToken: String, validationSessionId: String)
    }

    /** Recibe un resultado booleano. */
    @FunctionalInterface
    interface OnBooleanResultListener {
        /** @param value resultado producido por la operación. */
        fun onResult(value: Boolean)
    }

    /** Recibe la asociación almacenada localmente. */
    @FunctionalInterface
    interface OnDeviceAssociationListener {
        /** @param association asociación actual, o `null` si no existe. */
        fun onSuccess(association: DeviceAssociation?)
    }

    /** Recibe un resultado de texto que puede ser `null`. */
    @FunctionalInterface
    interface OnNullableStringResultListener {
        /** @param value resultado producido por la operación, o `null`. */
        fun onResult(value: String?)
    }

    /** Recibe una transacción pendiente detectada por polling. */
    @FunctionalInterface
    interface OnTransactionDetectedListener {
        /** @param transactionId identificador de la transacción pendiente. */
        fun onTransactionDetected(transactionId: String)
    }

    /**
     * Inicializa la SDK. Debe invocarse antes que cualquier operación, excepto
     * [parseAuthenticationLink].
     *
     * @param context contexto de la aplicación o de una actividad.
     * @param apiKey credencial de la integración.
     * @param environment ambiente de ID Digital.
     * @param onError listener de error.
     * @param onCompleted listener invocado cuando la SDK queda lista.
     * @param baseUrl URL alternativa reservada para desarrollo.
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        apiKey: String,
        environment: IDDigitalSDKEnvironment,
        onError: OnErrorListener?,
        onCompleted: OnCompletedListener?,
        baseUrl: String? = null
    ) {
        try {
            sdk = IDDigitalSDK.initialize(
                context,
                apiKey,
                environment,
                { error -> onError?.onError(error) },
                { result -> onCompleted?.onCompleted(result) },
                baseUrl
            )
        } catch (error: Throwable) {
            onError?.onError(error.toIDDigitalError())
        }
    }

    /**
     * Extrae el `transactionId` de un deep link de autenticación.
     *
     * @param uri URI recibida por la aplicación.
     * @return identificador de transacción, o `null` si la URI no lo contiene.
     */
    @JvmStatic
    fun parseAuthenticationLink(uri: Uri): String? = IDDigitalSDK.parseAuthenticationLink(uri)

    /**
     * Asocia el dispositivo con el ciudadano de una transacción.
     *
     * @param context contexto desde el que se presenta el flujo.
     * @param transactionId identificador recibido por push o deep link.
     * @param onError listener de error.
     * @param onCompleted listener del resultado de asociación.
     */
    @JvmStatic
    fun associate(
        context: Context,
        transactionId: String,
        onError: OnErrorListener?,
        onCompleted: OnAssociationCompletedListener?
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            initializedSdk.associate(
                context,
                transactionId,
                { error -> onError?.onError(error) },
                { idToken, validationSessionId ->
                    onCompleted?.onCompleted(idToken, validationSessionId)
                }
            )
        }
    }

    /**
     * Asocia el dispositivo mediante un QR cross-device.
     *
     * @param context contexto desde el que se presenta la cámara.
     * @param onError listener de error.
     * @param onCompleted listener del `finishUrl` opcional.
     */
    @JvmStatic
    fun associateViaQrScan(
        context: Context,
        onError: OnErrorListener?,
        onCompleted: OnNullableStringResultListener?
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            initializedSdk.associateViaQrScan(
                context,
                { error -> onError?.onError(error) },
                { finishUrl -> onCompleted?.onResult(finishUrl) }
            )
        }
    }

    /**
     * Valida una transacción mediante un QR cross-device.
     *
     * @param context contexto desde el que se presenta la cámara.
     * @param type desafío que completará el ciudadano.
     * @param onError listener de error.
     * @param onCompleted listener del `finishUrl` opcional.
     */
    @JvmStatic
    fun validateViaQrScan(
        context: Context,
        type: ChallengeType,
        onError: OnErrorListener?,
        onCompleted: OnNullableStringResultListener?
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            initializedSdk.validateViaQrScan(
                context,
                type,
                { error -> onError?.onError(error) },
                { finishUrl -> onCompleted?.onResult(finishUrl) }
            )
        }
    }

    /**
     * Consulta si existe una asociación local.
     *
     * @param onError listener de error.
     * @param listener listener del resultado.
     */
    @JvmStatic
    fun isAssociated(onError: OnErrorListener?, listener: OnBooleanResultListener) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            try {
                listener.onResult(initializedSdk.isAssociated())
            } catch (error: Throwable) {
                onError?.onError(error.toIDDigitalError())
            }
        }
    }

    /**
     * Obtiene la asociación almacenada localmente.
     *
     * @param onError listener de error.
     * @param listener listener de la asociación.
     */
    @JvmStatic
    fun getDeviceAssociation(
        onError: OnErrorListener?,
        listener: OnDeviceAssociationListener
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            initializedSdk.getDeviceAssociation(
                { error -> onError?.onError(error) },
                { association -> listener.onSuccess(association) }
            )
        }
    }

    /**
     * Elimina la asociación actual.
     *
     * @param onError listener que recibe [NotInitializedError] si corresponde.
     */
    @JvmStatic
    @JvmOverloads
    fun removeAssociation(onError: OnErrorListener? = null) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            initializedSdk.removeAssociation()
        }
    }

    /**
     * Crea y presenta una sesión de validación.
     *
     * @param context contexto desde el que se presenta el desafío.
     * @param type desafío que completará el ciudadano.
     * @param onError listener de error.
     * @param onCompleted listener del identificador de sesión validada.
     */
    @JvmStatic
    fun createValidationSession(
        context: Context,
        type: ChallengeType,
        onError: OnErrorListener?,
        onCompleted: OnCompletedListener?
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        initializedSdk.createValidationSession(
            context,
            type,
            { error -> onError?.onError(error) },
            { result -> onCompleted?.onCompleted(result) }
        )
    }

    /**
     * Cierra una transacción OIDC con una sesión validada.
     *
     * @param transactionId identificador de la transacción.
     * @param validationSessionId identificador de la sesión validada.
     * @param onError listener de error.
     * @param onCompleted listener del `finishUrl` opcional.
     */
    @JvmStatic
    fun completeTransaction(
        transactionId: String,
        validationSessionId: String,
        onError: OnErrorListener?,
        onCompleted: OnNullableStringResultListener?
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        scope.launch {
            initializedSdk.completeTransaction(
                transactionId,
                validationSessionId,
                { error -> onError?.onError(error) },
                { finishUrl -> onCompleted?.onResult(finishUrl) }
            )
        }
    }

    /**
     * Inicia la detección periódica de transacciones pendientes.
     *
     * @param intervalMs intervalo entre consultas en milisegundos.
     * @param onError listener que recibe [NotInitializedError] si corresponde.
     * @param listener listener de la transacción detectada.
     */
    @JvmStatic
    fun startActiveTransactionPolling(
        intervalMs: Long,
        onError: OnErrorListener?,
        listener: OnTransactionDetectedListener
    ) {
        val initializedSdk = sdkOrReport(onError) ?: return
        initializedSdk.startActiveTransactionPolling(intervalMs) { transactionId ->
            listener.onTransactionDetected(transactionId)
        }
    }

    /**
     * Inicia el polling con el intervalo predeterminado de 10 segundos.
     *
     * @param onError listener que recibe [NotInitializedError] si corresponde.
     * @param listener listener de la transacción detectada.
     */
    @JvmStatic
    fun startActiveTransactionPolling(
        onError: OnErrorListener?,
        listener: OnTransactionDetectedListener
    ) {
        startActiveTransactionPolling(10_000L, onError, listener)
    }

    /**
     * Detiene el polling de transacciones pendientes.
     *
     * @param onError listener que recibe [NotInitializedError] si corresponde.
     */
    @JvmStatic
    @JvmOverloads
    fun stopActiveTransactionPolling(onError: OnErrorListener? = null) {
        val initializedSdk = sdkOrReport(onError) ?: return
        initializedSdk.stopActiveTransactionPolling()
    }

    private fun sdkOrReport(onError: OnErrorListener?): IDDigitalSDK? {
        val initializedSdk = sdk
        if (initializedSdk == null) {
            onError?.onError(NotInitializedError())
        }
        return initializedSdk
    }
}
