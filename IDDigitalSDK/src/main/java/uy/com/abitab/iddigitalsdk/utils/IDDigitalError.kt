package uy.com.abitab.iddigitalsdk.utils

import java.io.IOException
import java.net.ConnectException
import java.net.PortUnreachableException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


sealed class IDDigitalError(override val message: String, override val cause: Throwable? = null) :
    Throwable(message, cause) {

    // Network Errors (Client)
    sealed class NetworkError(message: String, cause: Throwable? = null) :
        IDDigitalError(message, cause) {
        data object NoInternetConnection :
            NetworkError("No Internet connection.", null) {
            private fun readResolve(): Any = NoInternetConnection
        }

        data class Timeout(val exception: Throwable) :
            NetworkError("Connection timed out.", exception)

        data class UnknownHost(val exception: Throwable) :
            NetworkError("Could not resolve host.", exception)

        data class ServerUnreachable(val exception: Throwable) :
            NetworkError("Network error: ${exception.message}", exception)
    }

    // Server Errors
    sealed class ServerError(message: String, cause: Throwable? = null) :
        IDDigitalError(message, cause) {
        data class ServiceUnavailable(val statusCode: Int, val responseBody: String?) :
            ServerError("Service unavailable (code: $statusCode).", null)

        data class BadResponse(val statusCode: Int, val responseBody: String?) :
            ServerError("Invalid server response (code: $statusCode).", null)

        data class UnexpectedResponse(val statusCode: Int, val responseBody: String?) :
            ServerError("Unexpected server response (code: $statusCode).", null)
    }

    // SDK Usage Errors
    sealed class SDKError(message: String, cause: Throwable? = null) :
        IDDigitalError(message, cause) {
        data class InvalidApiKey(val reason: String) : SDKError("Invalid API Key: $reason")
        data class NotInitialized(val reason: String) : SDKError("SDK not initialized: $reason")
        data class InvalidDocument(val reason: String) : SDKError("Invalid document: $reason")
        data class TooManyAttempts(val reason: String) : SDKError("Too many attempts: $reason")
        data class InvalidChallengeId(val reason: String) : SDKError("Invalid challenge ID: $reason")

    }

    // Other errors
    data class CameraPermissionError(
        override val message: String,
        override val cause: Throwable? = null
    ) :
        IDDigitalError(message, cause)

    data class UserCancelledError(
        override val message: String,
        override val cause: Throwable? = null
    ) : IDDigitalError(message, cause)

    data class UnknownError(override val message: String, override val cause: Throwable? = null) :
        IDDigitalError(message, cause)

    data class TimeoutError(override val message: String, override val cause: Throwable? = null) :
        IDDigitalError(message, cause)
}

fun Throwable.toIDDigitalError(context: String = "Unknown error"): IDDigitalError = when (this) {
    is IDDigitalError -> this
    is UnknownHostException -> IDDigitalError.NetworkError.UnknownHost(this)
    is SocketTimeoutException -> IDDigitalError.NetworkError.Timeout(this)
    is ConnectException -> IDDigitalError.NetworkError.NoInternetConnection
    is PortUnreachableException -> IDDigitalError.NetworkError.UnknownHost(this)
    is IOException -> IDDigitalError.UnknownError(context, this)
    else -> IDDigitalError.UnknownError(context, this)
}