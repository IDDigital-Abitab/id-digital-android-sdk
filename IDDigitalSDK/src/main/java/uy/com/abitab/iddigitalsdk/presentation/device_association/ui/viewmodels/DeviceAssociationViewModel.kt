package uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import saveDeviceAssociation
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.domain.models.Challenge
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.domain.usecases.CheckCanAssociateUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.ChallengeValidationError
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.UnknownError
import uy.com.abitab.iddigitalsdk.utils.UserCannotBeAssociatedError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError
import uy.com.abitab.iddigitalsdk.domain.models.Document as DocumentModel

class DeviceAssociationViewModel(
    application: Application,
    private val checkCanAssociateUseCase: CheckCanAssociateUseCase,
    private val createDeviceAssociationUseCase: CreateDeviceAssociationUseCase,
    private val completeDeviceAssociationUseCase: CompleteDeviceAssociationUseCase,
    private val executePinChallengeUseCase: ExecutePinChallengeUseCase,
    private val executeLivenessChallengeUseCase: ExecuteLivenessChallengeUseCase,
    private val validateLivenessChallengeUseCase: ValidateLivenessChallengeUseCase,
    private val validatePinChallengeUseCase: ValidatePinChallengeUseCase,
    private val pinDataStoreManager: PinDataStoreManager
) : AndroidViewModel(application) {

    private val _uiState = MutableSharedFlow<DeviceAssociationUiState>(replay = 1)
    val uiState = _uiState.asSharedFlow()

    private val MAX_VALIDATION_ATTEMPTS = 3

    private lateinit var deviceAssociationSession: ValidationSession
    private var challenges: List<Challenge> = emptyList()
    private var currentChallengeIndex = 0
    private var currentChallengeValidationErrors = 0
    private lateinit var document: DocumentModel
    private var viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)


    fun setDocument(document: DocumentModel) {
        this.document = document
        viewModelScope.launch {
            _uiState.emit(DeviceAssociationUiState.Initial)
        }
    }


    fun startDeviceAssociation() {
        viewModelScope.launch {
            try {
                _uiState.emit(DeviceAssociationUiState.Loading)
                val canAssociate = checkCanAssociateUseCase(document)
                if (!canAssociate) {
                    _uiState.emit(DeviceAssociationUiState.Error(UserCannotBeAssociatedError()))
                    return@launch
                }
                val session = createDeviceAssociationUseCase(document)
                challenges = session.challenges
                deviceAssociationSession = session
                currentChallengeIndex = 0
                launchNextChallenge()
            } catch (e: Exception) {
                _uiState.emit(DeviceAssociationUiState.Error(UnknownError("Error starting association: ${e.message}")))
            }
        }
    }

    private suspend fun finishDeviceAssociation() {
        if (!::deviceAssociationSession.isInitialized) {
            throw IllegalStateException("deviceAssociationSession is not initialized")
        }
        val deviceAssociation = completeDeviceAssociationUseCase(deviceAssociationSession.id)

        val context = getApplication<Application>()

        context.saveDeviceAssociation(deviceAssociation)
        viewModelScope.launch {
            _uiState.emit(DeviceAssociationUiState.Success)
        }
    }

    private suspend fun launchNextChallenge() {
        if (currentChallengeIndex >= challenges.size) {
            finishDeviceAssociation()
            return
        }

        if (currentChallengeValidationErrors >= MAX_VALIDATION_ATTEMPTS) {
            _uiState.emit(DeviceAssociationUiState.Error(ChallengeValidationError()))
            return
        }

        val challenge = challenges[currentChallengeIndex]
        val challengeType = ChallengeType.fromString(challenge.type)


        if (challengeType == null) {
            viewModelScope.launch {
                _uiState.emit(
                    DeviceAssociationUiState.Error(
                        UnknownError("Invalid challenge type")
                    )
                )
            }
            return
        }

        _uiState.emit(
            DeviceAssociationUiState.LaunchChallenge(
                challenge,
                currentChallengeValidationErrors > 0
            )
        )
    }

    suspend fun executeChallenge(): String? {
        val currentChallenge = challenges[currentChallengeIndex]
        val currentChallengeName = ChallengeType.fromString(currentChallenge.type)
        if (currentChallengeName == ChallengeType.Liveness) {
            try {
                _uiState.emit(DeviceAssociationUiState.Loading)
                return executeLivenessChallengeUseCase(currentChallenge.id)
            } catch (e: Throwable) {
                _uiState.emit(DeviceAssociationUiState.Error(e.toIDDigitalError("Error executing challenge")))
            }
        }
        if (currentChallengeName == ChallengeType.Pin) {
            try {
                _uiState.emit(DeviceAssociationUiState.Loading)
                executePinChallengeUseCase(currentChallenge.id)
                return null
            } catch (e: Throwable) {
                _uiState.emit(DeviceAssociationUiState.Error(e.toIDDigitalError("Error executing challenge")))
            }
        }
        return null
    }

    suspend fun validateChallenge(data: Any? = null, options: Map<String, Any>? = null) {
        val currentChallenge = challenges[currentChallengeIndex]
        val currentChallengeName = ChallengeType.fromString(currentChallenge.type)
        if (currentChallengeName == ChallengeType.Liveness) {
            try {
                _uiState.emit(DeviceAssociationUiState.Loading)
                val isValid = validateLivenessChallengeUseCase(currentChallenge.id)
                if (isValid) {
                    currentChallengeIndex++
                    currentChallengeValidationErrors = 0
                } else {
                    currentChallengeValidationErrors++
                }
                launchNextChallenge()
            } catch (e: Throwable) {
                currentChallengeValidationErrors++
                launchNextChallenge()
            }
        }
        if (currentChallengeName == ChallengeType.Pin) {
            try {
                _uiState.emit(DeviceAssociationUiState.Loading)
                val enteredPin = data as String
                validatePinChallengeUseCase(currentChallenge.id, enteredPin)
                if (options?.get("saveBiometricPin") == true) {
                    pinDataStoreManager.savePinAndBiometricPreference(enteredPin, true)
                }
                currentChallengeIndex++
                launchNextChallenge()
            } catch (e: Throwable) {
                currentChallengeValidationErrors++
                launchNextChallenge()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

sealed class DeviceAssociationUiState {
    object Initial : DeviceAssociationUiState()
    object Loading : DeviceAssociationUiState()
    data class LaunchChallenge(val challenge: Challenge, val isRetry: Boolean = false) :
        DeviceAssociationUiState()

    object Success : DeviceAssociationUiState()
    data class Error(val error: IDDigitalError) : DeviceAssociationUiState()
}
