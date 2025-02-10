package uy.com.abitab.iddigitalsdk.presentation.pin.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.usecases.CreatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.utils.toIDDigitalError

class PinViewModel(
    application: Application,
    private val createPinChallengeUseCase: CreatePinChallengeUseCase,
    private val executePinChallengeUseCase: ExecutePinChallengeUseCase,
    private val validatePinChallengeUseCase: ValidatePinChallengeUseCase,

) : AndroidViewModel(application) {
    private val _uiState = MutableSharedFlow<PinUiState>(replay = 1)
    val uiState = _uiState.asSharedFlow()

    private lateinit var document: Document

    fun requestPin() {
        viewModelScope.launch {
            Log.d("PinViewModel", "Starting requestPin")

            _uiState.emit(PinUiState.Loading)

            createChallenge()
        }
    }

    private fun createChallenge() {
        viewModelScope.launch {
            Log.d("PinViewModel", "createChallenge")
            val challengeId = try {
                createPinChallengeUseCase(this@PinViewModel.document)
            } catch (e: Throwable) {
                Log.e("PinViewModel", "Error al crear el challenge", e)
                val error = e.toIDDigitalError("Error creating challenge")
                _uiState.emit(PinUiState.Error(error))
                return@launch
            }
            _uiState.emit(PinUiState.ChallengeCreated(challengeId))
        }
    }

    fun executeChallenge(challengeId: String) {
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

    fun validateChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                validatePinChallengeUseCase(challengeId)
                _uiState.emit(PinUiState.Success(challengeId))
            } catch (e: Throwable) {
                _uiState.emit(PinUiState.Error(IDDigitalError.UnknownError("Error validating challenge: ${e.message}")))
                return@launch
            }
        }

    }

    fun setInitialState(document: Document){
        this.document = document
        viewModelScope.launch {
            _uiState.emit(PinUiState.Initial(document))
        }
    }
}

sealed class PinUiState {
    data class Initial(val document: Document) : PinUiState()
    object Loading : PinUiState()
    data class ChallengeCreated(val challengeId: String) : PinUiState()
    data class ChallengeExecuted(val challengeId: String) : PinUiState()
    data class ChallengeCompleted(val challengeId: String) : PinUiState()
    data class Success(val challengeId: String) : PinUiState()
    data class Error(val error: IDDigitalError) : PinUiState()
}