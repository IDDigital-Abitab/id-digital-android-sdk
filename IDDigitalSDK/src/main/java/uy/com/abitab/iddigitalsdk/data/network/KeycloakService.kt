package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.NetworkUtils
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class KeycloakService(
    private val httpClient: OkHttpClient,
    private val context: Context
) : BaseService() {

    private fun getKeycloakBaseUrl(): String {
        val environmentName: String? = getKoin().getProperty("environment")
        val environment = try {
            IDDigitalSDKEnvironment.valueOf(environmentName ?: IDDigitalSDKEnvironment.PRODUCTION.name)
        } catch (e: IllegalArgumentException) {
            IDDigitalSDKEnvironment.PRODUCTION
        }
        return when (environment) {
            IDDigitalSDKEnvironment.STAGING -> "https://bqm-keycloak-dev.alabamasolutions.com"
            IDDigitalSDKEnvironment.PRODUCTION -> "https://bqm-keycloak.alabamasolutions.com"
        }
    }

    private fun buildKeycloakUrl(realm: String, path: String): String {
        val baseUrl = getKeycloakBaseUrl()
        return "$baseUrl/realms/$realm/$path"
    }

    suspend fun sendAuthenticationData(
        tabId: String,
        sessionCode: String,
        clientId: String,
        realm: String,
        sdkToken: String,
    ): String = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isInternetAvailable(context)) {
            throw NoInternetConnection()
        }

        val formBody = FormBody.Builder()
            .add("grant_type", "password")
            .add("session_code", sessionCode)
            .add("tab_id", tabId)
            .add("client_id", clientId)
            .add("sdk_token", sdkToken)
            .build()

        val url = buildKeycloakUrl(realm, "protocol/openid-connect/token")
        val request = Request.Builder()
            .post(formBody)
            .url(url)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
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

                return@withContext responseBody
            }
        } catch (e: Throwable) {
            throw e.toIDDigitalError("Error sending data to Keycloak")
        }
    }
}

