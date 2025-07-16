package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository

class ValidatePinChallengeUseCase(
    private val pinRepository: PinRepository, private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(challengeId: String, pin: String): Boolean =
        withContext(dispatcher) {
            val isValid = pinRepository.validateChallenge(challengeId, pin)
            return@withContext isValid
        }
}