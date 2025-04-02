package uy.com.abitab.iddigitalsdk.presentation.pin.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class PinViewModel(
    application: Application,
    private val executePinChallengeUseCase: ExecutePinChallengeUseCase,
    private val validatePinChallengeUseCase: ValidatePinChallengeUseCase,

) : AndroidViewModel(application) {
    private val _uiState = MutableSharedFlow<PinUiState>(replay = 1)
    val uiState = _uiState.asSharedFlow()

    private lateinit var challengeId: String

    fun requestPin() {
        viewModelScope.launch {
            Log.d("PinViewModel", "Starting requestPin")

            _uiState.emit(PinUiState.Loading)
            executeChallenge()
        }
    }

    fun executeChallenge() {
        viewModelScope.launch {
            try {
                executePinChallengeUseCase(challengeId)
                _uiState.emit(PinUiState.ChallengeExecuted(challengeId))
            } catch (e: Throwable) {
                Log.e("PinViewModel", "Error al ejecutar el challenge", e)
                _uiState.emit(PinUiState.Error(e.toIDDigitalError("Error executing challenge")))
            }
        }
    }

    fun validateChallenge(pin: String) {
        viewModelScope.launch {
            try {
                val isValid = try {
                    validatePinChallengeUseCase(challengeId, pin)
                } catch (e: Throwable) {
                    Log.e("PinViewModel", "Error al validar el challenge", e)
                    val error = e.toIDDigitalError("Error validating challenge")
                    _uiState.emit(PinUiState.Error(error))
                    return@launch
                }

                if (!isValid) {
                    _uiState.emit(PinUiState.ChallengeValidationError(challengeId))
                }
                else {
                    _uiState.emit(PinUiState.Success(challengeId))
                }
            } catch (e: Throwable) {
                if (e is IDDigitalError) {
                    _uiState.emit(PinUiState.Error(e))
                    return@launch
                }
                _uiState.emit(PinUiState.Error(IDDigitalError.UnknownError("Error validating challenge: ${e.message}"),))
                return@launch
            }
        }

    }

    fun setInitialState(challengeId: String){
        this.challengeId = challengeId
        viewModelScope.launch {
            _uiState.emit(PinUiState.Initial)
        }
    }
}

sealed class PinUiState {
    object Initial : PinUiState()
    object Loading : PinUiState()
    data class ChallengeExecuted(val challengeId: String) : PinUiState()
    data class ChallengeValidationError(val challengeId: String) : PinUiState()
    data class Success(val challengeId: String) : PinUiState()
    data class Error(val error: IDDigitalError) : PinUiState()
}