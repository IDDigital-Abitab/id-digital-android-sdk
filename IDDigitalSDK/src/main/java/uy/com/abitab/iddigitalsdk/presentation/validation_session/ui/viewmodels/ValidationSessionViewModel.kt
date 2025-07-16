package uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import getDeviceAssociation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.domain.models.Challenge
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.models.ValidationSession
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateValidationSessionUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.ChallengeValidationError
import uy.com.abitab.iddigitalsdk.utils.DeviceNotAssociatedError
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.UnknownError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError
import java.time.Instant

class ValidationSessionViewModel(
    application: Application,
    private val createValidationSessionUseCase: CreateValidationSessionUseCase,
    private val executePinChallengeUseCase: ExecutePinChallengeUseCase,
    private val executeLivenessChallengeUseCase: ExecuteLivenessChallengeUseCase,
    private val validateLivenessChallengeUseCase: ValidateLivenessChallengeUseCase,
    private val validatePinChallengeUseCase: ValidatePinChallengeUseCase,
    private val pinDataStoreManager: PinDataStoreManager
) : AndroidViewModel(application) {

    private val _uiState = MutableSharedFlow<ValidationSessionUiState>(replay = 1)
    val uiState = _uiState.asSharedFlow()

    private val MAX_VALIDATION_ATTEMPTS = 3

    private lateinit var validationSession: ValidationSession
    private var challenges: List<Challenge> = emptyList()
    private var currentChallengeIndex = 0
    private var currentChallengeValidationErrors = 0
    private lateinit var type: ChallengeType
    private var viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var pinRecentlyChanged: Boolean = false

    fun setType(type: ChallengeType) {
        this.type = type
        viewModelScope.launch {
            _uiState.emit(ValidationSessionUiState.Initial)
        }
    }

    private suspend fun isAssociated(): Boolean {
        val context = getApplication<Application>()
        val retrievedAssociation = context.getDeviceAssociation().firstOrNull()
        return retrievedAssociation != null
    }

    fun createValidationSession(type: ChallengeType) {
        viewModelScope.launch {
            try {
                if (!isAssociated()) {
                    _uiState.emit(ValidationSessionUiState.Error(DeviceNotAssociatedError()))
                    return@launch
                }

                _uiState.emit(ValidationSessionUiState.Loading)
                val session = createValidationSessionUseCase(type)
                challenges = session.challenges
                validationSession = session
                currentChallengeIndex = 0
                launchNextChallenge()
            } catch (e: Throwable) {
                _uiState.emit(ValidationSessionUiState.Error(e.toIDDigitalError("Error creating validation session")))
            }
        }
    }

    private suspend fun launchNextChallenge() {
        if (currentChallengeIndex >= challenges.size) {
            viewModelScope.launch {
                _uiState.emit(ValidationSessionUiState.Success)
            }
            return
        }

        if (currentChallengeValidationErrors >= MAX_VALIDATION_ATTEMPTS) {
            _uiState.emit(ValidationSessionUiState.Error(ChallengeValidationError()))
            return
        }

        val challenge = challenges[currentChallengeIndex]
        val challengeType = ChallengeType.fromString(challenge.type)

        if (challengeType == null) {
            viewModelScope.launch {
                _uiState.emit(
                    ValidationSessionUiState.Error(
                        UnknownError("Invalid challenge type")
                    )
                )
            }
            return
        }

        _uiState.emit(
            ValidationSessionUiState.LaunchChallenge(
                challenge, currentChallengeValidationErrors > 0, pinRecentlyChanged
            )
        )
    }

    suspend fun executeChallenge(): Any? {
        val currentChallenge = challenges[currentChallengeIndex]
        val currentChallengeName = ChallengeType.fromString(currentChallenge.type)
        if (currentChallengeName == ChallengeType.Liveness) {
            try {
                _uiState.emit(ValidationSessionUiState.Loading)
                val result = executeLivenessChallengeUseCase(currentChallenge.id)
                return result
            } catch (e: Throwable) {
                _uiState.emit(ValidationSessionUiState.Error(e.toIDDigitalError("Error executing challenge")))
            }
        }
        if (currentChallengeName == ChallengeType.Pin) {
            try {
                _uiState.emit(ValidationSessionUiState.Loading)
                val backendPinLastUpdated = executePinChallengeUseCase(currentChallenge.id)
                val localLastBiometricUsageStr = pinDataStoreManager.getLastBiometricPinUsage()
                val localLastBiometricUsage = localLastBiometricUsageStr?.let { Instant.parse(it) }
                pinRecentlyChanged =
                    if (backendPinLastUpdated != null && localLastBiometricUsage != null) {
                        backendPinLastUpdated.isAfter(localLastBiometricUsage)
                    } else {
                        false
                    }

                return pinRecentlyChanged
            } catch (e: Throwable) {
                _uiState.emit(ValidationSessionUiState.Error(e.toIDDigitalError("Error executing challenge")))
            }
        }
        return null
    }

    suspend fun validateChallenge(data: Map<String, Any?> = emptyMap()) {
        val currentChallenge = challenges[currentChallengeIndex]
        val currentChallengeName = ChallengeType.fromString(currentChallenge.type)
        if (currentChallengeName == ChallengeType.Liveness) {
            try {
                _uiState.emit(ValidationSessionUiState.Loading)
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
                _uiState.emit(ValidationSessionUiState.Loading)
                val enteredPin = data["pin"] as? String
                val usedBiometric = data["usedBiometric"] as? Boolean ?: false
                val savePinToBiometrics = data["savePinToBiometrics"] as? Boolean ?: false
                validatePinChallengeUseCase(currentChallenge.id, enteredPin ?: "")

                if (savePinToBiometrics && enteredPin != null) {
                    pinDataStoreManager.savePinAndBiometricPreference(enteredPin, true)
                }

                if (usedBiometric || savePinToBiometrics) {
                    pinDataStoreManager.saveLastBiometricPinUsage(Instant.now().toString())
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

sealed class ValidationSessionUiState {
    object Initial : ValidationSessionUiState()
    object Loading : ValidationSessionUiState()
    data class LaunchChallenge(
        val challenge: Challenge,
        val isRetry: Boolean = false,
        val pinRecentlyChanged: Boolean = false
    ) : ValidationSessionUiState()

    object Success : ValidationSessionUiState()
    data class Error(val error: IDDigitalError) : ValidationSessionUiState()
}