package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.data.network.ValidationSessionService
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.models.Record
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession

class ValidationSessionRepositoryImpl(private val validationSessionService: ValidationSessionService) :
    ValidationSessionRepository {

    override suspend fun checkCanAssociate(document: Document): Boolean {
        return validationSessionService.checkCanAssociate(document)
    }

    override suspend fun createDeviceAssociation(document: Document): ValidationSession {
        return validationSessionService.createDeviceAssociation(document)
    }

    override suspend fun completeDeviceAssociation(id: String): DeviceAssociation {
        return validationSessionService.completeDeviceAssociation(id)
    }

    override suspend fun createValidationSession(type: ChallengeType): ValidationSession {
        return validationSessionService.createValidationSession(type)
    }

    override suspend fun removeAssociation(): Unit {
        return validationSessionService.removeAssociation()
    }

//    override suspend fun executeChallenge(challengeId: String, data: Record): Unit {
//        return validationSessionService.executeChallenge(challengeId, data)
//    }
//
//    override suspend fun validateChallenge(challengeId: String, data: Record): Boolean {
//        return validationSessionService.validateChallenge(challengeId, data)
//    }
}