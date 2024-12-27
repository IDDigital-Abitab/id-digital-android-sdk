package uy.com.abitab.iddigitalsdk.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uy.com.abitab.iddigitalsdk.BuildConfig
import uy.com.abitab.iddigitalsdk.Document
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class LivenessService(private val accessToken: String) {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(path: String): String {
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        return "$baseUrl/$path"
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(request)
        }
        .build()

    public suspend fun createChallenge(document: Document): String =
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
                val responseBody = response.body?.string()
                    ?: throw IOException("Cuerpo de la respuesta vacío")

                val json = JSONObject(responseBody)
                val dataObject = json.getJSONObject("data")
                return@withContext dataObject.getString("challengeId")
            }
        }

    public suspend fun executeChallenge(challengeId: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/execute/"))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Error en la solicitud: ${response}")
                }
                val responseBody = response.body?.string()
                    ?: throw IOException("Cuerpo de la respuesta vacío")

                val json = JSONObject(responseBody)
                val dataObject = json.getJSONObject("data")
                return@withContext dataObject.getString("sessionId")
            }
        }

    public suspend fun validateChallenge(challengeId: String): Unit =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .post("{}".toRequestBody(JSON))
                .url(buildUrl("challenges/${challengeId}/validate/"))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Error en la solicitud: ${response}")
                }
                val responseBody = response.body?.string()
                    ?: throw IOException("Cuerpo de la respuesta vacío")

                return@withContext
            }
        }

}