package uy.com.abitab.iddigitalsdk.domain.models

data class ValidationSession(
    val id: String,
    val type: String,
    val status: String,
    val createdAt: String,
    val expirationDate: String,
    val challenges: List<Challenge>,
    val payload: Map<String, Any>
)

data class DeviceAssociation(val token: String, val document: Document, val createdAt: String)

data class CanAssociate(val canAssociate: Boolean)