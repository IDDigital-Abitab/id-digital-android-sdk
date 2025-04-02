package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.BuildConfig
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.NetworkUtils
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError
import java.security.MessageDigest
import java.util.UUID

class ValidationSessionService(private val httpClient: OkHttpClient, private val context: Context) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(path: String): String {
        val baseUrl = BuildConfig.ID_DIGITAL_BASE_URL.trimEnd('/')
        return "$baseUrl/$path"
    }



//    suspend fun createChallenge(document: Document): String =
//        withContext(Dispatchers.IO) {
//            if (!NetworkUtils.isInternetAvailable(context)) {
//                throw IDDigitalError.NetworkError.NoInternetConnection
//            }
//
//            val data = mapOf(
//                "documentNumber" to document.number,
//                "documentType" to (document.type ?: "ci"),
//                "documentCountry" to (document.country ?: "UY")
//            )
//
//            val jsonObject = JSONObject(data)
//            val requestBody = jsonObject.toString().toRequestBody(JSON)
//
//            val request = Request.Builder()
//                .post(requestBody)
//                .url(buildUrl("challenges/pin/"))
//                .build()
//
//            try {
//                httpClient.newCall(request).execute().use { response ->
//                    val responseBody = response.body.string()
//                    if (!response.isSuccessful) {
//                        throw when (response.code) {
//                            in 500..599 -> IDDigitalError.ServerError.ServiceUnavailable(
//                                response.code,
//                                responseBody
//                            )
//
//                            400, 404 -> IDDigitalError.ServerError.BadResponse(
//                                response.code,
//                                responseBody
//                            )
//
//                            else -> IDDigitalError.ServerError.UnexpectedResponse(
//                                response.code,
//                                responseBody
//                            )
//                        }
//                    }
//                    val json = JSONObject(responseBody)
//                    val dataObject = json.getJSONObject("data")
//                    return@withContext dataObject.getString("challengeId")
//                }
//            } catch (e: Throwable) {
//                throw e.toIDDigitalError("Error in createChallenge")
//            }
//        }


    suspend fun createDeviceAssociation(document: Document): Unit =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.d("ValidationSessionService", "createDeviceAssociation - No internet connection")
                throw IDDigitalError.NetworkError.NoInternetConnection
            }

            val data = mapOf(
                "document_number" to document.number,
                "document_type" to (document.type ?: "ci"),
                "document_country" to (document.country ?: "UY")
            )

            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .post(requestBody)
                .url(buildUrl("associations/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw when (response.code) {
                            in 500..599 -> IDDigitalError.ServerError.ServiceUnavailable(
                                response.code,
                                responseBody
                            )
                            400, 404 -> IDDigitalError.ServerError.BadResponse(
                                response.code,
                                responseBody
                            )
                            else -> IDDigitalError.ServerError.UnexpectedResponse(
                                response.code,
                                responseBody
                            )
                        }
                    }

                    return@withContext
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in createDeviceAssociation")
            }
        }

    suspend fun executeChallenge(challengeId: String, data: Record): Unit =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.d("ValidationSessionService", "executeChallenge - No internet connection")
                throw IDDigitalError.NetworkError.NoInternetConnection
            }

            val request = Request.Builder()
                .post("$data".toRequestBody(JSON)) // Check if this works well
                .url(buildUrl("challenges/${challengeId}/execute/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw when (response.code) {
                            in 500..599 -> IDDigitalError.ServerError.ServiceUnavailable(
                                response.code,
                                responseBody
                            )

                            400, 404 -> IDDigitalError.ServerError.BadResponse(
                                response.code,
                                responseBody
                            )

                            else -> IDDigitalError.ServerError.UnexpectedResponse(
                                response.code,
                                responseBody
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
                Log.d("ValidationSessionService", "validateChallenge - No internet connection")
                throw IDDigitalError.NetworkError.NoInternetConnection
            }
            val gson = Gson()
            val json = gson.toJson(data)

            val request = Request.Builder()
                .post(json.toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/validate/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body.string()
                        val jsonResponse = JSONObject(responseBody)
                        // TODO improve this
                        val backendErrorCode = jsonResponse.getString("code")
                        Log.d("ValidationSessionService", "validateChallenge - backendErrorCode: $backendErrorCode")
                        if (backendErrorCode === "invalid-pin") {
                            return@withContext false
                        }
                        if (backendErrorCode === "too-many-attempts") {
                            throw IDDigitalError.SDKError.TooManyAttempts("too many attempts")
                        }
                        throw when (response.code) {
                            in 500..599 -> IDDigitalError.ServerError.ServiceUnavailable(
                                response.code,
                                responseBody
                            )

                            400, 404 -> IDDigitalError.ServerError.BadResponse(
                                response.code,
                                responseBody
                            )

                            else -> IDDigitalError.ServerError.UnexpectedResponse(
                                response.code,
                                responseBody
                            )
                        }
                    }
                    return@withContext true
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in validateChallenge")
            }
        }
}
