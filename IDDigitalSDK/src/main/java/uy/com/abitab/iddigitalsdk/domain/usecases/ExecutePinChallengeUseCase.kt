package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository

class ExecutePinChallengeUseCase(private val pinRepository: PinRepository) {
    suspend operator fun invoke(challengeId: String) {
        return pinRepository.executeChallenge(challengeId)
    }
}