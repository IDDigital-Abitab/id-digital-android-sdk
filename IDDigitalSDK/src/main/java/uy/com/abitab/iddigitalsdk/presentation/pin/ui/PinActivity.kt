package uy.com.abitab.iddigitalsdk.presentation.pin.ui

import LoadingScreen
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens.PinScreen
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.viewmodels.PinUiState
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.viewmodels.PinViewModel
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher

class PinActivity : ComponentActivity() {
    private val viewModel: PinViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureSystemUI()

        val document = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_DOCUMENT, Document::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_DOCUMENT) as? Document
        }

        if (document == null) {
            CallbackHandler.onError(
                IDDigitalError.SDKError.InvalidDocument("Document is null")
            )
            finish()
            return
        }

        registerPermissionLauncher(this)

        viewModel.setInitialState(document)
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        when (uiState) {
                            is PinUiState.Initial -> {
                                setContent {
                                    LoadingScreen()
                                    viewModel.createChallenge()
                                }
                            }

                            is PinUiState.Loading -> {
                                setContent {
                                    LoadingScreen()
                                }
                            }

                            is PinUiState.ChallengeCreated -> {
                                Log.d(
                                    "PinActivity",
                                    "ChallengeCreated: ${uiState.challengeId}"
                                )
                                viewModel.executeChallenge(uiState.challengeId)
                            }

                            is PinUiState.ChallengeExecuted -> {
                                Log.d(
                                    "PinActivity",
                                    "ChallengeExecuted: ${uiState.challengeId}"
                                )
                                setContent {
                                    PinScreen(onCompleted = { pin ->
                                        viewModel.validateChallenge(uiState.challengeId, pin)
                                    }, onBack = { finish() })
                                }
                            }

                            is PinUiState.ChallengeValidated -> {
                                Log.d("PinActivity", "ChallengeValidated: ${uiState.challengeId}, result: ${uiState.isValid}")

                                if (!uiState.isValid) {
                                    setContent {
                                        PinScreen(onCompleted = { pin ->
                                            viewModel.validateChallenge(uiState.challengeId, pin)
                                        }, onBack = { finish() }, hasError=true)
                                    }
                                }
                            }

                            is PinUiState.Success -> {
                                Log.d("PinActivity", "Success: ${uiState.challengeId}")
                                CallbackHandler.onCompleted(uiState.challengeId)
                                finish()
                            }

                            is PinUiState.Error -> {
                                Log.e("PinActivity", "Error: ${uiState.error}")
                                CallbackHandler.onError(uiState.error)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
    }

    companion object {

        private const val EXTRA_DOCUMENT = "EXTRA_DOCUMENT"

        fun createIntent(
            context: Context,
            document: Document,

            ): Intent {
            return Intent(context, PinActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT, document)
            }
        }
    }
}