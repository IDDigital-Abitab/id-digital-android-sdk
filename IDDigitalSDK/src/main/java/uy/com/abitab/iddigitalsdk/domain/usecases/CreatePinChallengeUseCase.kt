package uy.com.abitab.iddigitalsdk.domain.usecases

import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository

class CreatePinChallengeUseCase(private val pinRepository: PinRepository) {
    suspend operator fun invoke(document: Document): String {
        return pinRepository.createChallenge(document)
    }
}