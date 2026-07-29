package com.example.iddigital.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.iddigital.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val NOTIFICATION_CHANNEL_ID = "id_digital_sample_pending_verification"
private const val NOTIFICATION_ID = 1

/**
 * Recibe el push cross-device que en producción enviaría el backend del
 * Integrador con su propia infraestructura FCM (ver
 * .docs/sdk/cliente/04-integracion-fcm.md). En este workspace, quien lo envía
 * es el mock BQM (sdk/firebase_mock_bqm.py en id-2.0-backend), simulando ese
 * mismo rol con un proyecto Firebase propio de esta app de ejemplo.
 *
 * Solo muestra una notificación local con los datos del push como extras
 * (ver [showPendingVerificationNotification]) — no actualiza [IncomingPush]
 * directamente. Ese método puede ejecutarse con la app en segundo plano, y
 * la SDK necesita abrir una Activity propia (challenge PIN/Liveness) al
 * resolver la transacción; si se dispara ese flujo mientras la app no tiene
 * ninguna Activity visible, Android bloquea el `startActivity()` interno
 * (restricción de Background Activity Launch). Delegar el dato únicamente al
 * `PendingIntent` de la notificación garantiza que [IncomingPush] solo se
 * actualiza (ver MainActivity.handleIntent) cuando el usuario efectivamente
 * trae la app al frente tocando la notificación.
 */
class IDDigitalSampleFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val transactionId = remoteMessage.data[PushPayloadKeys.TRANSACTION_ID] ?: return
        val type = remoteMessage.data[PushPayloadKeys.TYPE] ?: return
        val documentNumber = remoteMessage.data[PushPayloadKeys.DOCUMENT_NUMBER]
        val documentType = remoteMessage.data[PushPayloadKeys.DOCUMENT_TYPE]
        val documentCountry = remoteMessage.data[PushPayloadKeys.DOCUMENT_COUNTRY]

        val payload = PushPayload(transactionId, type, documentNumber, documentType, documentCountry)
        showPendingVerificationNotification(payload)
    }

    private fun showPendingVerificationNotification(payload: PushPayload) {
        createNotificationChannelIfNeeded()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PushPayloadKeys.TRANSACTION_ID, payload.transactionId)
            putExtra(PushPayloadKeys.TYPE, payload.type)
            putExtra(PushPayloadKeys.DOCUMENT_NUMBER, payload.documentNumber)
            putExtra(PushPayloadKeys.DOCUMENT_TYPE, payload.documentType)
            putExtra(PushPayloadKeys.DOCUMENT_COUNTRY, payload.documentCountry)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = if (payload.type == "association") {
            "Asociá tu identidad digital para continuar"
        } else {
            "Confirmá tu identidad para continuar"
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ID Digital — App de ejemplo")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Verificación pendiente",
            NotificationManager.IMPORTANCE_HIGH,
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}
