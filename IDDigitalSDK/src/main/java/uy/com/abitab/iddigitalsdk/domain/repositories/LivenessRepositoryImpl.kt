package uy.com.abitab.iddigitalsdk.data.repositories

import uy.com.abitab.iddigitalsdk.data.network.LivenessService
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository

class LivenessRepositoryImpl(private val livenessService: LivenessService) : LivenessRepository {
    override suspend fun createChallenge(document: Document): String {
        return livenessService.createChallenge(document)
    }

    override suspend fun executeChallenge(challengeId: String): String {
        return livenessService.executeChallenge(challengeId)
    }

    override suspend fun validateChallenge(challengeId: String) {
        return livenessService.validateChallenge(challengeId)
    }
}