package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

class ExecuteChallengeUseCase(
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
//    suspend operator fun invoke(challengeId: String, data: Record): Unit =
//        withContext(dispatcher) {
//            return@withContext validationSessionRepository.executeChallenge(challengeId, data)
//        }
}









