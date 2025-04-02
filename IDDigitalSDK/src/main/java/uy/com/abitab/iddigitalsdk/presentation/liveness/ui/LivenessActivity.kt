package uy.com.abitab.iddigitalsdk.presentation.liveness.ui

import FaceLivenessComponent
import LoadingScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uy.com.abitab.iddigitalsdk.CallbackHandler
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessInstructionsScreen
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels.LivenessUiState
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels.LivenessViewModel
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager.registerPermissionLauncher

class LivenessActivity : ComponentActivity() {
    private val viewModel: LivenessViewModel by viewModel()

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
                            is LivenessUiState.Initial -> {
                                setContent {
                                    LivenessInstructionsScreen(onStart = {
                                        viewModel.startLiveness()
                                    }, onBack = { finish() })
                                }
                            }

                            is LivenessUiState.Loading -> {
                                setContent {
                                    LoadingScreen()
                                }
                            }

                            is LivenessUiState.ChallengeExecuted -> {
                                Log.d(
                                    "LivenessActivity", "ChallengeExecuted: ${uiState.challengeId}"
                                )
                                startFaceLivenessDetector(uiState.sessionId)
                            }

                            is LivenessUiState.ChallengeValidationError -> {
                                setContent {
                                    // @TODO: Handle invalid liveness
                                    Text("Invalid liveness")
                                }
                            }

                            is LivenessUiState.Success -> {
                                Log.d("LivenessActivity", "Success: ${uiState.challengeId}")
                                CallbackHandler.onCompleted(uiState.challengeId)
                                finish()
                            }

                            is LivenessUiState.Error -> {
                                Log.e("LivenessActivity", "Error: ${uiState.error}")
                                CallbackHandler.onError(uiState.error)
                                finish()
                            }

                            else -> {}
                        }
                    }
                }
                launch {
                    viewModel.permissionResultChannel.collect { isGranted ->
                        viewModel.onPermissionResult(isGranted)
                    }
                }
            }
        }
    }

    private fun configureSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
    }

    private fun startFaceLivenessDetector(sessionId: String) {
        lifecycleScope.launch {
            setContent {
                FaceLivenessComponent(sessionId = sessionId, onComplete = {
                    viewModel.onLivenessCompleted()
                }, onError = { error ->
                    viewModel.onLivenessError(error)
                })
            }
        }
    }


    companion object {
        private const val EXTRA_CHALLENGE_ID = "EXTRA_CHALLENGE_ID"

        fun createIntent(
            context: Context,
            challengeId: String,
        ): Intent {
            return Intent(context, LivenessActivity::class.java).apply {
                putExtra(EXTRA_CHALLENGE_ID, challengeId)
            }
        }
    }
}