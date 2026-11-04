package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepositoryImpl
import uy.com.abitab.iddigitalsdk.support.SdkHttpRule
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class PinAndLivenessServiceTest {

    @get:Rule
    val http = SdkHttpRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val document = Document(number = "12345672", type = "ci", country = "UY")

    private fun pinService(online: Boolean = true) = PinService(
        http.client,
        context,
        isInternetAvailable = { online },
    )

    private fun livenessService(online: Boolean = true) = LivenessService(
        http.client,
        context,
        isInternetAvailable = { online },
    )

    @Test
    fun validatePin_success() {
        // Given the backend accepts the PIN
        http.enqueue(200, """{"data":{},"code":"success"}""")
        val repository = PinRepositoryImpl(pinService())

        // When the citizen submits it
        val valid = runBlocking { repository.validateChallenge("ch-1", "1234") }

        // Then the challenge is valid
        assertTrue(valid)
        assertTrue(http.server.takeRequest().body.readUtf8().contains("1234"))
    }

    @Test
    fun validatePin_invalidPinDoesNotSucceed() {
        // Given the backend rejects the PIN
        http.enqueue(401, """{"code":"invalid-pin"}""")

        // When the citizen submits it
        val error = assertThrows(IDDigitalError::class.java) {
            runBlocking { pinService().validateChallenge("ch-1", "0000") }
        }

        // Then the SDK does not treat the challenge as passed
        assertTrue(error.message.orEmpty().isNotBlank())
    }

    @Test
    fun validatePin_tooManyAttemptsDoesNotSucceed() {
        // Given the citizen is locked out
        http.enqueue(422, """{"code":"too-many-attempts"}""")

        // When another PIN is submitted
        val error = assertThrows(IDDigitalError::class.java) {
            runBlocking { pinService().validateChallenge("ch-1", "0000") }
        }

        // Then the SDK does not treat the challenge as passed
        assertTrue(error.message.orEmpty().isNotBlank())
    }

    @Test
    fun createAndExecutePinChallenge() {
        // Given the backend creates a PIN challenge and reports when the PIN last changed
        http.enqueue(200, """{"data":{"challengeId":"ch-9"},"code":"success"}""")
        http.enqueue(200, """{"data":{"pinLastUpdated":"2026-01-02T03:04:05Z"},"code":"success"}""")
        val repository = PinRepositoryImpl(pinService())

        // When the SDK prepares the challenge
        val challengeId = runBlocking { repository.createChallenge(document) }
        val updated = runBlocking { repository.executeChallenge(challengeId) }

        // Then both calls return the backend values
        assertEquals("ch-9", challengeId)
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), updated)
    }

    @Test
    fun executePinChallenge_withoutLastUpdate() {
        // Given the backend has no pinLastUpdated
        http.enqueue(200, """{"data":{},"code":"success"}""")

        // When the challenge is executed
        val updated = runBlocking { pinService().executeChallenge("ch-1") }

        // Then there is no timestamp to compare
        assertNull(updated)
    }

    @Test
    fun validatePin_withoutNetwork() {
        // Given the device is offline
        val offline = pinService(online = false)

        // When a PIN is submitted
        assertThrows(NoInternetConnection::class.java) {
            runBlocking { offline.validateChallenge("ch-1", "1234") }
        }

        // Then the SDK does not call the backend
        assertEquals(0, http.server.requestCount)
    }

    @Test
    fun livenessChallenge_roundTrip() {
        // Given liveness can be created, executed and accepted
        http.enqueue(200, """{"data":{"challengeId":"live-1"},"code":"success"}""")
        http.enqueue(200, """{"data":{"sessionId":"aws-session"},"code":"success"}""")
        http.enqueue(200, """{"data":{},"code":"success"}""")
        val repository = LivenessRepositoryImpl(livenessService())

        // When the SDK runs the three steps
        val challengeId = runBlocking { repository.createChallenge(document) }
        val sessionId = runBlocking { repository.executeChallenge(challengeId) }
        val valid = runBlocking { repository.validateChallenge(challengeId) }

        // Then each step returns the backend value
        assertEquals("live-1", challengeId)
        assertEquals("aws-session", sessionId)
        assertTrue(valid)
    }

    @Test
    fun validateLiveness_rejectedChallengeReturnsFalse() {
        // Given the face did not match
        http.enqueue(422, """{"code":"challenge-failed"}""")

        // When liveness is validated
        val valid = runBlocking { livenessService().validateChallenge("live-1") }

        // Then the challenge failed and can be retried
        assertFalse(valid)
    }
}
