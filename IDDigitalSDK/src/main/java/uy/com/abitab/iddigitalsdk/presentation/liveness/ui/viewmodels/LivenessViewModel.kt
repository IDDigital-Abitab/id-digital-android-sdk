package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.PermissionsManagerInterface
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class LivenessViewModel(
    application: Application,
    private val permissionsManager: PermissionsManagerInterface,
    private val createLivenessChallengeUseCase: CreateLivenessChallengeUseCase,
    private val executeLivenessChallengeUseCase: ExecuteLivenessChallengeUseCase,
    private val validateLivenessChallengeUseCase: ValidateLivenessChallengeUseCase

) : AndroidViewModel(application) {

    private val _permissionResultChannel = Channel<Boolean>()
    val permissionResultChannel = _permissionResultChannel.receiveAsFlow()

    private val _uiState = MutableSharedFlow<LivenessUiState>(replay = 1)
    val uiState = _uiState.asSharedFlow()

    private lateinit var document: Document
    private var cameraPermissionRequested = false


    fun startLiveness() {
        viewModelScope.launch {
            Log.d("LivenessViewModel", "Iniciando proceso de liveness")

            _uiState.emit(LivenessUiState.Loading)
            val context = getApplication<Application>().applicationContext

            if (!permissionsManager.hasCameraPermission(context)) {
                Log.d("LivenessViewModel", "Solicitando permiso de cámara")
                cameraPermissionRequested = true
                val isGranted = permissionsManager.requestCameraPermission(context)
                _permissionResultChannel.send(isGranted)
                return@launch
            }
            createChallenge()
        }
    }

    private fun createChallenge() {
        viewModelScope.launch {
            Log.d("LivenessViewModel", "createChallenge")
            val challengeId = try {
                createLivenessChallengeUseCase(this@LivenessViewModel.document)
            } catch (e: Throwable) {
                Log.e("LivenessViewModel", "Error al crear el challenge", e)
                val error = e.toIDDigitalError("Error creating challenge")
                _uiState.emit(LivenessUiState.Error(error))
                return@launch
            }
            _uiState.emit(LivenessUiState.ChallengeCreated(challengeId))
        }
    }

    fun executeChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                val sessionId = executeLivenessChallengeUseCase(challengeId)
                _uiState.emit(LivenessUiState.ChallengeExecuted(challengeId, sessionId))
            } catch (e: Throwable) {
                Log.e("LivenessViewModel", "Error al ejecutar el challenge", e)
                _uiState.emit(LivenessUiState.Error(e.toIDDigitalError("Error executing challenge")))
            }
        }
    }

    fun validateChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                validateLivenessChallengeUseCase(challengeId)
                _uiState.emit(LivenessUiState.Success(challengeId))
            } catch (e: Throwable) {
                _uiState.emit(LivenessUiState.Error(IDDigitalError.UnknownError("Error validating challenge: ${e.message}")))
                return@launch
            }
        }

    }

    fun onPermissionResult(isGranted: Boolean) {
        viewModelScope.launch {
            if (!cameraPermissionRequested) return@launch
            cameraPermissionRequested = false
            if (!isGranted) {
                Log.e("LivenessViewModel", "Camera permission denied")
                _uiState.emit(LivenessUiState.Error(IDDigitalError.CameraPermissionError("Camera permission denied")))
                return@launch
            }
            createChallenge()
        }
    }

    fun onLivenessCompleted(challengeId: String) {
        viewModelScope.launch {
            _uiState.emit(LivenessUiState.ChallengeCompleted(challengeId))
        }
    }

    fun onLivenessError(error: FaceLivenessDetectionException) {
        val idDigitalError = when (error) {
            is FaceLivenessDetectionException.UserCancelledException ->
                IDDigitalError.UserCancelledError(
                    "User cancelled the validation process",
                )

            is FaceLivenessDetectionException.CameraPermissionDeniedException ->
                IDDigitalError.CameraPermissionError(
                    "Camera permission denied",
                    error.throwable
                )

            is FaceLivenessDetectionException.SessionTimedOutException ->
                IDDigitalError.TimeoutError(
                    "Session timed out",
                    error.throwable

                )

            else -> IDDigitalError.UnknownError(
                "An unexpected error occurred",
                error.throwable
            )
        }
        viewModelScope.launch {
            _uiState.emit(LivenessUiState.Error(idDigitalError))
        }
    }

    fun setInitialState(document: Document){
        this.document = document
        viewModelScope.launch {
            _uiState.emit(LivenessUiState.Initial(document))
        }
    }
}

sealed class LivenessUiState {
    data class Initial(val document: Document) : LivenessUiState()
    object Loading : LivenessUiState()
    data class ChallengeCreated(val challengeId: String) : LivenessUiState()
    data class ChallengeExecuted(val challengeId: String, val sessionId: String) : LivenessUiState()
    data class ChallengeCompleted(val challengeId: String) : LivenessUiState()
    data class Success(val challengeId: String) : LivenessUiState()
    data class Error(val error: IDDigitalError) : LivenessUiState()
}