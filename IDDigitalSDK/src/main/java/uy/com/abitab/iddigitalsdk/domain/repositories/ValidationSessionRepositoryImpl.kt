package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.data.network.ValidationSessionService
import uy.com.abitab.iddigitalsdk.domain.models.Document

class ValidationSessionRepositoryImpl(private val validationSessionService: ValidationSessionService) :
    ValidationSessionRepository {

    override suspend fun createDeviceAssociation(document: Document) {
        return validationSessionService.createDeviceAssociation(document)
    }
}