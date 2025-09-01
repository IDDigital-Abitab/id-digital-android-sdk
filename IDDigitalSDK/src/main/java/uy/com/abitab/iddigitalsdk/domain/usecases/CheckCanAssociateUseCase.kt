package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

class CheckCanAssociateUseCase (
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(document: Document): Boolean = withContext(dispatcher) {
        return@withContext validationSessionRepository.checkCanAssociate(document)
    }
}