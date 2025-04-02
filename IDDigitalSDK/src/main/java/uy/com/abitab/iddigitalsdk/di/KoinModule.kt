package uy.com.abitab.iddigitalsdk.di

import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.data.network.LivenessService
import uy.com.abitab.iddigitalsdk.data.network.PinService
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateAndLaunchLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateAndLaunchPinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.LaunchChallengeActivityUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels.LivenessViewModel
import uy.com.abitab.iddigitalsdk.presentation.pin.ui.viewmodels.PinViewModel
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializerInterface
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager
import uy.com.abitab.iddigitalsdk.utils.PermissionsManagerInterface
import uy.com.abitab.iddigitalsdk.utils.getDeviceFingerprint
import java.util.concurrent.TimeUnit


internal fun sdkModule() = module {
    single<PermissionsManagerInterface> { PermissionsManager }
    single<AmplifyInitializerInterface> { AmplifyInitializer }

    single {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-api-key", IDDigitalSDK.getApiKey())
                    .header("x-device-fingerprint", getDeviceFingerprint(this.androidContext()))
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // --- SERVICES ---
    single { LivenessService(get(), get()) }
    single { PinService(get(), get()) }

    // --- REPOSITORIES ---
    single<LivenessRepository> { LivenessRepositoryImpl(get()) }
    single<PinRepository> { PinRepositoryImpl(get()) }
    single<ValidationSessionRepository> { ValidationSessionRepositoryImpl(get()) }


    // --- USE CASES ---
    factory { LaunchChallengeActivityUseCase(get(), get(), get()) }
    // liveness
    factory { CreateAndLaunchLivenessChallengeUseCase(get(), get()) }
    factory { ExecuteLivenessChallengeUseCase(get()) }
    factory { ValidateLivenessChallengeUseCase(get()) }
    // pin
    factory { CreateAndLaunchPinChallengeUseCase(get(), get()) }
    factory { ExecutePinChallengeUseCase(get()) }
    factory { ValidatePinChallengeUseCase(get()) }
    // validation session
//    factory { CreateDeviceAssociationUseCase(get()) }
//    factory { CreateChallengeUseCase(get()) }
//    factory { ExecuteChallengeUseCase(get()) }
//    factory { ValidateChallengeUseCase(get()) }


    // --- VIEW MODELS ---
    viewModelOf(::LivenessViewModel)
    viewModelOf(::PinViewModel)
}