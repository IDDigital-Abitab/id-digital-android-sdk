package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ProtocolException
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.BuildConfig
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.NetworkUtils
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError
import java.io.IOException
import java.net.ConnectException

class LivenessService(private val httpClient: OkHttpClient, private val context: Context) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(path: String): String {
        val baseUrl = BuildConfig.ID_DIGITAL_BASE_URL.trimEnd('/')
        return "$baseUrl/$path"
    }

    suspend fun createChallenge(document: Document): String =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw IDDigitalError.NetworkError.NoInternetConnection
            }

            val data = mapOf(
                "documentNumber" to document.number,
                "documentType" to (document.type ?: "ci"),
                "documentCountry" to (document.country ?: "UY")
            )

            val jsonObject = JSONObject(data)
            val requestBody = jsonObject.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .post(requestBody)
                .url(buildUrl("challenges/liveness/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
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
                    val json = JSONObject(responseBody)
                    val dataObject = json.getJSONObject("data")
                    return@withContext dataObject.getString("challengeId")
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in createChallenge")
            }
        }

    suspend fun executeChallenge(challengeId: String): String =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.d("LivenessService", "executeChallenge - No internet connection")
                throw IDDigitalError.NetworkError.NoInternetConnection
            }

            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
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

                    val json = JSONObject(responseBody)
                    val dataObject = json.getJSONObject("data")
                    return@withContext dataObject.getString("sessionId")
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in executeChallenge")
            }
        }

    suspend fun validateChallenge(challengeId: String): Unit =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.d("LivenessService", "validateChallenge - No internet connection")
                throw IDDigitalError.NetworkError.NoInternetConnection
            }

            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/validate/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body.string()
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
                if (e is ProtocolException) {
                    // Ignore ProtocolException. This can happen with a 204 No Content
                    // response that has an (incorrect) Content-Type: application/json header
                    return@withContext
                }
                throw e.toIDDigitalError("Error in validateChallenge")
            }
        }
}
