package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

class RemoveAssociationUseCase (
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): Unit = withContext(dispatcher) {
        return@withContext validationSessionRepository.removeAssociation()
    }
}