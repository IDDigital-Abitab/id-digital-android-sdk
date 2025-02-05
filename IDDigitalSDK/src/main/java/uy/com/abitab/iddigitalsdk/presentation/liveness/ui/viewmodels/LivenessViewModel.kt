package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.IDDigitalError
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.PermissionsManagerInterface

class LivenessViewModel(
    application: Application,
    private val permissionsManager: PermissionsManagerInterface,
    private val createLivenessChallengeUseCase: CreateLivenessChallengeUseCase,
    private val executeLivenessChallengeUseCase: ExecuteLivenessChallengeUseCase,
    private val validateLivenessChallengeUseCase: ValidateLivenessChallengeUseCase

) : AndroidViewModel(application) {

    private val _permissionResultChannel = Channel<Boolean>()
    val permissionResultChannel = _permissionResultChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow<LivenessUiState>(LivenessUiState.Initial)
    val uiState: StateFlow<LivenessUiState> = _uiState.asStateFlow()

    private lateinit var document: Document
    private var cameraPermissionRequested = false

    fun startLiveness(document: Document) {
        this.document = document

        viewModelScope.launch {
            Log.d("LivenessViewModel", "Iniciando proceso de liveness")

            _uiState.value = LivenessUiState.Loading
            val context = getApplication<Application>().applicationContext

            if (!permissionsManager.hasCameraPermission(context)) {
                Log.d("LivenessViewModel", "Solicitando permiso de cámara")
                cameraPermissionRequested = true
                val isGranted = permissionsManager.requestCameraPermission(context)
                _permissionResultChannel.send(isGranted)
                return@launch
            }
            createChallenge(document)
        }
    }

    private fun createChallenge(document: Document) {
        viewModelScope.launch {
            val challengeId = try {
                createLivenessChallengeUseCase(document)
            } catch (e: Throwable) {
                _uiState.value =
                    LivenessUiState.Error(IDDigitalError.UnknownError("Error al crear el challenge: ${e.message}"))
                return@launch
            }

            try {
                executeLivenessChallengeUseCase(challengeId)
            } catch (e: Throwable) {
                _uiState.value =
                    LivenessUiState.Error(IDDigitalError.UnknownError("Error al ejecutar el challenge: ${e.message}"))
                return@launch
            }

            _uiState.value = LivenessUiState.ChallengeCreated(challengeId)
        }
    }

    fun validateChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                validateLivenessChallengeUseCase(challengeId)
                _uiState.value =
                    LivenessUiState.Success(challengeId)
            } catch (e: Throwable) {
                _uiState.value =
                    LivenessUiState.Error(IDDigitalError.UnknownError("Error validating challenge: ${e.message}"))
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
                _uiState.value =
                    LivenessUiState.Error(IDDigitalError.CameraPermissionError("Camera permission denied"))
                return@launch
            }
            createChallenge(document)
        }
    }

    fun onLivenessCompleted(challengeId: String) {
        viewModelScope.launch {
            _uiState.value = LivenessUiState.ChallengeCompleted(challengeId)
        }
    }
}

sealed class LivenessUiState {
    object Initial : LivenessUiState()
    object Loading : LivenessUiState()
    data class ChallengeCreated(val challengeId: String) : LivenessUiState()
    data class ChallengeCompleted(val challengeId: String) : LivenessUiState()
    data class Success(val challengeId: String) : LivenessUiState()
    data class Error(val error: IDDigitalError) : LivenessUiState()
}