package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository

class ValidateLivenessChallengeUseCase(
    private val livenessRepository: LivenessRepository,
) {
    suspend operator fun invoke(challengeId: String): Boolean {
        return livenessRepository.validateChallenge(challengeId)
    }
}
