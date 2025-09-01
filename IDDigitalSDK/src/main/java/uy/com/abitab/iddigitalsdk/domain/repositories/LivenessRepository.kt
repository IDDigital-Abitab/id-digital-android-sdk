package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.domain.models.Document

interface LivenessRepository {
    suspend fun createChallenge(document: Document): String
    suspend fun executeChallenge(challengeId: String): String
    suspend fun validateChallenge(challengeId: String): Boolean
}