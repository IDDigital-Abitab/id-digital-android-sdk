package com.example.iddigital.fcm

import android.content.Intent
import androidx.compose.runtime.mutableStateOf

/**
 * Datos de un push cross-device recibido (en producción, por FCM/APNs propios
 * del Integrador). Ver .docs/sdk/cliente/03-endpoint-push.md /
 * 04-integracion-fcm.md para el contrato de transactionId/type/documentNumber.
 * documentType/documentCountry solo vienen poblados para type="association" -
 * necesarios para construir el Document requerido por associate() (ver
 * PendingVerificationFlow.kt).
 */
data class PushPayload(
    val transactionId: String,
    val type: String,
    val documentNumber: String?,
    val documentType: String?,
    val documentCountry: String?,
)

/** Claves usadas tanto en el data payload de FCM como en los extras del intent de la notificación. */
object PushPayloadKeys {
    const val TRANSACTION_ID = "idDigitalTransactionId"
    const val TYPE = "idDigitalType"
    const val DOCUMENT_NUMBER = "idDigitalDocumentNumber"
    const val DOCUMENT_TYPE = "idDigitalDocumentType"
    const val DOCUMENT_COUNTRY = "idDigitalDocumentCountry"
}

/**
 * Holder simple para pasar el payload recibido desde [IDDigitalSampleFcmService]
 * (o desde el intent de la notificación al tocarla) hacia la UI Compose, mismo
 * patrón que `keycloakRedirect` en MainActivity.kt.
 */
object IncomingPush {
    val current = mutableStateOf<PushPayload?>(null)
}

/** Extrae el [PushPayload] de los extras puestos por la notificación al abrir la app (ver IDDigitalSampleFcmService). */
fun pushPayloadFromIntent(intent: Intent?): PushPayload? {
    val transactionId = intent?.getStringExtra(PushPayloadKeys.TRANSACTION_ID) ?: return null
    val type = intent.getStringExtra(PushPayloadKeys.TYPE) ?: return null
    val documentNumber = intent.getStringExtra(PushPayloadKeys.DOCUMENT_NUMBER)
    val documentType = intent.getStringExtra(PushPayloadKeys.DOCUMENT_TYPE)
    val documentCountry = intent.getStringExtra(PushPayloadKeys.DOCUMENT_COUNTRY)
    return PushPayload(transactionId, type, documentNumber, documentType, documentCountry)
}
