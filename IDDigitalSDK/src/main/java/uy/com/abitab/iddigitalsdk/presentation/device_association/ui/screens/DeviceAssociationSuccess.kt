package uy.com.abitab.iddigitalsdk.presentation.device_association.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uy.com.abitab.iddigitalsdk.composables.components.Button
import uy.com.abitab.iddigitalsdk.composables.components.IDDigitalWatermark
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme

@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceAssociationSuccessPreview() {
    DeviceAssociationSuccess({})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAssociationSuccess(onContinue: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()


    AbitabTheme {
        Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,

                    ),
                scrollBehavior = scrollBehavior,
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = 0.dp,
                        )
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp)
            ) {

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .clip(
                            RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp
                            )
                        )
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Asociación exitosa",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                    )

                    Text(
                        "Ya podés utilizar ID Digital para validar tu identidad en esta app.",
                        modifier = Modifier.padding(bottom = 32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 48.dp)
                    ) {
                        Button(
                            text = "Continuar", onClick = onContinue

                        )
                    }
                }

                IDDigitalWatermark()

                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))

            }

        }
    }
}