package uy.com.abitab.iddigitalsdk.utils

import java.io.IOException
import java.net.ConnectException
import java.net.PortUnreachableException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class IDDigitalError(override val message: String, override val cause: Throwable? = null) :
    Throwable(message, cause)

// Network Errors (Client)
data class NoInternetConnection(override val message: String = "No internet connection.", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class TimeoutError(override val message: String = "Connection timed out.", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class UnknownHostError(override val message: String = "Could not resolve host.", override val cause: Throwable? = null) : IDDigitalError(message, cause)

// Server Errors
data class ServiceUnavailableError(val statusCode: Int, val responseBody: String?, override val message: String = "Service unavailable (code: $statusCode).", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class BadResponseError(val statusCode: Int, val responseBody: String?, override val message: String = "Invalid server response (code: $statusCode).", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class UnexpectedResponseError(val statusCode: Int, val responseBody: String?, override val message: String = "Unexpected server response (code: $statusCode).", override val cause: Throwable? = null) : IDDigitalError(message, cause)

// SDK Usage Errors
data class InvalidApiKeyError(val reason: String, override val message: String = "Invalid API Key: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class NotInitializedError(override val message: String = "IDDigitalSDK has not been initialized. Call initialize() first.", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class InvalidDocumentError(val reason: String, override val message: String = "Invalid document: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class TooManyAttemptsError(val reason: String, override val message: String = "Too many attempts: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class DeviceNotAssociatedError(override val message: String = "Device is not associated", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class UserCannotBeAssociatedError(override val message: String = "User cannot be associated", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class InvalidChallengeIdError(val reason: String, override val message: String = "Invalid challenge ID: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class InvalidPinError(val reason: String, override val message: String = "Invalid PIN: $reason", override val cause: Throwable? = null) : IDDigitalError(message, cause)
data class ChallengeValidationError(override val message: String = "Challenge validation error", override val cause: Throwable? = null) : IDDigitalError(message, cause)

// Other errors
data class CameraPermissionError(override val message: String, override val cause: Throwable? = null) :
    IDDigitalError(message, cause)

data class UserCancelledError(override val message: String, override val cause: Throwable? = null) : IDDigitalError(message, cause)

data class UnknownError(override val message: String, override val cause: Throwable? = null) :
    IDDigitalError(message, cause)



fun Throwable.toIDDigitalError(context: String = "Unknown error"): IDDigitalError = when (this) {
    is IDDigitalError -> this
    is UnknownHostException -> UnknownHostError(cause = this)
    is SocketTimeoutException -> TimeoutError(cause = this)
    is ConnectException -> NoInternetConnection(cause = this)
    is PortUnreachableException -> UnknownHostError(cause = this)
    is IOException -> UnknownError(context, this)
    else -> UnknownError(context, this)
}
