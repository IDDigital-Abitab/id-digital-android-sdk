package uy.com.abitab.iddigitalsdk.domain.repositories

import uy.com.abitab.iddigitalsdk.domain.models.Document

interface ValidationSessionRepository {
    suspend fun createDeviceAssociation(document: Document): Unit
}