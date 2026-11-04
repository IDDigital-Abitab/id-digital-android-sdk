package uy.com.abitab.iddigitalsdk.domain

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.CompleteTransactionResult
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteTransactionUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateValidationSessionUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.GetPendingTransactionsUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.RemoveAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.support.FakeLivenessRepository
import uy.com.abitab.iddigitalsdk.support.FakePinRepository
import uy.com.abitab.iddigitalsdk.support.FakeValidationSessionRepository
import uy.com.abitab.iddigitalsdk.support.deviceAssociation
import uy.com.abitab.iddigitalsdk.support.validationSession
import uy.com.abitab.iddigitalsdk.utils.ApiResponse

class SdkModelSerializationTest {

    private val gson = Gson()

    @Test
    fun deviceAssociation_roundTripKeepsIdToken() {
        // Given an association the backend issued with an idToken
        val original = deviceAssociation(idToken = "header.payload.sig")

        // When the SDK serializes and reads it back
        val restored = gson.fromJson(gson.toJson(original), DeviceAssociation::class.java)

        // Then document and idToken survive
        assertEquals(original, restored)
    }

    @Test
    fun deviceAssociation_nullIdTokenFromEnvelope() {
        // Given the success envelope without an OIDC token
        val json = """
            {
              "data": {
                "token": "device-token",
                "idToken": null,
                "document": {"number": "12345672", "type": "ci", "country": "UY"},
                "createdAt": "2026-01-01T00:00:00Z"
              }
            }
        """.trimIndent()
        val type = object : TypeToken<ApiResponse<DeviceAssociation>>() {}.type

        // When Gson reads the envelope
        val response = gson.fromJson<ApiResponse<DeviceAssociation>>(json, type)

        // Then idToken is absent and the document is present
        assertNull(response.data.idToken)
        assertEquals("12345672", response.data.document.number)
    }

    @Test
    fun validationSession_readsChallengesFromEnvelope() {
        // Given a session envelope with one PIN challenge
        val json = """
            {
              "data": {
                "id": "session-1",
                "type": "validation",
                "status": "pending",
                "createdAt": "2026-01-01T00:00:00Z",
                "expirationDate": "2026-01-01T01:00:00Z",
                "challenges": [
                  {
                    "id": "ch-1",
                    "type": "liveness",
                    "status": "pending",
                    "expirationDate": "2026-01-01T00:30:00Z"
                  }
                ],
                "payload": {"latestPinUpdate": "2026-01-01T00:00:00Z"}
              }
            }
        """.trimIndent()
        val type = object : TypeToken<ApiResponse<ValidationSession>>() {}.type

        // When Gson reads the envelope
        val session = gson.fromJson<ApiResponse<ValidationSession>>(json, type).data

        // Then the challenge type and payload are kept
        assertEquals("session-1", session.id)
        assertEquals("liveness", session.challenges.single().type)
        assertEquals("2026-01-01T00:00:00Z", session.payload["latestPinUpdate"])
    }

    @Test
    fun completeTransactionResult_readsNullableFinishUrl() {
        // Given a completion envelope whose finish URL is null
        val json = """{"data":{"finishUrl":null}}"""
        val type = object : TypeToken<ApiResponse<CompleteTransactionResult>>() {}.type

        // When Gson reads it
        val result = gson.fromJson<ApiResponse<CompleteTransactionResult>>(json, type).data

        // Then the URL stays null
        assertNull(result.finishUrl)
    }

    @Test
    fun challengeType_parsesContractNames() {
        // Given the names the backend sends
        // When the SDK maps them
        // Then pin and liveness match, and unknown names do not
        assertEquals(ChallengeType.Pin, ChallengeType.fromString("PIN"))
        assertEquals(ChallengeType.Liveness, ChallengeType.fromString("liveness"))
        assertNull(ChallengeType.fromString("sms"))
        assertEquals("pin", ChallengeType.Pin.toString())
        assertEquals("liveness", ChallengeType.Liveness.toString())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SdkUseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val sessions = FakeValidationSessionRepository()
    private val pins = FakePinRepository()
    private val liveness = FakeLivenessRepository()

    @Test
    fun createDeviceAssociation_returnsRepositorySession() = runBlocking {
        // Given a session ready for the transaction
        sessions.associationSession = validationSession(id = "session-7")
        val useCase = CreateDeviceAssociationUseCase(sessions, dispatcher)

        // When association is created
        val session = useCase("tx-7")

        // Then the use case returns that session and forwards the transaction id
        assertEquals("session-7", session.id)
        assertEquals("tx-7", sessions.lastTransactionId)
    }

    @Test
    fun completeDeviceAssociation_returnsIdToken() = runBlocking {
        // Given completion will issue an idToken
        sessions.completedAssociation = deviceAssociation(idToken = "jwt")
        val useCase = CompleteDeviceAssociationUseCase(sessions, dispatcher)

        // When the association is completed
        val association = useCase("session-7")

        // Then the idToken is the one the flow returns to the app
        assertEquals("jwt", association.idToken)
    }

    @Test
    fun createValidationSession_returnsSessionForChallengeType() = runBlocking {
        // Given a validation session for an associated device
        sessions.createdSession = validationSession(id = "validation-3")
        val useCase = CreateValidationSessionUseCase(sessions, dispatcher)

        // When authentication starts
        val session = useCase(ChallengeType.Pin)

        // Then the session id is ready for completeTransaction
        assertEquals("validation-3", session.id)
    }

    @Test
    fun completeTransaction_returnsFinishUrl() = runBlocking {
        // Given the repository can close the OIDC transaction
        val useCase = CompleteTransactionUseCase(sessions, dispatcher)

        // When the app completes it
        val result = useCase("tx-1", "validation-3")

        // Then the finish URL is forwarded
        assertEquals("https://rp.example/callback", result.finishUrl)
    }

    @Test
    fun removeAssociation_delegatesToRepository() = runBlocking {
        // Given an association stored remotely
        val useCase = RemoveAssociationUseCase(sessions, dispatcher)

        // When the app removes it
        useCase()

        // Then the repository performed the removal
        assertEquals(true, sessions.removed)
    }

    @Test
    fun getPendingTransactions_delegatesToRepository() = runBlocking {
        // Given no pending logins
        val useCase = GetPendingTransactionsUseCase(sessions, dispatcher)

        // When the poller asks
        val pending = useCase()

        // Then the empty list is forwarded
        assertEquals(emptyList<Any>(), pending)
    }

    @Test
    fun pinUseCases_delegateToRepository() = runBlocking {
        // Given a PIN challenge
        val execute = ExecutePinChallengeUseCase(pins)
        val validate = ValidatePinChallengeUseCase(pins, dispatcher)

        // When it is executed and validated
        val updated = execute("ch-1")
        val valid = validate("ch-1", "1234")

        // Then the repository saw the challenge id and accepted the PIN
        assertNull(updated)
        assertEquals(true, valid)
        assertEquals("ch-1", pins.executedChallengeId)
    }

    @Test
    fun livenessUseCases_delegateToRepository() = runBlocking {
        // Given a liveness challenge that will pass
        liveness.sessionId = "aws-session"
        liveness.valid = true
        val execute = ExecuteLivenessChallengeUseCase(liveness)
        val validate = ValidateLivenessChallengeUseCase(liveness)

        // When it is executed and validated
        val sessionId = execute("live-1")
        val valid = validate("live-1")

        // Then both results come from the repository
        assertEquals("aws-session", sessionId)
        assertEquals(true, valid)
    }
}
