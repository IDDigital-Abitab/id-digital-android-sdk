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
import uy.com.abitab.iddigitalsdk.domain.models.CanAssociate
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.utils.ApiResponse
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.NetworkUtils
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.TooManyAttemptsError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class ValidationSessionService(private val httpClient: OkHttpClient, private val context: Context): BaseService() {
    suspend fun checkCanAssociate(document: Document): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val data = mapOf(
                "document_number" to document.number,
                "document_type" to (document.type ?: "ci"),
                "document_country" to (document.country ?: "UY")
            )

            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request =
                Request.Builder().post(requestBody).url(buildUrl("can-associate/")).build()

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
                val apiResponseType = object : TypeToken<ApiResponse<CanAssociate>>() {}.type
                val apiResponse: ApiResponse<CanAssociate> =
                    gson.fromJson(responseBody, apiResponseType)
                return@withContext apiResponse.data.canAssociate
            }
        } catch (e: Throwable) {
            throw e.toIDDigitalError("Error in checkCanAssociate")
        }

    }

    suspend fun createDeviceAssociation(document: Document): ValidationSession =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            val data = mapOf(
                "document_number" to document.number,
                "document_type" to (document.type ?: "ci"),
                "document_country" to (document.country ?: "UY")
            )

            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder().post(requestBody).url(buildUrl("associations/")).build()

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

            val request =
                Request.Builder().post("$data".toRequestBody(JSON)) // Check if this works well
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
            throw e.toIDDigitalError("Error in removeAssociation")
        }

    }


}
