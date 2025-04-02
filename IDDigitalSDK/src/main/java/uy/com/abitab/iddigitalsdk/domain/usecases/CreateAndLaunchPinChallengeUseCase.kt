package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository

class CreateAndLaunchPinChallengeUseCase(
    private val launchChallengeActivityUseCase: LaunchChallengeActivityUseCase,
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(document: Document) {
        val challengeId = pinRepository.createChallenge(document)
        launchChallengeActivityUseCase(challengeId, ChallengeType.Pin)
    }
}
