package uy.com.abitab.iddigitalsdk.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ProtocolException
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.BuildConfig
import uy.com.abitab.iddigitalsdk.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

class LivenessService(private val accessToken: String) {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(path: String): String {
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        return "$baseUrl/$path"
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Api-Key $accessToken")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Error en la solicitud: ${response.code}")
                }
                val responseBody = response.body.string()

                val json = JSONObject(responseBody)
                val dataObject = json.getJSONObject("data")
                return@withContext dataObject.getString("challengeId")
            }
        }

    suspend fun executeChallenge(challengeId: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/execute/"))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Error en la solicitud: ${response}")
                }
                val responseBody = response.body.string()

                val json = JSONObject(responseBody)
                val dataObject = json.getJSONObject("data")
                return@withContext dataObject.getString("sessionId")
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
                        throw IOException("Error en la solicitud: ${response}")
                    }
                }
            } catch (e: ProtocolException) {
                // Avoid crashing when the server returns a 204 No Content response with application/json content type
            }
            return@withContext
        }
}
