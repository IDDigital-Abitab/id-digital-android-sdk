package uy.com.abitab.iddigitalsdk.domain.models

/**
 * Representación interna de una sesión de validación.
 *
 * @suppress
 */
data class ValidationSession(
    val id: String,
    val type: String,
    val status: String,
    val createdAt: String,
    val expirationDate: String,
    val challenges: List<Challenge>,
    val payload: Map<String, Any>
)

/**
 * Asociación almacenada para el dispositivo actual.
 *
 * @property token credencial privada utilizada por la SDK para autenticar al dispositivo.
 * La aplicación no debe transmitirla ni persistir copias adicionales.
 * @property document documento del ciudadano asociado.
 * @property createdAt fecha de creación informada por el backend.
 * @property idToken token OIDC disponible cuando la integración tiene un secreto activo, o
 * `null` cuando no corresponde emitirlo.
 */
data class DeviceAssociation(
    val token: String,
    val document: Document,
    val createdAt: String,
    val idToken: String?,
)

/**
 * Resultado interno del cierre de una transacción.
 *
 * @suppress
 */
data class CompleteTransactionResult(val finishUrl: String?)

/**
 * Representación interna de una transacción pendiente detectada por polling.
 *
 * @suppress
 */
data class PendingTransaction(val id: String)

internal data class PendingTransactionsData(val transactions: List<PendingTransaction>)