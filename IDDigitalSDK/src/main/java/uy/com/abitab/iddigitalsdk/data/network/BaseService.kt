package uy.com.abitab.iddigitalsdk.data.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.component.KoinComponent
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment

abstract class BaseService : KoinComponent {
    val JSON = "application/json; charset=utf-8".toMediaType()
    val EMPTY_REQUEST_BODY = ByteArray(0).toRequestBody()

    private val baseUrl: String
        get() {
            val environmentName: String? = getKoin().getProperty("environment")
            val environment = try {
                IDDigitalSDKEnvironment.valueOf(environmentName ?: IDDigitalSDKEnvironment.PRODUCTION.name)
            } catch (e: IllegalArgumentException) {
                IDDigitalSDKEnvironment.PRODUCTION
            }

            return when (environment) {
                IDDigitalSDKEnvironment.STAGING -> "https://auth.identificaciondigital.com.uy/api/v2/sdk"
                IDDigitalSDKEnvironment.PRODUCTION -> "https://auth.identidaddigital.com.uy/api/v2/sdk"
            }
        }


    protected fun buildUrl(path: String): String {
        return "${baseUrl.trimEnd('/')}/$path"
    }
}