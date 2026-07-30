package uy.com.abitab.iddigitalsdk.utils

import java.io.IOException
import java.net.ConnectException
import java.net.PortUnreachableException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Error controlado informado por la SDK mediante sus callbacks `onError`.
 *
 * @property message descripción técnica apta para registro y diagnóstico.
 * @property cause excepción original, cuando está disponible.
 */
sealed class IDDigitalError(override val message: String, override val cause: Throwable? = null) :
    Throwable(message, cause)

/** El dispositivo no dispone de conexión a Internet. */
data class NoInternetConnection(override val message: String = "No internet connection.", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La operación de red superó el tiempo máximo de espera. */
data class TimeoutError(override val message: String = "Connection timed out.", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** El dispositivo no pudo resolver el host del servicio. */
data class UnknownHostError(override val message: String = "Could not resolve host.", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * El servicio no está disponible temporalmente.
 *
 * @property statusCode código HTTP recibido.
 * @property responseBody cuerpo recibido, si existe.
 */
data class ServiceUnavailableError(val statusCode: Int, val responseBody: String?, override val message: String = "Service unavailable (code: $statusCode).", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * El servicio devolvió una respuesta inválida para la solicitud.
 *
 * @property statusCode código HTTP recibido.
 * @property responseBody cuerpo recibido, si existe.
 */
data class BadResponseError(val statusCode: Int, val responseBody: String?, override val message: String = "Invalid server response (code: $statusCode).", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * El servicio devolvió una respuesta que la SDK no reconoce.
 *
 * @property statusCode código HTTP recibido.
 * @property responseBody cuerpo recibido, si existe.
 */
data class UnexpectedResponseError(val statusCode: Int, val responseBody: String?, override val message: String = "Unexpected server response (code: $statusCode).", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * La API key no es válida para la operación o el ambiente seleccionado.
 *
 * @property reason detalle informado por el servicio.
 */
data class InvalidApiKeyError(val reason: String, override val message: String = "Invalid API Key: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La aplicación intentó utilizar la SDK antes de inicializarla. */
data class NotInitializedError(override val message: String = "IDDigitalSDK has not been initialized. Call initialize() first.", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * Los datos de documento recibidos no son válidos.
 *
 * @property reason detalle de validación.
 */
data class InvalidDocumentError(val reason: String, override val message: String = "Invalid document: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * El ciudadano agotó la cantidad de intentos permitidos para el desafío.
 *
 * @property reason detalle informado por el servicio.
 */
data class TooManyAttemptsError(val reason: String, override val message: String = "Too many attempts: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La operación requiere una asociación local que no existe o dejó de ser válida. */
data class DeviceNotAssociatedError(override val message: String = "Device is not associated", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * El identificador de desafío no existe o no puede utilizarse.
 *
 * @property reason detalle de validación.
 */
data class InvalidChallengeIdError(val reason: String, override val message: String = "Invalid challenge ID: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/**
 * El PIN ingresado no cumple el formato o la validación requerida.
 *
 * @property reason detalle de validación.
 */
data class InvalidPinError(val reason: String, override val message: String = "Invalid PIN: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** El desafío no pudo validarse correctamente. */
data class ChallengeValidationError(override val message: String = "Challenge validation error", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La transacción OIDC indicada no existe o ya no está disponible. */
data class TransactionNotFoundError(override val message: String = "OIDC transaction not found", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La sesión de validación indicada no existe o ya no está disponible. */
data class ValidationSessionNotFoundError(override val message: String = "Validation session not found", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La sesión todavía contiene desafíos pendientes. */
data class SessionHasUncompletedChallengesError(override val message: String = "Validation session has uncompleted challenges", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La transacción y la sesión pertenecen a ciudadanos diferentes. */
data class ForbiddenError(override val message: String = "Forbidden: transaction and validation session belong to different citizens", override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** La aplicación no concedió el permiso de cámara necesario para el flujo. */
data class CameraPermissionError(override val message: String, override val cause: Throwable? = null) :
    IDDigitalError(message, cause)

/** El ciudadano canceló el flujo presentado por la SDK. */
data class UserCancelledError(override val message: String, override val cause: Throwable? = null) : IDDigitalError(message, cause)

/** Ocurrió un error que no puede clasificarse en una categoría más específica. */
data class UnknownError(override val message: String, override val cause: Throwable? = null) :
    IDDigitalError(message, cause)



/**
 * Conversión interna de excepciones a errores públicos.
 *
 * @suppress
 */
internal fun Throwable.toIDDigitalError(context: String = "Unknown error"): IDDigitalError = when (this) {
    is IDDigitalError -> this
    is UnknownHostException -> UnknownHostError(cause = this)
    is SocketTimeoutException -> TimeoutError(cause = this)
    is ConnectException -> NoInternetConnection(cause = this)
    is PortUnreachableException -> UnknownHostError(cause = this)
    is IOException -> UnknownError(unknownErrorMessage(context, this), this)
    else -> UnknownError(unknownErrorMessage(context, this), this)
}

/**
 * El `context` por si solo (ej. "Error completing QR transaction") no dice nada sobre
 * la causa real - incluir el tipo/mensaje de la excepción original evita tener que
 * correlacionar con logs de backend para diagnosticar un error inesperado.
 */
private fun unknownErrorMessage(context: String, cause: Throwable): String {
    val causeDescription = cause.message?.let { "${cause::class.simpleName}: $it" }
        ?: cause::class.simpleName
        ?: "unknown"
    return "$context ($causeDescription)"
}
