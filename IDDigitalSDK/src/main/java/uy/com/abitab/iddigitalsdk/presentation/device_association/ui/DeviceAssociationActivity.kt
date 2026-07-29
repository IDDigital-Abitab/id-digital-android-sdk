package uy.com.abitab.iddigitalsdk.presentation.device_association.ui

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
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.screens.DeviceAssociation
import uy.com.abitab.iddigitalsdk.utils.UnknownError

class DeviceAssociationActivity : AppCompatActivity() {
    private var activityScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)

        if (transactionId == null) {
            CallbackHandler.onError(
                UnknownError("transactionId is null")
            )
            finish()
            return
        }

        setContent {
            val context = LocalContext.current
            DeviceAssociation(transactionId = transactionId, context = context, onClose = {
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
        private const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"

        fun createIntent(
            context: Context,
            transactionId: String,
        ): Intent {
            return Intent(context, DeviceAssociationActivity::class.java).apply {
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
            }
        }
    }
}