package uy.com.abitab.iddigitalsdk.presentation.device_association.ui

import androidx.activity.ComponentActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import uy.com.abitab.iddigitalsdk.presentation.liveness.ui.viewmodels.DeviceAssociationViewModel

class DeviceAssociationActivity : ComponentActivity() {
    private val viewModel: DeviceAssociationViewModel by viewModel()


}