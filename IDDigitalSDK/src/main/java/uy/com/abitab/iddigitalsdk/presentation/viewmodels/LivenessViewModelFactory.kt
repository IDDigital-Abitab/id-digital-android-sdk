package uy.com.abitab.iddigitalsdk.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase

class LivenessViewModelFactory(
    private val createLivenessChallengeUseCase: CreateLivenessChallengeUseCase,
    private val executeLivenessChallengeUseCase: ExecuteLivenessChallengeUseCase,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LivenessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LivenessViewModel(createLivenessChallengeUseCase, executeLivenessChallengeUseCase, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}