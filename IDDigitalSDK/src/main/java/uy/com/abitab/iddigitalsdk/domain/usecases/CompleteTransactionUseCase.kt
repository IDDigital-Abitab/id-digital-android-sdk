package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.models.CompleteTransactionResult
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

internal class CompleteTransactionUseCase (
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(transactionId: String, validationSessionId: String): CompleteTransactionResult = withContext(dispatcher) {
        return@withContext validationSessionRepository.completeTransaction(transactionId, validationSessionId)
    }
}
