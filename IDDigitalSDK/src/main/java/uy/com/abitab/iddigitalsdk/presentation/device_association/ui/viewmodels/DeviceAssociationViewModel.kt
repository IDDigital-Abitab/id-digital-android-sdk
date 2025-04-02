package uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.domain.models.Document
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository
import uy.com.abitab.iddigitalsdk.domain.usecases.LaunchChallengeActivityUseCase
import uy.com.abitab.iddigitalsdk.utils.IDDigitalError

class DeviceAssociationViewModel(
    application: Application,
    private val validationSessionRepository: ValidationSessionRepository,
    private val launchChallengeActivityUseCase: LaunchChallengeActivityUseCase,
    ) : AndroidViewModel(application) {
//    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
//
//    fun setActivityResultLauncher(launcher: ActivityResultLauncher<Intent>) {
//        activityResultLauncher = launcher
//    }
//
//    fun associateDevice(context: Context, document: Document) {
//        activityResultLauncher = registerActivityResult(context)
//        viewModelScope.launch {
//            try {
//                val session = validationSessionRepository.createDeviceAssociation(document)
//                for (challenge in session.data.challenges) {
//                    launchChallengeActivityUseCase(challenge)
//                    // Espera el resultado de la actividad
//                    // El resultado se manejará en onActivityResult
//                }
//            } catch (e: Exception) {
//                // Manejar el error
//            }
//        }
//    }
//
//    private fun registerActivityResult(context: Context): ActivityResultLauncher<Intent> {
//        return (context as ComponentActivity).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val challengeId = result.data?.getStringExtra("challengeId") ?: ""
//                val challengeType = result.data?.getStringExtra("challengeType") ?: ""
//                val response = result.data?.getStringExtra("response") ?: ""
//
//                viewModelScope.launch {
//                    validateChallenge(challengeId, challengeType, response)
//                }
//
//            } else {
//                // Manejar el caso en que la actividad no devolvió un resultado OK
//            }
//        }
//    }
//
//    suspend fun validateChallenge(challengeId: String, challengeType: String, response: Any) {
//        when (challengeType) {
//            "liveness" -> livenessRepository.validateChallenge(challengeId, response)
//            "pin" -> pinRepository.validateChallenge(challengeId, response)
//            else -> throw IllegalArgumentException("Tipo de desafío desconocido: $challengeType")
//        }
//    }
}

sealed class DeviceAssociationUiState {
    data class Initial(val document: Document) : LivenessUiState()
    object Loading : LivenessUiState()
    data class LivenessChallengeCreated(val challengeId: String) : LivenessUiState()
    data class LivenessChallengeExecuted(val challengeId: String, val sessionId: String) : LivenessUiState()
    data class LivenessChallengeCompleted(val challengeId: String) : LivenessUiState()
    data class Success(val challengeId: String) : LivenessUiState()
    data class Error(val error: IDDigitalError) : LivenessUiState()
}