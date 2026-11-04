package uy.com.abitab.iddigitalsdk.support

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Local HTTP server plus the Koin properties [BaseService] reads for its base URL.
 * Production keeps using [uy.com.abitab.iddigitalsdk.utils.NetworkUtils].
 */
class SdkHttpRule : ExternalResource() {
    lateinit var server: MockWebServer
        private set
    lateinit var client: OkHttpClient
        private set

    override fun before() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        server = MockWebServer()
        server.start()
        startKoin {
            properties(mapOf("customBaseUrl" to server.url("/").toString().trimEnd('/')))
        }
        client = OkHttpClient()
    }

    override fun after() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        if (::server.isInitialized) {
            server.shutdown()
        }
    }

    fun enqueue(code: Int, body: String = "") {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
        )
    }
}
