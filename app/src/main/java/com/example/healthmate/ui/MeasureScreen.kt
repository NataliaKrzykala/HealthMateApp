package com.example.healthmate.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.healthmate.ble.BloodPressureMonitor
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothUUIDs
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ble.Thermometer
import com.example.healthmate.ble.WeightScale
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID


//@Composable
//fun MeasureScreen(
//    modifier: Modifier = Modifier,
//    bluetoothHandler: BluetoothHandler,
//    bluetoothViewModel: BluetoothViewModel,
//    healthMateUiState: HealthMateUiState
//) {
//    var showDetails by remember { mutableStateOf(false) }
//    var services by remember { mutableStateOf<List<BluetoothGattService>>(emptyList()) }
//
//    bluetoothHandler.setOnServicesDiscoveredCallback { discoveredServices ->
//        services = discoveredServices
//        showDetails = true
//    }
//
//    //bluetoothViewModel.resetSaveFlag()
//
//    if (showDetails) {
//        BluetoothDetailsScreen(
//            bluetoothHandler = bluetoothHandler,
//            bluetoothViewModel = bluetoothViewModel,
//            healthMateUiState = healthMateUiState
//        ) {
//            showDetails = false
//        }
//    } else {
//        val pairedDevices = bluetoothHandler.getBondedDevices()
//
//        val composition by rememberLottieComposition(
//            spec = LottieCompositionSpec.RawRes(R.raw.anim)
//        )
//        var isPlaying by remember {
//            mutableStateOf(true)
//        }
//        val progress by animateLottieCompositionAsState(
//            composition = composition,
//            iterations = LottieConstants.IterateForever
//        )
//
//        Column(
//            modifier = modifier,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
//            ) {
//                if (pairedDevices == null) {
//                    LottieAnimation(
//                        composition = composition,
//                        progress = {
//                            progress
//                        }
//                    )
//                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
//                    Text(
//                        stringResource(R.string.paired_list),
//                        style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
//                    )
//                } else {
//                    PairedDevicesList(pairedDevices, bluetoothHandler, bluetoothViewModel)
//                }
//            }
//        }
//    }
//}

@SuppressLint("MissingPermission")
@Composable
fun MeasureScreen(
    modifier: Modifier = Modifier,
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel,
    healthMateUiState: HealthMateUiState
) {
    bluetoothHandler.onDeviceConnectedCallback = { deviceType ->
        Log.e("Bluetooth", "Device connected (measure screen): ${deviceType.name}")
        bluetoothViewModel.setCurrentDevice(deviceType) // Przekazanie typu urządzenia do ViewModel
    }

    var showDetails by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(true) }
    var currentDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    val pairedDevices = bluetoothHandler.getBondedDevices()

    bluetoothHandler.onConnectionStateChanged = { newState ->
        bluetoothViewModel.onConnectionStateChanged(newState)
    }

    if (pairedDevices != null) {
        Log.e("Paired Devices List", "List is of size: ${pairedDevices.size}")
    }

    LaunchedEffect(Unit) {
        isConnecting = true
        Log.e("isConnecting", "Attempting to connect...")

        // Sprawdzenie uprawnień na początku
        if (!bluetoothHandler.hasBluetoothPermission()) {
            isConnecting = false
            Log.e("isConnecting", "Permission denied for Bluetooth")
            return@LaunchedEffect
        }

        val deviceList = pairedDevices?.toList()
        if (deviceList.isNullOrEmpty()) {
            Log.e("isConnecting", "No paired devices found.")
            isConnecting = false
            return@LaunchedEffect
        }

        var connected = false // Flaga do śledzenia sukcesu połączenia

        // Powtarzaj, dopóki nie połączysz się z którymś urządzeniem
        while (!connected) {
            for (device in deviceList) {
                if (connected) break // Wyjście, jeśli już połączono

                //try {
                    if (device.name.contains("A&D") || device.name.contains("nRF")) {
                        Log.e("isConnecting", "Attempting to connect to device: ${device.name}")

                        val initialConnect = bluetoothHandler.connectToGattServer(device)
                        if(!initialConnect){
                            currentDevice = device
                            connected = true
                        }
                        delay(3000) // Opóźnienie na ustabilizowanie połączenia
                        //delay(5000)
                    }
            }

            // Jeśli po przejściu przez wszystkie urządzenia nadal brak połączenia
            if (!connected) {
                Log.e("isConnecting", "Retrying connection with all devices...")
                delay(2000) // Krótkie opóźnienie przed kolejną rundą prób
            }
        }
        isConnecting = false
    }

    when {
        isConnecting -> {
            ConnectingAnimationScreen()
        }
        else -> {
            BluetoothDetailsScreen(
                bluetoothHandler = bluetoothHandler,
                bluetoothViewModel = bluetoothViewModel,
                healthMateUiState = healthMateUiState,
                onBack = { /* Obsługa powrotu */ }
            )
        }
    }
}

@Composable
fun ConnectingAnimationScreen() {
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
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}




@Composable
fun PairedDevicesList(
    pairedDevices: Set<BluetoothDevice>?,
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel
) {
    //Log.e("Bluetooth", "Setting onDeviceConnectedCallback")
    bluetoothHandler.onDeviceConnectedCallback = { deviceType ->
        Log.e("Bluetooth", "Device connected (measure screen): $deviceType")
        bluetoothViewModel.setCurrentDevice(deviceType) // Przekazanie typu urządzenia do ViewModel
    }

    // NEW
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.paired_devices),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(id = R.dimen.padding_medium)
            )
        ) {
            pairedDevices?.forEach {
                    device ->
                if (bluetoothHandler.bluetoothEnabled()) {
                    if (device.name.contains("A&D") || device.name.contains("nRF")) {
                        ShowSelectedDevice(deviceName = device.name, onDeviceClick = { bluetoothHandler.connectToGattServer(device) })
                    }
                } else {
                    bluetoothHandler.checkAndRequestBluetoothPermission()
                    // Obsługa braku uprawnień
                    Text(stringResource(R.string.bluetooth_permission_not_granted))
                }
            }
        }
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

        }
    }

}

@Composable
fun ShowSelectedDevice(
    deviceName: String?,
    onDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onDeviceClick,
        modifier = modifier.widthIn(min = 250.dp)
    ) {
        if (deviceName != null) {
            Text(
                text = deviceName, //stringResource(labelResourceId),
                style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
    Divider(thickness = dimensionResource(R.dimen.thickness_divider))
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

    //Log.e("Logged User", "Logged user ID: ${loggedUser.uzytkownikId}")

    var devName = bluetoothHandler.getConnectedDeviceName()
    var isEffectTriggered = remember { mutableStateOf(false) }

    LaunchedEffect(device) {
        //Log.e("Bluetooth", "LaunchedEffect triggered with device: $device")
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

    if (device != null && characteristicValues.isNotEmpty()) {
        device?.let { device ->
            when (device) {
                is Thermometer -> device.setName(stringResource(R.string.thermometer_name))
                is WeightScale -> device.setName(stringResource(R.string.weight_scale_name))
                is BloodPressureMonitor -> device.setName(stringResource(R.string.bpm_name))
            }
        }
        val parsedData = device?.parseData(characteristicValue)
        if (parsedData != null) {
            //Log.e("Bluetooth", "Char values: $characteristicValues")
            //Log.e("Logged User", "Logged user ID pt2: ${loggedUser.uzytkownikId}")
            bluetoothViewModel.saveDeviceAndMeasurement(loggedUser.uzytkownikId, characteristicValues, device!!, devName, parsedData,
                stringResource(R.string.unknown), stringResource(R.string.no_info),
                stringResource(R.string.thermometer_name), stringResource(R.string.weight_scale_name), stringResource(R.string.bpm_name),
                stringResource(R.string.temperature_name), stringResource(R.string.pulse_name), stringResource(R.string.time_of_measurement)
                )
        }

        //NEW
        Column(
            modifier = Modifier.fillMaxHeight()
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
                Divider(thickness = dimensionResource(R.dimen.thickness_divider), modifier = Modifier.height(0.dp))
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
        Text(stringResource(R.string.no_dev_connected))
    }
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
