package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession

interface ValidationSessionRepository {
    suspend fun checkCanAssociate(document: Document): Boolean
    suspend fun createDeviceAssociation(document: Document): ValidationSession
    suspend fun completeDeviceAssociation(id: String): DeviceAssociation
    suspend fun createValidationSession(type: ChallengeType): ValidationSession
    suspend fun removeAssociation(): Unit
}