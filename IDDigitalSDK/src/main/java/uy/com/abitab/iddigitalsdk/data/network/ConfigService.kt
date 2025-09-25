package uy.com.abitab.iddigitalsdk.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import uy.com.abitab.iddigitalsdk.domain.models.ConfigData
import uy.com.abitab.iddigitalsdk.utils.ApiResponse
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class ConfigService(private val httpClient: OkHttpClient):
    BaseService() {

    suspend fun getConfiguration(): ConfigData = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .get()
            .url(buildUrl("initialize/"))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                if (!response.isSuccessful) {
                    throw when (response.code) {
                        in 500..599 -> ServiceUnavailableError(response.code, responseBody)
                        400, 404 -> BadResponseError(response.code, responseBody)
                        else -> UnexpectedResponseError(response.code, responseBody)
                    }
                }

                val gson = Gson()
                val apiResponseType = object : TypeToken<ApiResponse<ConfigData>>() {}.type
                val apiResponse: ApiResponse<ConfigData> = gson.fromJson(responseBody, apiResponseType)
                return@withContext apiResponse.data
            }
        } catch (e: Throwable) {
            throw e.toIDDigitalError("Error in getConfiguration")
        }
    }
}