package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.domain.models.Document
import java.time.Instant

interface PinRepository {
    suspend fun createChallenge(document: Document): String
    suspend fun executeChallenge(challengeId: String): Instant?
    suspend fun validateChallenge(challengeId: String, pin: String): Boolean
}