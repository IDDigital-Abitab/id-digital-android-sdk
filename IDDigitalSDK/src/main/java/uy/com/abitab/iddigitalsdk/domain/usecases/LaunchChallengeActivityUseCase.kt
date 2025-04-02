package uy.com.abitab.iddigitalsdk.domain.usecases

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.LivenessActivity
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.PinActivity

sealed class ChallengeType {
    object Liveness : ChallengeType()
    object Pin : ChallengeType()
}

class LaunchChallengeActivityUseCase(
    private val livenessRepository: LivenessRepository,
    private val pinRepository: PinRepository,
    private val context: Context,
) {
    suspend operator fun invoke(challengeId: String, challengeType: ChallengeType) {
        when (challengeType) {
            ChallengeType.Liveness -> {
                livenessRepository.executeChallenge(challengeId)
                val intent = LivenessActivity.createIntent(context, challengeId)
                context.startActivity(intent)
            }

            ChallengeType.Pin -> {
                pinRepository.executeChallenge(challengeId)
                val intent = PinActivity.createIntent(context, challengeId)
                context.startActivity(intent)
            }

            else -> {
                throw IllegalArgumentException("Tipo de desafío desconocido: ${challengeType}")
            }
        }
    }
}