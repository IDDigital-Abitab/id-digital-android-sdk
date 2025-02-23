package uy.com.abitab.iddigitalsdk.presentation.liveness.ui

import FaceLivenessComponent
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
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.screens.LivenessCompletedLoadingScreen
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

                            is LivenessUiState.ChallengeCreated -> {
                                Log.d(
                                    "LivenessActivity",
                                    "ChallengeCreated: ${uiState.challengeId}"
                                )
                                viewModel.executeChallenge(uiState.challengeId)
                            }

                            is LivenessUiState.ChallengeExecuted -> {
                                Log.d(
                                    "LivenessActivity",
                                    "ChallengeExecuted: ${uiState.challengeId}"
                                )
                                startFaceLivenessDetector(uiState.challengeId, uiState.sessionId)
                            }

                            is LivenessUiState.ChallengeCompleted -> {
                                Log.d(
                                    "LivenessActivity",
                                    "ChallengeCompleted: ${uiState.challengeId}"
                                )
                                setContent {
                                    LivenessCompletedLoadingScreen()
                                }
                                viewModel.validateChallenge(uiState.challengeId)
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

    private fun startFaceLivenessDetector(challengeId: String, sessionId: String) {
        lifecycleScope.launch {

                setContent {
                    FaceLivenessComponent(
                        sessionId = sessionId,
                        onComplete = {
                            viewModel.onLivenessCompleted(challengeId)
                        },
                        onError = { error ->
                            viewModel.onLivenessError(error)
                        }
                    )


                }

        }
    }


    companion object {
        private const val EXTRA_DOCUMENT = "EXTRA_DOCUMENT"

        fun createIntent(
            context: Context,
            document: Document,

            ): Intent {
            return Intent(context, LivenessActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT, document)
            }
        }
    }
}