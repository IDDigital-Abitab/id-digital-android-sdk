package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.data.network.PinService
import uy.com.abitab.iddigitalsdk.domain.models.Document
import java.time.Instant

class PinRepositoryImpl(private val pinService: PinService) : PinRepository {
    override suspend fun createChallenge(document: Document): String {
        return pinService.createChallenge(document)
    }

    override suspend fun executeChallenge(challengeId: String): Instant? {
        return pinService.executeChallenge(challengeId)
    }

    override suspend fun validateChallenge(challengeId: String, pin: String): Boolean {
        return pinService.validateChallenge(challengeId, pin)
    }
}