package uy.com.abitab.iddigitalsdk.presentation.pin.ui

import LoadingScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uy.com.abitab.iddigitalsdk.CallbackHandler
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

        val challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID)

        if (challengeId == null) {
            CallbackHandler.onError(
                IDDigitalError.SDKError.InvalidChallengeId("Challenge ID is null")
            )
            finish()
            return
        }

        registerPermissionLauncher(this)

        viewModel.setInitialState(challengeId)
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
                                    viewModel.requestPin()
                                }
                            }

                            is PinUiState.Loading -> {
                                setContent {
                                    LoadingScreen()
                                }
                            }

                            is PinUiState.ChallengeExecuted -> {
                                Log.d(
                                    "PinActivity",
                                    "ChallengeExecuted: ${uiState.challengeId}"
                                )
                                setContent {
                                    PinScreen(onCompleted = { pin ->
                                        viewModel.validateChallenge(pin)
                                    }, onBack = { finish() })
                                }
                            }

                            is PinUiState.ChallengeValidationError -> {
                                Log.d(
                                    "PinActivity",
                                    "ChallengeValidationError: ${uiState.challengeId}"
                                )
                                setContent {
                                    PinScreen(onCompleted = { pin ->
                                        viewModel.validateChallenge(pin)
                                    }, onBack = { finish() }, hasError = true)
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
        private const val EXTRA_CHALLENGE_ID = "EXTRA_CHALLENGE_ID"

        fun createIntent(
            context: Context,
            challengeId: String,
        ): Intent {
            return Intent(context, PinActivity::class.java).apply {
                putExtra(EXTRA_CHALLENGE_ID, challengeId)
            }
        }
    }
}