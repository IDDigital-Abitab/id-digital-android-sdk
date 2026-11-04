package uy.com.abitab.iddigitalsdk.support

import uy.com.abitab.iddigitalsdk.domain.models.Challenge
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.CompleteTransactionResult
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.models.PendingTransaction
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository
import java.time.Instant

internal fun pinChallenge(id: String = "ch-1") = Challenge(
    id = id,
    type = ChallengeType.Pin.typeName,
    status = "pending",
    expirationDate = "2026-01-01T00:30:00Z",
)

internal fun validationSession(
    id: String = "session-1",
    challenges: List<Challenge> = listOf(pinChallenge()),
) = ValidationSession(
    id = id,
    type = "device_association",
    status = "pending",
    createdAt = "2026-01-01T00:00:00Z",
    expirationDate = "2026-01-01T01:00:00Z",
    challenges = challenges,
    payload = emptyMap(),
)

internal fun deviceAssociation(idToken: String? = "header.payload.sig") = DeviceAssociation(
    token = "device-token",
    document = Document(number = "12345672", type = "ci", country = "UY"),
    createdAt = "2026-01-01T00:00:00Z",
    idToken = idToken,
)

internal class FakeValidationSessionRepository : ValidationSessionRepository {
    var associationSession: ValidationSession = validationSession()
    var completedAssociation: DeviceAssociation = deviceAssociation()
    var createdSession: ValidationSession = validationSession(id = "validation-1")
    var failure: Throwable? = null
    var lastTransactionId: String? = null
    var removed: Boolean = false

    override suspend fun createDeviceAssociation(transactionId: String): ValidationSession {
        lastTransactionId = transactionId
        failure?.let { throw it }
        return associationSession
    }

    override suspend fun completeDeviceAssociation(id: String): DeviceAssociation {
        failure?.let { throw it }
        return completedAssociation
    }

    override suspend fun createValidationSession(type: ChallengeType): ValidationSession {
        failure?.let { throw it }
        return createdSession
    }

    override suspend fun completeTransaction(
        transactionId: String,
        validationSessionId: String,
    ): CompleteTransactionResult = CompleteTransactionResult("https://rp.example/callback")

    override suspend fun getPendingTransactions(): List<PendingTransaction> = emptyList()

    override suspend fun removeAssociation() {
        removed = true
    }
}

internal class FakePinRepository : PinRepository {
    var onValidate: suspend (String, String) -> Boolean = { _, _ -> true }
    var executedChallengeId: String? = null

    override suspend fun createChallenge(document: Document): String = "pin-challenge"

    override suspend fun executeChallenge(challengeId: String): Instant? {
        executedChallengeId = challengeId
        return null
    }

    override suspend fun validateChallenge(challengeId: String, pin: String): Boolean {
        return onValidate(challengeId, pin)
    }
}

internal class FakeLivenessRepository : LivenessRepository {
    var sessionId: String = "liveness-session"
    var valid: Boolean = true

    override suspend fun createChallenge(document: Document): String = "liveness-challenge"

    override suspend fun executeChallenge(challengeId: String): String = sessionId

    override suspend fun validateChallenge(challengeId: String): Boolean = valid
}
