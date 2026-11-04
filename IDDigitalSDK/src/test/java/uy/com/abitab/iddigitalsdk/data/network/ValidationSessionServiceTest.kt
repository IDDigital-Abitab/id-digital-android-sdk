package uy.com.abitab.iddigitalsdk.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepositoryImpl
import uy.com.abitab.iddigitalsdk.support.SdkHttpRule
import uy.com.abitab.iddigitalsdk.utils.BadResponseError
import uy.com.abitab.iddigitalsdk.utils.DeviceNotAssociatedError
import uy.com.abitab.iddigitalsdk.utils.ForbiddenError
import uy.com.abitab.iddigitalsdk.utils.NoInternetConnection
import uy.com.abitab.iddigitalsdk.utils.ServiceUnavailableError
import uy.com.abitab.iddigitalsdk.utils.SessionHasUncompletedChallengesError
import uy.com.abitab.iddigitalsdk.utils.TransactionNotFoundError
import uy.com.abitab.iddigitalsdk.utils.UnexpectedResponseError
import uy.com.abitab.iddigitalsdk.utils.ValidationSessionNotFoundError

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ValidationSessionServiceTest {

    @get:Rule
    val http = SdkHttpRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun service(online: Boolean = true) = ValidationSessionService(
        http.client,
        context,
        isInternetAvailable = { online },
    )

    @Test
    fun createDeviceAssociation_deserializesSessionAndChallenges() {
        // Given a pending association with a PIN challenge
        http.enqueue(200, sessionBody(id = "session-1"))
        val repository = ValidationSessionRepositoryImpl(service())

        // When the SDK starts the association
        val session = runBlocking { repository.createDeviceAssociation("tx-1") }

        // Then the session and its challenge are available to the flow
        assertEquals("session-1", session.id)
        assertEquals("pin", session.challenges.single().type)
        val request = http.server.takeRequest()
        assertEquals("/associations/", request.path)
        assertTrue(request.body.readUtf8().contains("tx-1"))
    }

    @Test
    fun createDeviceAssociation_transactionNotFound() {
        // Given the backend does not know the transaction
        http.enqueue(404, """{"code":"transaction-not-found"}""")

        // When association starts
        val error = assertThrows(TransactionNotFoundError::class.java) {
            runBlocking { service().createDeviceAssociation("missing") }
        }

        // Then the SDK reports that the transaction does not exist
        assertEquals("OIDC transaction not found", error.message)
    }

    @Test
    fun createDeviceAssociation_serverError() {
        // Given the backend fails with 500
        http.enqueue(500, """{"code":"internal-error"}""")

        // When association starts
        val error = assertThrows(ServiceUnavailableError::class.java) {
            runBlocking { service().createDeviceAssociation("tx-1") }
        }

        // Then the SDK reports the service as unavailable
        assertEquals(500, error.statusCode)
    }

    @Test
    fun createDeviceAssociation_withoutNetworkDoesNotCallBackend() {
        // Given the device is offline
        val offline = service(online = false)

        // When association starts
        assertThrows(NoInternetConnection::class.java) {
            runBlocking { offline.createDeviceAssociation("tx-1") }
        }

        // Then no request leaves the device
        assertEquals(0, http.server.requestCount)
    }

    @Test
    fun completeDeviceAssociation_returnsIdToken() {
        // Given the backend issues an idToken for the association
        http.enqueue(200, associationBody(idToken = "header.payload.sig"))
        val repository = ValidationSessionRepositoryImpl(service())

        // When the association is completed
        val association = runBlocking { repository.completeDeviceAssociation("session-1") }

        // Then the SDK returns the token and the citizen document
        assertEquals("header.payload.sig", association.idToken)
        assertEquals("12345672", association.document.number)
        assertEquals("ci", association.document.type)
        assertEquals("/associations/session-1/", http.server.takeRequest().path)
    }

    @Test
    fun completeDeviceAssociation_allowsNullIdToken() {
        // Given the client has no active secret, so the backend omits the OIDC token
        http.enqueue(200, associationBody(idToken = null))

        // When the association is completed
        val association = runBlocking { service().completeDeviceAssociation("session-1") }

        // Then idToken stays null for the caller to treat as absent
        assertNull(association.idToken)
    }

    @Test
    fun completeDeviceAssociation_badRequest() {
        // Given the backend rejects the association id
        http.enqueue(400, """{"code":"form-errors"}""")

        // When completion is requested
        val error = assertThrows(BadResponseError::class.java) {
            runBlocking { service().completeDeviceAssociation("session-1") }
        }

        // Then the SDK reports an invalid response
        assertEquals(400, error.statusCode)
    }

    @Test
    fun createValidationSession_deserializesRequestedChallenge() {
        // Given an associated device and a PIN validation session
        http.enqueue(200, sessionBody(id = "validation-9", type = "validation"))
        val repository = ValidationSessionRepositoryImpl(service())

        // When authentication starts
        val session = runBlocking { repository.createValidationSession(ChallengeType.Pin) }

        // Then the session id is the one the app will pass to completeTransaction
        assertEquals("validation-9", session.id)
        val request = http.server.takeRequest()
        assertEquals("/validations/", request.path)
        assertTrue(request.body.readUtf8().contains("pin"))
    }

    @Test
    fun createValidationSession_invalidDeviceAssociationToken() {
        // Given the local association token is no longer accepted
        http.enqueue(401, """{"code":"invalid-device-association-token"}""")

        // When authentication starts
        val error = assertThrows(DeviceNotAssociatedError::class.java) {
            runBlocking { service().createValidationSession(ChallengeType.Pin) }
        }

        // Then the SDK tells the app to associate again
        assertEquals("Device is not associated", error.message)
    }

    @Test
    fun createValidationSession_unexpectedStatus() {
        // Given a status the SDK does not map to a specific error
        http.enqueue(401, """{"code":"something-else"}""")

        // When authentication starts
        val error = assertThrows(UnexpectedResponseError::class.java) {
            runBlocking { service().createValidationSession(ChallengeType.Liveness) }
        }

        // Then the SDK keeps the HTTP status for diagnosis
        assertEquals(401, error.statusCode)
    }

    @Test
    fun completeTransaction_returnsFinishUrl() {
        // Given a validated session that can close the OIDC transaction
        http.enqueue(200, """{"data":{"finishUrl":"https://rp.example/callback"},"code":"success"}""")
        val repository = ValidationSessionRepositoryImpl(service())

        // When the app completes the transaction
        val result = runBlocking { repository.completeTransaction("tx-1", "validation-9") }

        // Then the SDK returns the URL the integrator may open
        assertEquals("https://rp.example/callback", result.finishUrl)
        val body = http.server.takeRequest().body.readUtf8()
        assertTrue(body.contains("tx-1"))
        assertTrue(body.contains("validation-9"))
    }

    @Test
    fun completeTransaction_nullFinishUrlIsSuccess() {
        // Given the backend authorized the transaction but has no finish URL
        http.enqueue(200, """{"data":{"finishUrl":null},"code":"success"}""")

        // When the app completes the transaction
        val result = runBlocking { service().completeTransaction("tx-1", "validation-9") }

        // Then a null URL is success: the browser finishes by polling
        assertNull(result.finishUrl)
    }

    @Test
    fun completeTransaction_malformedSuccessBodyReturnsNullFinishUrl() {
        // Given HTTP 200 with a body Gson cannot read
        http.enqueue(200, "not-json")

        // When the app completes the transaction
        val result = runBlocking { service().completeTransaction("tx-1", "validation-9") }

        // Then the SDK still reports success and leaves the URL empty
        assertNull(result.finishUrl)
    }

    @Test
    fun completeTransaction_mapsBusinessErrors() {
        assertCompleteTransactionError("transaction-not-found", TransactionNotFoundError::class.java)
        assertCompleteTransactionError("validation-session-not-found", ValidationSessionNotFoundError::class.java)
        assertCompleteTransactionError(
            "session-has-uncompleted-challenges",
            SessionHasUncompletedChallengesError::class.java,
        )
        assertCompleteTransactionError("forbidden", ForbiddenError::class.java)
    }

    @Test
    fun getPendingTransactions_deserializesIds() {
        // Given two logins waiting for this device
        http.enqueue(
            200,
            """{"data":{"transactions":[{"id":"tx-1"},{"id":"tx-2"}]},"code":"success"}""",
        )
        val repository = ValidationSessionRepositoryImpl(service())

        // When the SDK polls
        val pending = runBlocking { repository.getPendingTransactions() }

        // Then both ids are returned in order
        assertEquals(listOf("tx-1", "tx-2"), pending.map { it.id })
    }

    @Test
    fun getPendingTransactions_unauthorized() {
        // Given the bearer token is rejected
        http.enqueue(401, """{"code":"unauthorized"}""")

        // When the SDK polls
        val error = assertThrows(BadResponseError::class.java) {
            runBlocking { service().getPendingTransactions() }
        }

        // Then the SDK reports an invalid response
        assertEquals(401, error.statusCode)
    }

    @Test
    fun removeAssociation_acceptsEmptySuccessBody() {
        // Given the backend deletes the association
        http.enqueue(204)
        val repository = ValidationSessionRepositoryImpl(service())

        // When the SDK removes it
        runBlocking { repository.removeAssociation() }

        // Then the call is a DELETE against the association resource
        val request = http.server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/associations/", request.path)
    }

    private fun assertCompleteTransactionError(code: String, type: Class<out Throwable>) {
        http.enqueue(400, """{"code":"$code"}""")
        assertThrows(type) {
            runBlocking { service().completeTransaction("tx-1", "validation-9") }
        }
    }

    private fun sessionBody(id: String, type: String = "device_association") = """
        {
          "data": {
            "id": "$id",
            "type": "$type",
            "status": "pending",
            "createdAt": "2026-01-01T00:00:00Z",
            "expirationDate": "2026-01-01T01:00:00Z",
            "challenges": [
              {
                "id": "ch-1",
                "type": "pin",
                "status": "pending",
                "expirationDate": "2026-01-01T00:30:00Z"
              }
            ],
            "payload": {}
          },
          "code": "success"
        }
    """.trimIndent()

    private fun associationBody(idToken: String?) = """
        {
          "data": {
            "token": "device-token",
            "idToken": ${if (idToken == null) "null" else "\"$idToken\""},
            "document": {"number": "12345672", "type": "ci", "country": "UY"},
            "createdAt": "2026-01-01T00:00:00Z"
          },
          "code": "success"
        }
    """.trimIndent()
}
