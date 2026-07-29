package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.CompleteTransactionResult
import uy.com.abitab.iddigitalsdk.domain.models.PendingTransaction
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession

interface ValidationSessionRepository {
    suspend fun createDeviceAssociation(transactionId: String): ValidationSession
    suspend fun completeDeviceAssociation(id: String): DeviceAssociation
    suspend fun createValidationSession(type: ChallengeType): ValidationSession
    suspend fun completeTransaction(transactionId: String, validationSessionId: String): CompleteTransactionResult
    suspend fun getPendingTransactions(): List<PendingTransaction>
    suspend fun removeAssociation(): Unit
//    suspend fun executeChallenge(challengeId: String, data: Record): Unit
//    suspend fun validateChallenge(challengeId: String, data: Record): Boolean
}