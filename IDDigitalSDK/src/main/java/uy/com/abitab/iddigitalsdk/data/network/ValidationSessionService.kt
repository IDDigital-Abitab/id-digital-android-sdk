package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.CompleteTransactionResult
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.PendingTransaction
import uy.com.abitab.iddigitalsdk.domain.models.PendingTransactionsData
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.utils.ApiResponse
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.DeviceNotAssociatedError
import uy.com.abitab.iddigitalsdk.utils.ForbiddenError
import uy.com.abitab.iddigitalsdk.utils.NetworkUtils
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.SessionHasUncompletedChallengesError
import uy.com.abitab.iddigitalsdk.utils.TooManyAttemptsError
import uy.com.abitab.iddigitalsdk.utils.TransactionNotFoundError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.ValidationSessionNotFoundError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class ValidationSessionService(private val httpClient: OkHttpClient, private val context: Context): BaseService() {
    suspend fun createDeviceAssociation(transactionId: String): ValidationSession =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val data = mapOf("transaction_id" to transactionId)

            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder().post(requestBody).url(buildUrl("associations/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        val backendErrorJson = try {
                            JSONObject(responseBody)
                        } catch (e: Throwable) {
                            null
                        }
                        throw when (backendErrorJson?.optString("code")) {
                            "transaction-not-found" -> TransactionNotFoundError()

                            else -> when (response.code) {
                                in 500..599 -> ServiceUnavailableError(
                                    response.code, responseBody
                                )

                                400, 404, 422 -> BadResponseError(
                                    response.code, responseBody
                                )

                                else -> UnexpectedResponseError(
                                    response.code, responseBody
                                )
                            }
                        }
                    }


                    val gson = Gson()
                    val apiResponseType =
                        object : TypeToken<ApiResponse<ValidationSession>>() {}.type
                    val apiResponse: ApiResponse<ValidationSession> =
                        gson.fromJson(responseBody, apiResponseType)
                    return@withContext apiResponse.data
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in createDeviceAssociation")
            }
        }

    suspend fun completeDeviceAssociation(id: String): DeviceAssociation =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val request =
                Request.Builder().post(EMPTY_REQUEST_BODY).url(buildUrl("associations/$id/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw when (response.code) {
                            in 500..599 -> ServiceUnavailableError(
                                response.code, responseBody
                            )

                            400, 404 -> BadResponseError(
                                response.code, responseBody
                            )

                            else -> UnexpectedResponseError(
                                response.code, responseBody
                            )
                        }
                    }

                    val gson = Gson()
                    val apiResponseType =
                        object : TypeToken<ApiResponse<DeviceAssociation>>() {}.type
                    val apiResponse: ApiResponse<DeviceAssociation> =
                        gson.fromJson(responseBody, apiResponseType)
                    return@withContext apiResponse.data
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in completeDeviceAssociation")
            }
        }

    suspend fun createValidationSession(challengeType: ChallengeType): ValidationSession =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val data = mapOf(
                "challenges_types" to arrayOf(
                    challengeType.toString().lowercase()
                )
            )
            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder().post(requestBody).url(buildUrl("validations/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        val backendErrorCode = try {
                            JSONObject(responseBody).getString("code")
                        } catch (e: Throwable) {
                            null
                        }
                        throw when (backendErrorCode) {
                            // Local storage still has a cached association (isAssociated()
                            // returned true), but the backend no longer recognizes its token
                            // (e.g. deactivated/removed on ID Digital's side). Reusing
                            // DeviceNotAssociatedError here (see IDDigitalSDK.kt) lets callers
                            // clear the stale local association and retry as a fresh
                            // association instead of surfacing a generic unexpected error.
                            "invalid-device-association-token" -> DeviceNotAssociatedError()
                            else -> when (response.code) {
                                in 500..599 -> ServiceUnavailableError(
                                    response.code, responseBody
                                )

                                400, 404 -> BadResponseError(
                                    response.code, responseBody
                                )

                                else -> UnexpectedResponseError(
                                    response.code, responseBody
                                )
                            }
                        }
                    }

                    val gson = Gson()
                    val apiResponseType =
                        object : TypeToken<ApiResponse<ValidationSession>>() {}.type
                    val apiResponse: ApiResponse<ValidationSession> =
                        gson.fromJson(responseBody, apiResponseType)
                    return@withContext apiResponse.data
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in createDeviceAssociation")
            }
        }

    suspend fun executeChallenge(challengeId: String, data: Record): Unit =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val gson = Gson()
            val json = gson.toJson(data)

            val request =
                Request.Builder().post(json.toRequestBody(JSON))
                    .url(buildUrl("challenges/${challengeId}/execute/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw when (response.code) {
                            in 500..599 -> ServiceUnavailableError(
                                response.code, responseBody
                            )

                            400, 404 -> BadResponseError(
                                response.code, responseBody
                            )

                            else -> UnexpectedResponseError(
                                response.code, responseBody
                            )
                        }
                    }

                    return@withContext
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in executeChallenge")
            }
        }

    suspend fun validateChallenge(challengeId: String, data: Record): Boolean =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val gson = Gson()
            val json = gson.toJson(data)

            val request = Request.Builder().post(json.toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/validate/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body.string()
                        val jsonResponse = JSONObject(responseBody)
                        // TODO improve this
                        val backendErrorCode = jsonResponse.getString("code")
                        if (backendErrorCode === "invalid-pin") {
                            return@withContext false
                        }
                        if (backendErrorCode === "too-many-attempts") {
                            throw TooManyAttemptsError(response.message)
                        }
                        throw when (response.code) {
                            in 500..599 -> ServiceUnavailableError(
                                response.code, responseBody
                            )

                            400, 404 -> BadResponseError(
                                response.code, responseBody
                            )

                            else -> UnexpectedResponseError(
                                response.code, responseBody
                            )
                        }
                    }
                    return@withContext true
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in validateChallenge")
            }
        }

    suspend fun completeTransaction(transactionId: String, validationSessionId: String): CompleteTransactionResult =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val data = mapOf(
                "transaction_id" to transactionId,
                "validation_session_id" to validationSessionId
            )
            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request =
                Request.Builder().post(requestBody).url(buildUrl("complete-transaction/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        val backendErrorCode = try {
                            JSONObject(responseBody).getString("code")
                        } catch (e: Throwable) {
                            null
                        }
                        throw when (backendErrorCode) {
                            "transaction-not-found" -> TransactionNotFoundError()
                            "validation-session-not-found" -> ValidationSessionNotFoundError()
                            "session-has-uncompleted-challenges" -> SessionHasUncompletedChallengesError()
                            "forbidden" -> ForbiddenError()
                            else -> when (response.code) {
                                in 500..599 -> ServiceUnavailableError(
                                    response.code, responseBody
                                )

                                400, 404 -> BadResponseError(
                                    response.code, responseBody
                                )

                                else -> UnexpectedResponseError(
                                    response.code, responseBody
                                )
                            }
                        }
                    }

                    // finishUrl puede venir null (backend no pudo generarlo, ver
                    // sdk/views.py complete_oidc_transaction) - nunca se trata como error.
                    // El HTTP 200 ya significa que el backend autorizó la transacción, asi
                    // que un fallo de parseo de finishUrl no debe reportarse como error al
                    // usuario: se devuelve finishUrl=null en su lugar.
                    return@withContext try {
                        val gson = Gson()
                        val apiResponseType =
                            object : TypeToken<ApiResponse<CompleteTransactionResult>>() {}.type
                        val apiResponse: ApiResponse<CompleteTransactionResult>? =
                            gson.fromJson(responseBody, apiResponseType)
                        apiResponse?.data ?: CompleteTransactionResult(finishUrl = null)
                    } catch (e: Throwable) {
                        CompleteTransactionResult(finishUrl = extractFinishUrlLoosely(responseBody))
                    }
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in completeTransaction")
            }
        }

    /**
     * Fallback manual para cuando Gson no puede deserializar el body de
     * complete-transaction pese a un HTTP 200. Extrae `data.finishUrl` con
     * [JSONObject] directamente, tolerando ausencia de la clave o `null`.
     */
    private fun extractFinishUrlLoosely(responseBody: String): String? = try {
        val finishUrlValue = JSONObject(responseBody).optJSONObject("data")?.opt("finishUrl")
        if (finishUrlValue == null || finishUrlValue == JSONObject.NULL) null else finishUrlValue.toString()
    } catch (e: Throwable) {
        null
    }

    /**
     * Lists pending OIDC transactions for the citizen behind the current
     * DeviceAssociation (bearer token added by the OkHttp interceptor, see
     * KoinModule). Used by [IDDigitalSDK.startActiveTransactionPolling] -
     * .docs/sdk/cliente/09-polling-transaccion-activa.md.
     */
    suspend fun getPendingTransactions(): List<PendingTransaction> =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val request =
                Request.Builder().get().url(buildUrl("transactions/pending/")).build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw when (response.code) {
                            in 500..599 -> ServiceUnavailableError(
                                response.code, responseBody
                            )

                            400, 401, 404 -> BadResponseError(
                                response.code, responseBody
                            )

                            else -> UnexpectedResponseError(
                                response.code, responseBody
                            )
                        }
                    }

                    val gson = Gson()
                    val apiResponseType =
                        object : TypeToken<ApiResponse<PendingTransactionsData>>() {}.type
                    val apiResponse: ApiResponse<PendingTransactionsData> =
                        gson.fromJson(responseBody, apiResponseType)
                    return@withContext apiResponse.data.transactions
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in getPendingTransactions")
            }
        }

    suspend fun removeAssociation(): Unit = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            throw NoInternetConnection()
        }
        val request = Request.Builder().delete().url(buildUrl("associations/")).build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body.string()

                if (!response.isSuccessful) {
                    throw when (response.code) {
                        in 500..599 -> ServiceUnavailableError(
                            response.code, responseBody
                        )

                        400, 404, 422 -> BadResponseError(
                            response.code, responseBody
                        )

                        else -> UnexpectedResponseError(
                            response.code, responseBody
                        )
                    }
                }
                return@withContext
            }
        } catch (e: Throwable) {
            throw e.toIDDigitalError("Error in removeAssociation")
        }

    }
}
