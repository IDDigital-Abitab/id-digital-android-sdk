package com.example.iddigital.deeplink

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import uy.com.abitab.iddigitalsdk.IDDigitalSDK

/**
 * Deep link same-device del puente web (ver .docs/sdk/cliente/01-arquitectura-y-flujos.md).
 * A diferencia del push, la app decide localmente si asociar o validar (ver
 * LaunchedEffect en PendingVerificationFlow.kt). transactionId es el único dato que
 * viaja como query param: el backend resuelve al ciudadano desde la transacción (via
 * resolve_transaction_pk), así que no hace falta ningún dato de documento acá.
 */
data class DeepLinkPayload(
    val transactionId: String,
)

/** Holder simple para pasar el payload hacia la UI Compose, mismo patrón que IncomingPush. */
object IncomingDeepLink {
    val current = mutableStateOf<DeepLinkPayload?>(null)
}

/** Usa el helper de la SDK para extraer transactionId; null si el intent no es este deep link. */
fun deepLinkPayloadFromIntent(intent: Intent?): DeepLinkPayload? {
    val uri = intent?.data ?: return null
    val transactionId = IDDigitalSDK.parseAuthenticationLink(uri) ?: return null
    return DeepLinkPayload(transactionId)
}
