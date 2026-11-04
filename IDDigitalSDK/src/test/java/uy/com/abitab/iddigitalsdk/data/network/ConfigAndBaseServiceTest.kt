package uy.com.abitab.iddigitalsdk.data.network

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import uy.com.abitab.iddigitalsdk.domain.models.IDDigitalSDKEnvironment
import uy.com.abitab.iddigitalsdk.support.SdkHttpRule
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError

class ConfigServiceTest {

    @get:Rule
    val http = SdkHttpRule()

    @Test
    fun getConfiguration_deserializesCognitoSettings() = runBlocking {
        // Given the initialize payload from the backend
        http.enqueue(
            200,
            """
            {
              "data": {
                "cognitoAppClientId": "app",
                "cognitoUserPoolId": "pool",
                "cognitoIdentityPoolId": "identity",
                "region": "us-east-1"
              },
              "code": "success"
            }
            """.trimIndent(),
        )

        // When the SDK loads its configuration
        val config = ConfigService(http.client).getConfiguration()

        // Then the Cognito ids are available to Amplify setup
        assertEquals("app", config.cognitoAppClientId)
        assertEquals("pool", config.cognitoUserPoolId)
        assertEquals("identity", config.cognitoIdentityPoolId)
        assertEquals("us-east-1", config.region)
        assertEquals("/initialize/", http.server.takeRequest().path)
    }

    @Test
    fun getConfiguration_serverError() {
        // Given initialize is unavailable
        http.enqueue(503, """{"code":"unavailable"}""")

        // When the SDK loads its configuration
        val error = assertThrows(ServiceUnavailableError::class.java) {
            runBlocking { ConfigService(http.client).getConfiguration() }
        }

        // Then the HTTP status is preserved
        assertEquals(503, error.statusCode)
    }

    @Test
    fun getConfiguration_badRequest() {
        // Given the backend rejects the API key payload shape
        http.enqueue(400, """{"code":"form-errors"}""")

        // When the SDK loads its configuration
        val error = assertThrows(BadResponseError::class.java) {
            runBlocking { ConfigService(http.client).getConfiguration() }
        }

        // Then the SDK reports an invalid response
        assertEquals(400, error.statusCode)
    }
}

class BaseServiceTest {

    @After
    fun stopKoinIfStarted() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
    }

    @Test
    fun stagingEnvironment_usesStagingHost() {
        // Given the SDK was initialized for staging
        startKoin { properties(mapOf("environment" to IDDigitalSDKEnvironment.STAGING.name)) }

        // When a service builds a URL
        val url = UrlProbe().url("associations/")

        // Then it targets the staging API
        assertEquals(
            "https://auth.identificaciondigital.com.uy/api/v2/sdk/associations/",
            url,
        )
    }

    @Test
    fun missingEnvironment_usesProductionHost() {
        // Given initialize did not set an environment
        startKoin { properties(emptyMap()) }

        // When a service builds a URL
        val url = UrlProbe().url("validations/")

        // Then it targets production
        assertEquals(
            "https://auth.identidaddigital.com.uy/api/v2/sdk/validations/",
            url,
        )
    }

    @Test
    fun unknownEnvironment_usesProductionHost() {
        // Given a property that is not STAGING or PRODUCTION
        startKoin { properties(mapOf("environment" to "LOCAL")) }

        // When a service builds a URL
        val url = UrlProbe().url("associations/")

        // Then it falls back to production
        assertEquals(
            "https://auth.identidaddigital.com.uy/api/v2/sdk/associations/",
            url,
        )
    }

    @Test
    fun customBaseUrl_overridesEnvironment() {
        // Given an internal base URL for a development backend
        startKoin {
            properties(
                mapOf(
                    "environment" to IDDigitalSDKEnvironment.PRODUCTION.name,
                    "customBaseUrl" to "http://10.0.2.2:8000/api/v2/sdk/",
                )
            )
        }

        // When a service builds a URL
        val url = UrlProbe().url("associations/")

        // Then the custom host is used and the trailing slash is not duplicated
        assertEquals("http://10.0.2.2:8000/api/v2/sdk/associations/", url)
    }

    @Test
    fun blankCustomBaseUrl_keepsEnvironmentHost() {
        // Given a blank override
        startKoin {
            properties(
                mapOf(
                    "environment" to IDDigitalSDKEnvironment.STAGING.name,
                    "customBaseUrl" to "  ",
                )
            )
        }

        // When a service builds a URL
        val url = UrlProbe().url("associations/")

        // Then staging stays in place
        assertEquals(
            "https://auth.identificaciondigital.com.uy/api/v2/sdk/associations/",
            url,
        )
    }

    private class UrlProbe : BaseService() {
        fun url(path: String): String = buildUrl(path)
    }
}
