package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository

class ExecuteLivenessChallengeUseCase(private val livenessRepository: LivenessRepository) {
    suspend operator fun invoke(challengeId: String): String {
        return livenessRepository.executeChallenge(challengeId)
    }
}