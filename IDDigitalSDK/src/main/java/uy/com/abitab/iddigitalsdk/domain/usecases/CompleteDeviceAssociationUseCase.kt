package uy.com.abitab.iddigitalsdk.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uy.com.abitab.iddigitalsdk.domain.models.DeviceAssociation
import uy.com.abitab.iddigitalsdk.domain.repositories.ValidationSessionRepository

class CompleteDeviceAssociationUseCase(
    private val validationSessionRepository: ValidationSessionRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(id: String): DeviceAssociation = withContext(dispatcher) {
        val data = validationSessionRepository.completeDeviceAssociation(id)

        return@withContext data
    }
}