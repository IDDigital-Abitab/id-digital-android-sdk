package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository
import java.time.Instant

class ExecutePinChallengeUseCase(private val pinRepository: PinRepository) {
    suspend operator fun invoke(challengeId: String): Instant? {
        return pinRepository.executeChallenge(challengeId)
    }
}