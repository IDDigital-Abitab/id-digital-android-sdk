package uy.com.abitab.iddigitalsdk.di

import getDeviceAssociation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.data.network.ConfigService
import uy.com.abitab.iddigitalsdk.data.network.LivenessService
import uy.com.abitab.iddigitalsdk.data.network.PinService
import uy.com.abitab.iddigitalsdk.data.network.ValidationSessionService
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.LivenessRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.PinRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepositoryImpl
import uy.com.abitab.iddigitalsdk.domain.usecases.CheckCanAssociateUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateValidationSessionUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.RemoveAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels.DeviceAssociationViewModel
import uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels.ValidationSessionViewModel
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializer
import uy.com.abitab.iddigitalsdk.utils.AmplifyInitializerInterface
import uy.com.abitab.iddigitalsdk.utils.PermissionsManager
import uy.com.abitab.iddigitalsdk.utils.PermissionsManagerInterface
import uy.com.abitab.iddigitalsdk.utils.getDeviceFingerprint
import java.util.concurrent.TimeUnit


internal fun sdkModule() = module {
    single<PermissionsManagerInterface> { PermissionsManager }
    single<AmplifyInitializerInterface> { AmplifyInitializer }

    single { PinDataStoreManager(androidContext()) }

    factory {
        val context = androidContext()
        val apiKey: String? = getKoin().getProperty("apiKey")

        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val fingerprint = runBlocking {
                    getDeviceFingerprint(context)
                }
                val token = runBlocking {
                    context.getDeviceAssociation().first()?.token
                }
                val request = chain.request().newBuilder()
                if (apiKey != null)
                    request.header("x-api-key", apiKey)
                request.header("x-device-fingerprint", fingerprint)
                if (token != null)
                    request.header("Authorization", "Bearer $token")

                chain.proceed(request.build())
            }
            .connectTimeout(30000, TimeUnit.SECONDS)
            .readTimeout(30000, TimeUnit.SECONDS)
            .writeTimeout(30000, TimeUnit.SECONDS)
            .build()
    }

    // --- SERVICES ---
    single { LivenessService(get(), get()) }
    single { PinService(get(), get()) }
    single { ValidationSessionService(get(), get()) }
    single { ConfigService(get(), get()) }

    // --- REPOSITORIES ---
    single<LivenessRepository> { LivenessRepositoryImpl(get()) }
    single<PinRepository> { PinRepositoryImpl(get()) }
    single<ValidationSessionRepository> { ValidationSessionRepositoryImpl(get()) }


    // --- USE CASES ---
    // liveness
    factory { ExecuteLivenessChallengeUseCase(get()) }
    factory { ValidateLivenessChallengeUseCase(get()) }
    // pin
    factory { ExecutePinChallengeUseCase(get()) }
    factory { ValidatePinChallengeUseCase(get(), Dispatchers.IO) }
    // device association
    factory { CheckCanAssociateUseCase(get(), Dispatchers.IO) }
    factory { CreateDeviceAssociationUseCase(get(), Dispatchers.IO) }
    factory { CompleteDeviceAssociationUseCase(get(), Dispatchers.IO) }
    factory { RemoveAssociationUseCase(get(), Dispatchers.IO) }
    // validation session
    factory { CreateValidationSessionUseCase(get(), Dispatchers.IO) }


    // --- VIEW MODELS ---
    viewModelOf(::DeviceAssociationViewModel)
    viewModelOf(::ValidationSessionViewModel)
}