package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

class CreateValidationSessionUseCase(
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(type: ChallengeType): ValidationSession = withContext(dispatcher) {
        val session = validationSessionRepository.createValidationSession(type)

        return@withContext session
    }
}