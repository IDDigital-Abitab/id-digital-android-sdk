package uy.com.abitab.iddigitalsdk.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import uy.com.abitab.iddigitalsdk.data.network.LivenessService
import uy.com.abitab.iddigitalsdk.data.repositories.LivenessRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.presentation.viewmodels.LivenessViewModel
import okhttp3.OkHttpClient
import uy.com.abitab.iddigitalsdk.IDDigitalSDK
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializerInterface
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager
import uy.com.abitab.iddigitalsdk.utils.PermissionsManagerInterface
import java.util.concurrent.TimeUnit

internal fun sdkModule() = module {

    single<PermissionsManagerInterface> { PermissionsManager }
    single<AmplifyInitializerInterface> { AmplifyInitializer }

    single {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Api-Key ${IDDigitalSDK.getApiKey()}")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single { LivenessService(get()) }

    single<LivenessRepository> { LivenessRepositoryImpl(get()) }

    factory { CreateLivenessChallengeUseCase(get()) }

    factory { ExecuteLivenessChallengeUseCase(get()) }

    factory { ValidateLivenessChallengeUseCase(get()) }

    viewModel {
        LivenessViewModel(get(), get(), get(), get())
    }
}