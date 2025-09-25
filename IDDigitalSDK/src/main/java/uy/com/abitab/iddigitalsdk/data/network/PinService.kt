package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.NetworkUtils
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.TooManyAttemptsError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError
import java.time.Instant

class PinService(private val httpClient: OkHttpClient, private val context: Context): BaseService() {
    suspend fun createChallenge(document: Document): String =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
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
                .url(buildUrl("challenges/pin/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        throw when (response.code) {
                            in 500..599 -> ServiceUnavailableError(
                                response.code,
                                responseBody
                            )

                            400, 404 -> BadResponseError(
                                response.code,
                                responseBody
                            )

                            else -> UnexpectedResponseError(
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

    suspend fun executeChallenge(challengeId: String): Instant? =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
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
                            in 500..599 -> ServiceUnavailableError(
                                response.code,
                                responseBody
                            )

                            400, 404 -> BadResponseError(
                                response.code,
                                responseBody
                            )

                            else -> UnexpectedResponseError(
                                response.code,
                                responseBody
                            )
                        }
                    }
                    val json = JSONObject(responseBody)
                    val dataObject = json.optJSONObject("data")
                    val pinLastUpdatedStr = dataObject?.optString("pinLastUpdated")
                    return@withContext pinLastUpdatedStr?.takeIf { it.isNotBlank() }?.let {
                        Instant.parse(it)
                    }
                }
            } catch (e: Throwable) {
                throw e.toIDDigitalError("Error in executeChallenge")
            }
        }

    suspend fun validateChallenge(challengeId: String, pin: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isInternetAvailable(context)) {
                throw NoInternetConnection()
            }

            try {
                val gson = Gson()
                val requestBody = mapOf("pin" to pin)
                val json = gson.toJson(requestBody)

                val request = Request.Builder()
                    .post(json.toRequestBody(JSON))
                    .url(buildUrl("challenges/${challengeId}/validate/"))
                    .build()


                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body.string()
                        val jsonResponse = JSONObject(responseBody)
                        val backendErrorCode = jsonResponse.getString("code")
                        if (backendErrorCode === "invalid-pin") {
                            return@withContext false
                        }
                        if (backendErrorCode === "too-many-attempts") {
                            throw TooManyAttemptsError("too many pin attempts")
                        }
                        throw when (response.code) {
                            in 500..599 -> ServiceUnavailableError(
                                response.code,
                                responseBody
                            )

                            400, 404 -> BadResponseError(
                                response.code,
                                responseBody
                            )

                            else -> UnexpectedResponseError(
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
