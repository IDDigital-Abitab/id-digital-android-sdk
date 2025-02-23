@file:OptIn(ExperimentalMaterial3Api::class)

package uy.com.abitab.iddigitalsdk.presentation.pin.ui.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.com.abitab.iddigitalsdk.composables.components.IDDigitalWatermark
import uy.com.abitab.iddigitalsdk.ui.theme.AbitabTheme
import androidx.compose.material3.Button as MaterialButton


@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, device = "id:pixel_8_pro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PinScreenPreview() {
    PinScreen({}, {}, hasError = false)
}

const val PIN_LENGTH = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(onCompleted: (pin: String) -> Unit, onBack: () -> Unit, hasError: Boolean? = false) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var pinValue by remember { mutableStateOf("") }

    fun onDigitClick(value: String) {
        if (pinValue.length < PIN_LENGTH) {
            pinValue += value
        }
    }

    fun onBackSpaceClick() {
        pinValue = pinValue.dropLast(1)
    }

    fun checkPin() {
        if (pinValue.length != PIN_LENGTH) return
        onCompleted(pinValue)
        pinValue = ""
    }

    var showError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SideEffect {
        showError = hasError == true
        if (showError) {
            scope.launch {
                delay(3000)
                showError = false
            }
        }
    }

    AbitabTheme {
        Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface

                        )
                    }
                },
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
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Introduce tu PIN",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 64.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )


                    Column(modifier = Modifier.height(20.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                        ) {
                            AnimatedVisibility(
                                visible = showError,
                                enter = fadeIn(animationSpec = tween(durationMillis = 1000)) + slideInVertically(
                                    animationSpec = tween(durationMillis = 500)
                                ),
                                exit = fadeOut(animationSpec = tween(durationMillis = 500)) + slideOutVertically(
                                    animationSpec = tween(durationMillis = 500)
                                )
                            ) {
                                Text(
                                    "Pin incorrecto, inténtalo nuevamente.",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(4) {
                            if (it > 0) Spacer(modifier = Modifier.width(16.dp))
                            val backgroundColor =
                                if (it < pinValue.length) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(backgroundColor, shape = CircleShape)
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                PinDigitButton(value = "1", onClick = { onDigitClick("1") })
                                PinDigitButton(value = "2", onClick = { onDigitClick("2") })
                                PinDigitButton(value = "3", onClick = { onDigitClick("3") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                PinDigitButton(value = "4", onClick = { onDigitClick("4") })
                                PinDigitButton(value = "5", onClick = { onDigitClick("5") })
                                PinDigitButton(value = "6", onClick = { onDigitClick("6") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                PinDigitButton(value = "7", onClick = { onDigitClick("7") })
                                PinDigitButton(value = "8", onClick = { onDigitClick("8") })
                                PinDigitButton(value = "9", onClick = { onDigitClick("9") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                MaterialButton(shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.size(75.dp),
                                    onClick = { onBackSpaceClick() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = "Borrar"
                                    )
                                }
                                PinDigitButton(value = "0", onClick = { onDigitClick("0") })
                                MaterialButton(shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    modifier = Modifier.size(75.dp),
                                    onClick = { checkPin() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
                                        contentDescription = "Borrar"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                IDDigitalWatermark()

                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}

@Composable
fun PinDigitButton(value: String, onClick: (value: String) -> Unit) {
    return MaterialButton(shape = ButtonDefaults.filledTonalShape,
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier.size(75.dp),
        onClick = {
            onClick(value)
        }) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}