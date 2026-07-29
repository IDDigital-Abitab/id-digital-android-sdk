package uy.com.abitab.iddigitalsdk.presentation.qr_association.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import uy.com.abitab.iddigitalsdk.presentation.qr_association.ui.screens.QrAssociationFlow

/**
 * Hosts [QrAssociationFlow] - QR cross-device association+login (registro
 * reducido), see .docs/sdk/cliente/08-qr-cross-device.md. No identification
 * parameter is required to launch this flow: the citizen's identity is only
 * revealed once the QR is scanned (the transactionId encoded in it), so the
 * app never needs to know the document beforehand - see [QrAssociationFlow].
 */
class QrAssociationActivity : AppCompatActivity() {
    private var activityScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()

        setContent {
            val context = LocalContext.current
            QrAssociationFlow(context = context, onClose = {
                finish()
            })
        }
    }

    private fun configureSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, QrAssociationActivity::class.java)
        }
    }
}
