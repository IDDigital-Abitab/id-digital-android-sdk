package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository

class CreateAndLaunchLivenessChallengeUseCase(
    private val launchChallengeActivityUseCase: LaunchChallengeActivityUseCase,
    private val livenessRepository: LivenessRepository
) {
    suspend operator fun invoke(document: Document) {
        val challengeId = livenessRepository.createChallenge(document)
        launchChallengeActivityUseCase(challengeId, ChallengeType.Liveness)
    }
}