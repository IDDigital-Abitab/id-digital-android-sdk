package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.models.PendingTransaction
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

internal class GetPendingTransactionsUseCase(
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): List<PendingTransaction> = withContext(dispatcher) {
        return@withContext validationSessionRepository.getPendingTransactions()
    }
}
