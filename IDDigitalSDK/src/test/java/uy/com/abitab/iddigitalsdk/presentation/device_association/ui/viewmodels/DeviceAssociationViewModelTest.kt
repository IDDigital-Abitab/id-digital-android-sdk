package uy.com.abitab.iddigitalsdk.presentation.device_association.ui.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import uy.com.abitab.iddigitalsdk.data.PinDataStoreManager
import uy.com.abitab.iddigitalsdk.domain.usecases.CompleteDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.CreateDeviceAssociationUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecuteLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ExecutePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidateLivenessChallengeUseCase
import uy.com.abitab.iddigitalsdk.domain.usecases.ValidatePinChallengeUseCase
import uy.com.abitab.iddigitalsdk.support.FakeLivenessRepository
import uy.com.abitab.iddigitalsdk.support.FakePinRepository
import uy.com.abitab.iddigitalsdk.support.FakeValidationSessionRepository
import uy.com.abitab.iddigitalsdk.support.deviceAssociation
import uy.com.abitab.iddigitalsdk.support.idleMainUntil
import uy.com.abitab.iddigitalsdk.support.pinChallenge
import uy.com.abitab.iddigitalsdk.support.validationSession
import uy.com.abitab.iddigitalsdk.utils.ChallengeValidationError
import uy.com.abitab.iddigitalsdk.utils.TransactionNotFoundError
import uy.com.abitab.iddigitalsdk.utils.UnknownError

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class DeviceAssociationViewModelTest {

    private val sessions = FakeValidationSessionRepository()
    private val pins = FakePinRepository()
    private val liveness = FakeLivenessRepository()
    private val pinStore: PinDataStoreManager = mockk(relaxed = true)
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun associate_returnsIdTokenWhenPinChallengeSucceeds() = testFlow { viewModel, states ->
        // Given a transaction whose only challenge is PIN and the backend will issue an idToken
        sessions.completedAssociation = deviceAssociation(idToken = "header.payload.sig")
        viewModel.setTransactionId("tx-1")
        viewModel.startDeviceAssociation()
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is DeviceAssociationUiState.LaunchChallenge }
        }

        // When the citizen completes the PIN
        viewModel.validateChallenge("1234")
        idleMainUntil({ "Association did not finish. states=$states" }) {
            states.any { it is DeviceAssociationUiState.Success }
        }

        // Then the app receives the idToken and the validation session id
        val success = states.filterIsInstance<DeviceAssociationUiState.Success>().single()
        assertEquals("header.payload.sig", success.idToken)
        assertEquals("session-1", success.validationSessionId)
        assertEquals("tx-1", sessions.lastTransactionId)
    }

    @Test
    fun associate_returnsEmptyIdTokenWhenBackendOmitsIt() = testFlow { viewModel, states ->
        // Given the backend completes the association without an idToken
        sessions.completedAssociation = deviceAssociation(idToken = null)
        viewModel.setTransactionId("tx-1")
        viewModel.startDeviceAssociation()
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is DeviceAssociationUiState.LaunchChallenge }
        }

        // When the citizen completes the PIN
        viewModel.validateChallenge("1234")
        idleMainUntil({ "Association did not finish. states=$states" }) {
            states.any { it is DeviceAssociationUiState.Success }
        }

        // Then the app gets an empty token and can still close the transaction
        val success = states.filterIsInstance<DeviceAssociationUiState.Success>().single()
        assertEquals("", success.idToken)
        assertEquals("session-1", success.validationSessionId)
    }

    @Test
    fun associate_executesPinChallengeWithoutReturningASession() = testFlow { viewModel, states ->
        // Given association is waiting on the PIN challenge
        viewModel.setTransactionId("tx-1")
        viewModel.startDeviceAssociation()
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is DeviceAssociationUiState.LaunchChallenge }
        }

        // When the SDK executes that challenge
        val result = viewModel.executeChallenge()

        // Then PIN does not produce a liveness session id
        assertNull(result)
        assertEquals("ch-1", pins.executedChallengeId)
    }

    @Test
    fun associate_failsAfterThreeInvalidPins() = testFlow { viewModel, states ->
        // Given every PIN submission is rejected
        pins.onValidate = { _, _ -> throw IllegalStateException("invalid pin") }
        viewModel.setTransactionId("tx-1")
        viewModel.startDeviceAssociation()
        idleMainUntil({ "No challenge was presented. states=$states" }) {
            states.any { it is DeviceAssociationUiState.LaunchChallenge }
        }

        // When the citizen fails the challenge three times
        repeat(3) { viewModel.validateChallenge("0000") }
        idleMainUntil({ "Challenge was not exhausted. states=$states" }) {
            states.any { it is DeviceAssociationUiState.Error }
        }

        // Then the flow stops with a challenge validation error
        val error = states.filterIsInstance<DeviceAssociationUiState.Error>().last().error
        assertTrue(error is ChallengeValidationError)
    }

    @Test
    fun associate_reportsUnknownChallengeType() = testFlow { viewModel, states ->
        // Given the backend sends a challenge the SDK does not implement
        sessions.associationSession = validationSession(
            challenges = listOf(pinChallenge().copy(type = "sms")),
        )
        viewModel.setTransactionId("tx-1")

        // When association starts
        viewModel.startDeviceAssociation()
        idleMainUntil({ "Unknown challenge was not reported. states=$states" }) {
            states.any { it is DeviceAssociationUiState.Error }
        }

        // Then the flow fails before asking the citizen for input
        val error = states.filterIsInstance<DeviceAssociationUiState.Error>().single().error
        assertTrue(error is UnknownError)
        assertEquals("Invalid challenge type", error.message)
    }

    @Test
    fun associate_reportsMissingTransaction() = testFlow { viewModel, states ->
        // Given the transaction id does not exist
        sessions.failure = TransactionNotFoundError()
        viewModel.setTransactionId("missing")

        // When association starts
        viewModel.startDeviceAssociation()
        idleMainUntil({ "Missing transaction was not reported. states=$states" }) {
            states.any { it is DeviceAssociationUiState.Error }
        }

        // Then the app receives TransactionNotFoundError
        val error = states.filterIsInstance<DeviceAssociationUiState.Error>().single().error
        assertTrue(error is TransactionNotFoundError)
    }

    private fun viewModel() = DeviceAssociationViewModel(
        application,
        CreateDeviceAssociationUseCase(sessions, UnconfinedTestDispatcher()),
        CompleteDeviceAssociationUseCase(sessions, UnconfinedTestDispatcher()),
        ExecutePinChallengeUseCase(pins),
        ExecuteLivenessChallengeUseCase(liveness),
        ValidateLivenessChallengeUseCase(liveness),
        ValidatePinChallengeUseCase(pins, UnconfinedTestDispatcher()),
        pinStore,
    )

    private fun testFlow(
        block: suspend CoroutineScope.(
            DeviceAssociationViewModel,
            MutableList<DeviceAssociationUiState>,
        ) -> Unit,
    ) = runBlocking {
        val viewModel = viewModel()
        val states = mutableListOf<DeviceAssociationUiState>()
        val job = launch(Dispatchers.Main) { viewModel.uiState.collect { states.add(it) } }
        try {
            block(viewModel, states)
        } finally {
            job.cancel()
        }
    }
}
