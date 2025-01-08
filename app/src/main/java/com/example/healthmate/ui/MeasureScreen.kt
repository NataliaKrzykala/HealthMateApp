package com.example.healthmate.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.healthmate.R
import com.example.healthmate.ble.BleObserver
import com.example.healthmate.ble.BloodPressureMonitor
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothUUIDs
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ble.ShowEnableLocation
import com.example.healthmate.ble.ShowPermissions
import com.example.healthmate.ble.Thermometer
import com.example.healthmate.ble.WeightScale
import com.example.healthmate.ble.isLocationEnabled
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.data.permissionsList
import com.example.healthmate.ui.theme.Typography
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID


@SuppressLint("MissingPermission")
@Composable
fun MeasureScreen(
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel,
    healthMateUiState: HealthMateUiState,
    onCancel: () -> Unit
) {
    bluetoothHandler.onDeviceConnectedCallback = { deviceType ->
        Log.e("Bluetooth", "Device connected (measure screen): $deviceType")
        bluetoothViewModel.setCurrentDevice(deviceType) // Przekazanie typu urządzenia do ViewModel
    }

    val connectionState by bluetoothViewModel.connectionStateFlow.collectAsState()
    var hasEnteredDetailsScreen by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Connecting) }

    bluetoothHandler.onConnectionStateChanged = { newState ->
        bluetoothViewModel.onConnectionStateChanged(newState)
    }

    val context = LocalContext.current
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothHandler.isBluetoothEnabled()) }
    var isLocationEnabled by remember { mutableStateOf(isLocationEnabled(context)) }

    var isScanning by remember { mutableStateOf(false) }

    val manageScanning = {
        if (isBluetoothEnabled && isLocationEnabled && !isScanning) {
            bluetoothHandler.startScanning()
            isScanning = true
            Log.d("MeasureScreen", "Skanowanie rozpoczęte")
        } else if ((!isBluetoothEnabled || !isLocationEnabled) && isScanning) {
            bluetoothHandler.stopScanning()
            isScanning = false
            Log.d("MeasureScreen", "Skanowanie zatrzymane")
        }
        else {

        }
    }

    LaunchedEffect(true) {
        while (true) {
            // Sprawdzanie stanu Bluetooth i lokalizacji
            val bluetoothState = bluetoothHandler.isBluetoothEnabled()
            val locationState = isLocationEnabled(context)

            // Jeśli stan Bluetooth lub lokalizacji się zmieni, zaktualizuj stany
            if (bluetoothState != isBluetoothEnabled || locationState != isLocationEnabled) {
                isBluetoothEnabled = bluetoothState
                isLocationEnabled = locationState
                manageScanning() // Zaktualizuj skanowanie
            }

            delay(2000) // Odświeżanie co 2 sekundy
        }
    }



    LaunchedEffect(isLocationEnabled, isBluetoothEnabled) { //multiplePermissionsState.allPermissionsGranted,
        //if (multiplePermissionsState.allPermissionsGranted) {
        when {
            !isBluetoothEnabled -> {
                currentScreen = Screen.EnableBluetoothAndLocation
            }
            !isLocationEnabled -> {
                // Przekierowanie do włączenia lokalizacji
                currentScreen = Screen.EnableBluetoothAndLocation
            }
            else -> {
                // Można rozpocząć skanowanie
                bluetoothHandler.startScanning()
                currentScreen = Screen.Connecting
            }
        }
//        } else {
//            currentScreen = Screen.CheckingPermissions
//        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == BluetoothHandler.ConnectionState.CONNECTED) {
            currentScreen = Screen.Details
        } else if (connectionState == BluetoothHandler.ConnectionState.DISCONNECTED && !hasEnteredDetailsScreen) {
            currentScreen = Screen.Connecting
        }
    }

    when (currentScreen) {
//        Screen.CheckingPermissions -> {
//            ShowPermissions(multiplePermissionsState)
//        }

        Screen.Connecting -> {
            ConnectingAnimationScreen(
                bluetoothHandler = bluetoothHandler,
                onCancel = {
                    bluetoothHandler.stopScanning()
                    onCancel()
                }
            )
        }

        Screen.EnableBluetoothAndLocation -> {
            WaitingScreen()
        }

        Screen.Details -> {
            hasEnteredDetailsScreen = true
            BluetoothDetailsScreen(
                bluetoothHandler = bluetoothHandler,
                bluetoothViewModel = bluetoothViewModel,
                healthMateUiState = healthMateUiState,
                onBack = {
                    bluetoothHandler.stopScanning()
                    hasEnteredDetailsScreen = false
                    currentScreen = Screen.Connecting
                }
            )
        }
    }
}

enum class Screen {
    //CheckingPermissions,
    Connecting,
    EnableBluetoothAndLocation,
    Details
}

@Composable
fun ConnectingAnimationScreen(
    onCancel: () -> Unit,
    bluetoothHandler: BluetoothHandler,
) {

    DisposableEffect(Unit) {
        onDispose {
            bluetoothHandler.stopScanning() // Wywołaj stopScanning przy opuszczaniu strony
        }
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(composition = composition, progress = { progress })
        Text(
            text = stringResource(R.string.connecting),
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 32.dp)
        )

        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp)
                .padding(horizontal = 32.dp)

        ) {
            Text(text = stringResource(R.string.cancel))
        }
    }
}

@Composable
fun WaitingScreen(
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animloading))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(composition = composition, progress = { progress })
    }
}

@Composable
fun BluetoothDetailsScreen(
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel,
    healthMateUiState: HealthMateUiState,
    onBack: () -> Unit
) {
    bluetoothHandler.onCharacteristicChangedCallback = { value ->
        bluetoothViewModel.updateCharacteristicValue(value) // Odebranie wartości indicate
    }

    val characteristicValues by bluetoothViewModel.characteristicValues.collectAsState()
    val device by bluetoothViewModel.currentDevice.collectAsState()
    val characteristicValue by bluetoothViewModel.characteristicValue.collectAsState()
    val loggedUser = healthMateUiState.user

    // Czyszczenie characteristicValue przy wyjściu ze strony
    DisposableEffect(Unit) {
        onDispose {
            bluetoothViewModel.updateCharacteristicValue(byteArrayOf()) // Reset stanu
        }
    }

    var devName = bluetoothHandler.getConnectedDeviceName()
    devName = devName?.split("_")?.take(2)?.joinToString("_") ?: "unknown"
    var isEffectTriggered = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isEffectTriggered.value) {
            isEffectTriggered.value = true
            if (device != null) {
                val services = bluetoothHandler.getServices()
                if (services.isNotEmpty()) {
                    //bluetoothHandler.updateDateTime(services)
                    val values = bluetoothHandler.readAllCharacteristics(
                        services,
                        BluetoothUUIDs.serviceAndCharacteristicUUIDs
                    )
                    bluetoothViewModel.updateCharacteristicValues(values)
                    bluetoothHandler.handleDeviceActions(services, device)
                }
            }
        }
    }


    if (device != null && characteristicValues.isNotEmpty() && characteristicValue?.isNotEmpty() == true) {
        device?.let { device ->
            when (device) {
                is Thermometer -> device.setName(stringResource(R.string.thermometer_name))
                is WeightScale -> device.setName(stringResource(R.string.weight_scale_name))
                is BloodPressureMonitor -> device.setName(stringResource(R.string.bpm_name))
            }
        }

        val parsedData = device?.parseData(characteristicValue)
        if (parsedData != null) {
            bluetoothViewModel.saveDeviceAndMeasurement(
                loggedUser.uzytkownikId,
                characteristicValues,
                device!!,
                devName,
                parsedData,
                stringResource(R.string.unknown),
                stringResource(R.string.no_info),
                stringResource(R.string.thermometer_name),
                stringResource(R.string.weight_scale_name),
                stringResource(R.string.bpm_name),
                stringResource(R.string.temperature_name),
                stringResource(R.string.pulse_name),
                stringResource(R.string.time_of_measurement)
            )
        }

        //NEW
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            R.string.dev_name,
                            devName ?: stringResource(R.string.no_data)
                        ),
                        style = Typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(
                            R.string.dev_type,
                            device!!.name ?: stringResource(R.string.no_data)
                        ),
                        style = Typography.displayMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    //Divider(thickness = dimensionResource(R.dimen.thickness_divider))
                }
                device?.getDisplayData()?.forEach { key ->
                    Text(
                        text = "$key: ${parsedData?.get(key) ?: stringResource(R.string.no_data)}",
                        style = Typography.displayMedium
                    )
                }
                Divider(
                    thickness = dimensionResource(R.dimen.thickness_divider),
                    modifier = Modifier.height(0.dp)
                )
            }
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_Vsmall)),
                name = stringResource(R.string.manufacturer),
                value = characteristicValues[BluetoothUUIDs.UUID_MANUFACTURER]
                    ?: stringResource(R.string.no_data)
            )
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_Vsmall)),
                name = stringResource(R.string.device_model),
                value = characteristicValues[BluetoothUUIDs.UUID_MODEL_NUMBER]
                    ?: stringResource(R.string.no_data)
            )
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_Vsmall)),
                name = stringResource(R.string.battery_level),
                value = characteristicValues[BluetoothUUIDs.UUID_BATTERY_LEVEL]
                    ?: stringResource(R.string.no_data)
            )
        }

    } else {
        WaitingScreen()
    }

//    Button(onClick = onBack) {
//        Text(text = "Powrót")
//    }
}

@Composable
fun CharacteristicRead(
    name: String,
    modifier: Modifier = Modifier,
    value: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_small))
            ) {
                Text(text = name)
                Spacer(Modifier.weight(1f))
                CharacteristicReadMoreButton(
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                )
            }
            if (expanded) {
                if (value.length > 2) {
                    Text(
                        text = hexToString(value),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = Typography.bodyMedium
                    )
                } else {
                    Text(
                        text = value, //.hex
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = Typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacteristicReadMoreButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = stringResource(R.string.expand_button_content_description),
            //tint = MaterialTheme.colorScheme.secondary
        )
    }
}

fun ByteArray.toHexString(): String = joinToString(separator = " ") { byte -> "%02X".format(byte) }

//fun hexToString(hex: String): String {
//    val output = StringBuilder("")
//
//    // Przechodzimy przez hex parami (każde dwie cyfry reprezentują jeden znak)
//    for (i in hex.indices step 2) {
//        val str = hex.substring(i, i + 2)
//        // Konwersja z hex do wartości liczbowej, a następnie na znak
//        val char = str.toInt(16).toChar()
//        output.append(char)
//    }
//
//    return output.toString()
//}

fun hexToString(hex: String): String {
    val output = StringBuilder("")

    // Usuń wszystkie niepoprawne znaki (nie należące do zakresu 0-9, a-f, A-F)
    val sanitizedHex = hex.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    // Przechodzimy przez hex parami (każde dwie cyfry reprezentują jeden znak)
    for (i in sanitizedHex.indices step 2) {
        // Upewniamy się, że mamy parzystą liczbę znaków
        if (i + 2 <= sanitizedHex.length) {
            val str = sanitizedHex.substring(i, i + 2)
            // Konwersja z hex do wartości liczbowej, a następnie na znak
            val char = str.toInt(16).toChar()
            output.append(char)
        }
    }

    return output.toString()
}


/*@Preview
@Composable
fun MeasureScreenPreview(){
    val bluetoothHandler = BluetoothHandler()
    HealthMateTheme {
        MeasureScreen(bluetoothHandler = bluetoothHandler)
    }
}*/
