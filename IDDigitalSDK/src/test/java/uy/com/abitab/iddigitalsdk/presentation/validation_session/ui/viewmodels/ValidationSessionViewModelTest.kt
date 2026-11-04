package uy.com.abitab.iddigitalsdk.presentation.validation_session.ui.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import getDeviceAssociation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import removeDeviceAssociation
import saveDeviceAssociation
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.domain.models.ChallengeType
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateValidationSessionUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.support.FakeLivenessRepository
import uy.com.abitab.iddigitalsdk.support.FakePinRepository
import uy.com.abitab.iddigitalsdk.support.FakeValidationSessionRepository
import uy.com.abitab.iddigitalsdk.support.deviceAssociation
import uy.com.abitab.iddigitalsdk.support.idleMainUntil
import uy.com.abitab.iddigitalsdk.support.validationSession
import uy.com.abitab.iddigitalsdk.utils.ChallengeValidationError
import uy.com.abitab.iddigitalsdk.utils.DeviceNotAssociatedError

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ValidationSessionViewModelTest {

    private val sessions = FakeValidationSessionRepository()
    private val pins = FakePinRepository()
    private val liveness = FakeLivenessRepository()
    private val pinStore: PinDataStoreManager = mockk(relaxed = true)
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = ApplicationProvider.getApplicationContext()
        runBlocking { application.removeDeviceAssociation() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        if (::application.isInitialized) {
            runBlocking { application.removeDeviceAssociation() }
        }
    }

    @Test
    fun authentication_requiresAnAssociatedDevice() = testFlow { viewModel, states ->
        // Given this device has no local association
        // When the app starts authentication
        viewModel.createValidationSession(ChallengeType.Pin)
        idleMainUntil({ "Missing association was not reported. states=$states" }) {
            states.any { it is ValidationSessionUiState.Error }
        }

        // Then the SDK reports that the device is not associated
        val error = states.filterIsInstance<ValidationSessionUiState.Error>().single().error
        assertTrue(error is DeviceNotAssociatedError)
    }

    @Test
    fun authentication_returnsValidationSessionIdWhenPinSucceeds() = testFlow { viewModel, states ->
        // Given an associated device and a PIN session
        application.saveDeviceAssociation(deviceAssociation())
        sessions.createdSession = validationSession(id = "validation-3")
        viewModel.createValidationSession(ChallengeType.Pin)
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is ValidationSessionUiState.LaunchChallenge }
        }

        // When the citizen completes the PIN
        viewModel.validateChallenge(mapOf("pin" to "1234"))
        idleMainUntil({ "Authentication did not finish. states=$states" }) {
            states.any { it is ValidationSessionUiState.Success }
        }

        // Then the app receives the validation session id for completeTransaction
        val success = states.filterIsInstance<ValidationSessionUiState.Success>().single()
        assertEquals("validation-3", success.validationSessionId)
    }

    @Test
    fun authentication_clearsAssociationWhenTokenIsRejected() = testFlow { viewModel, states ->
        // Given a stale local association the backend no longer accepts
        application.saveDeviceAssociation(deviceAssociation())
        sessions.failure = DeviceNotAssociatedError()

        // When authentication starts
        viewModel.createValidationSession(ChallengeType.Pin)
        idleMainUntil({ "Rejected token was not reported. states=$states" }) {
            states.any { it is ValidationSessionUiState.Error }
        }

        // Then the SDK drops the local association so the next attempt can associate again
        val error = states.filterIsInstance<ValidationSessionUiState.Error>().single().error
        assertTrue(error is DeviceNotAssociatedError)
        assertNull(application.getDeviceAssociation().firstOrNull())
    }

    @Test
    fun authentication_executesPinChallenge() = testFlow { viewModel, states ->
        // Given an associated device waiting on PIN
        application.saveDeviceAssociation(deviceAssociation())
        viewModel.createValidationSession(ChallengeType.Pin)
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is ValidationSessionUiState.LaunchChallenge }
        }

        // When the SDK executes that challenge
        coEvery { pinStore.getLastBiometricPinUsage() } returns null
        val pinRecentlyChanged = viewModel.executeChallenge()

        // Then there is no newer PIN on the backend than the one stored for biometrics
        assertEquals(false, pinRecentlyChanged)
        assertEquals("ch-1", pins.executedChallengeId)
    }

    @Test
    fun authentication_failsAfterThreeInvalidPins() = testFlow { viewModel, states ->
        // Given an associated device and a PIN the backend keeps rejecting
        application.saveDeviceAssociation(deviceAssociation())
        pins.onValidate = { _, _ -> throw IllegalStateException("invalid pin") }
        viewModel.createValidationSession(ChallengeType.Pin)
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is ValidationSessionUiState.LaunchChallenge }
        }

        // When the citizen fails three times
        repeat(3) { viewModel.validateChallenge(mapOf("pin" to "0000")) }
        idleMainUntil({ "Challenge was not exhausted. states=$states" }) {
            states.any { it is ValidationSessionUiState.Error }
        }

        // Then the flow stops with a challenge validation error
        val error = states.filterIsInstance<ValidationSessionUiState.Error>().last().error
        assertTrue(error is ChallengeValidationError)
    }

    private fun viewModel() = ValidationSessionViewModel(
        application,
        CreateValidationSessionUseCase(sessions, UnconfinedTestDispatcher()),
        ExecutePinChallengeUseCase(pins),
        ExecuteLivenessChallengeUseCase(liveness),
        ValidateLivenessChallengeUseCase(liveness),
        ValidatePinChallengeUseCase(pins, UnconfinedTestDispatcher()),
        pinStore,
    )

    private fun testFlow(
        block: suspend CoroutineScope.(
            ValidationSessionViewModel,
            MutableList<ValidationSessionUiState>,
        ) -> Unit,
    ) = runBlocking {
        val viewModel = viewModel()
        val states = mutableListOf<ValidationSessionUiState>()
        val job = launch(Dispatchers.Main) { viewModel.uiState.collect { states.add(it) } }
        try {
            block(viewModel, states)
        } finally {
            job.cancel()
        }
    }
}
