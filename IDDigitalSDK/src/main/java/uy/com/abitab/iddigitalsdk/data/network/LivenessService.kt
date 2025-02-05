package uy.com.abitab.iddigitalsdk.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ProtocolException
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.BuildConfig
import uy.com.abitab.iddigitalsdk.domain.models.Document
import java.io.IOException

class LivenessService(private val httpClient: OkHttpClient) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(path: String): String {
        val baseUrl = BuildConfig.ID_DIGITAL_BASE_URL.trimEnd('/')
        return "$baseUrl/$path"
    }

    suspend fun createChallenge(document: Document): String =
        withContext(Dispatchers.IO) {
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
                    if (!response.isSuccessful) {
                        throw LivenessError.ServerError(response.code, response.body.string())
                    }
                    val responseBody = response.body.string()

                    val json = JSONObject(responseBody)
                    val dataObject = json.getJSONObject("data")
                    return@withContext dataObject.getString("challengeId")
                }
            } catch (e: Throwable) {
                when (e) {
                    is LivenessError.ServerError -> throw e
                    is IOException -> throw LivenessError.NetworkError(e)
                    else -> throw LivenessError.UnknownError(e)
                }
            }
        }

    suspend fun executeChallenge(challengeId: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/execute/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw LivenessError.ServerError(response.code, response.body.string())
                    }
                    val responseBody = response.body.string()

                    val json = JSONObject(responseBody)
                    val dataObject = json.getJSONObject("data")
                    return@withContext dataObject.getString("sessionId")
                }
            } catch (e: Throwable) {
                when (e) {
                    is LivenessError.ServerError -> throw e
                    is IOException -> throw LivenessError.NetworkError(e)
                    else -> throw LivenessError.UnknownError(e)
                }
            }
        }

    suspend fun validateChallenge(challengeId: String): Unit =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/validate/"))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body.string()
                        throw LivenessError.ServerError(response.code, errorBody)
                    }
                    return@withContext
                }
            } catch (e: Throwable) {
                when (e) {
                    is ProtocolException -> {
                        // ignore this error
                        return@withContext
                    }
                    is LivenessError.ServerError -> throw e
                    is IOException -> throw LivenessError.NetworkError(e)
                    else -> throw LivenessError.UnknownError(e)
                }
            }
        }
}

sealed class LivenessError(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null
) :
    Throwable(message, cause) {
    data class NetworkError(val exception: IOException) :
        LivenessError(1, "Error de red: ${exception.message}", exception)

    data class ServerError(val statusCode: Int, val responseBody: String) : // Cambiado
        LivenessError(2, "Error del servidor: $statusCode - $responseBody")

    data class UnknownError(val exception: Throwable) :
        LivenessError(3, "Error desconocido: ${exception.message}", exception)
}