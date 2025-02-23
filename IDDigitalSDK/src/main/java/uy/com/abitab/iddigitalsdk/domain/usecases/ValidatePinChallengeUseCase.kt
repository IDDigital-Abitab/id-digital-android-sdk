package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository

class ValidatePinChallengeUseCase(private val pinRepository: PinRepository) {
    suspend operator fun invoke(challengeId: String, pin: String): Boolean {
        return pinRepository.validateChallenge(challengeId, pin)
    }
}