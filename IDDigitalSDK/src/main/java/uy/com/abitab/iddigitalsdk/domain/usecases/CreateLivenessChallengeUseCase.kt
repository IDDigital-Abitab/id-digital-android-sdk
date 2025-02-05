package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository

class CreateLivenessChallengeUseCase(private val livenessRepository: LivenessRepository) {
    suspend operator fun invoke(document: Document): String {
        return livenessRepository.createChallenge(document)
    }
}