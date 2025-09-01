package uy.com.abitab.iddigitalsdk.presentation.validation_session.ui

import android.content.Context
import android.content.Intent
import android.os.Build
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
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.screens.ValidationSession
import uy.com.abitab.iddigitalsdk.utils.InvalidDocumentError

class ValidationSessionActivity : AppCompatActivity() {
    private var activityScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()
        val challengeType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CHALLENGE_TYPE, ChallengeType::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_CHALLENGE_TYPE) as? ChallengeType
        }

        if (challengeType == null) {
            CallbackHandler.onError(
                InvalidDocumentError("challengeType is null")
            )
            finish()
            return
        }

        setContent {
            val context = LocalContext.current
            ValidationSession(challengeType= challengeType, context = context, onClose = {
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
        private const val EXTRA_CHALLENGE_TYPE = "EXTRA_CHALLENGE_TYPE"

        fun createIntent(
            context: Context,
            challengeType: ChallengeType,
        ): Intent {
            return Intent(context, ValidationSessionActivity::class.java).apply {
                putExtra(EXTRA_CHALLENGE_TYPE, challengeType)
            }
        }
    }
}