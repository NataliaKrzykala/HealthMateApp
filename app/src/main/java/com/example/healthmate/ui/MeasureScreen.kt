package com.example.healthmate.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothUUIDs
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ble.Thermometer
import com.example.healthmate.ui.theme.Typography
import kotlinx.coroutines.delay
import java.util.UUID


@Composable
fun MeasureScreen(
    modifier: Modifier = Modifier,
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel
) {
    var showDetails by remember { mutableStateOf(false) }
    var services by remember { mutableStateOf<List<BluetoothGattService>>(emptyList()) }

    bluetoothHandler.setOnServicesDiscoveredCallback { discoveredServices ->
        services = discoveredServices
        showDetails = true
    }

    if (showDetails) {
        BluetoothDetailsScreen(
            bluetoothHandler = bluetoothHandler,
            bluetoothViewModel = bluetoothViewModel
        ) {
            showDetails = false
        }
    } else {
        val pairedDevices = bluetoothHandler.getBondedDevices()

        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(R.raw.anim)
        )
        var isPlaying by remember {
            mutableStateOf(true)
        }
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
            ) {
                if (pairedDevices == null) {
                    LottieAnimation(
                        composition = composition,
                        progress = {
                            progress
                        }
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
                    Text(
                        stringResource(R.string.paired_list),
                        style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    )
                } else {
                    PairedDevicesList(pairedDevices, bluetoothHandler, bluetoothViewModel)
                }
            }
        }
    }
}

@Composable
fun PairedDevicesList(
    pairedDevices: Set<BluetoothDevice>?,
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel
) {
    Log.e("Bluetooth", "Setting onDeviceConnectedCallback")
    bluetoothHandler.onDeviceConnectedCallback = { deviceType ->
        Log.e("Bluetooth", "Device connected (measure screen): ${deviceType.name}")
        bluetoothViewModel.setCurrentDevice(deviceType) // Przekazanie typu urządzenia do ViewModel
    }

    Column {
        Text(
            text = stringResource(R.string.paired_devices),
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold)
        )
        pairedDevices?.forEach { device ->
            if (bluetoothHandler.bluetoothEnabled()) {
                if (device.name.contains("A&D")) {
                    Text(
                        text = device.name ?: "Unknown Device",
                        modifier = Modifier.clickable {
                            bluetoothHandler.connectToGattServer(device)
                        }
                    )
                }
            } else {
                bluetoothHandler.checkAndRequestBluetoothPermission()
                // Obsługa braku uprawnień
                Text("Bluetooth permission not granted")
            }
        }
    }
}

@Composable
fun BluetoothDetailsScreen(
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel,
    onBack: () -> Unit
) {
    bluetoothHandler.onCharacteristicChangedCallback = { value ->
        bluetoothViewModel.updateCharacteristicValue(value) // Odebranie wartości indicate
    }

    val characteristicValues by bluetoothViewModel.characteristicValues.collectAsState()
    val device by bluetoothViewModel.currentDevice.collectAsState()
    val characteristicValue by bluetoothViewModel.characteristicValue.collectAsState()

    LaunchedEffect(device) {
        Log.e("Bluetooth", "LaunchedEffect1 triggered with device: $device")
        if (device != null) {
            Log.e("Bluetooth", "LaunchedEffect2 triggered with device: $device")
            val services = bluetoothHandler.getServices()
            if (services.isNotEmpty()) {
                val values = bluetoothHandler.readAllCharacteristics(
                    services,
                    BluetoothUUIDs.serviceAndCharacteristicUUIDs
                )
                bluetoothViewModel.updateCharacteristicValues(values)
                bluetoothHandler.handleDeviceActions(services, device)
            }
        }
    }

    var devName = bluetoothHandler.getConnectedDeviceName()
    var devTyp = "no device"
    if (device != null) {
        if (device == Thermometer()) {
            devTyp = "termometr"
        }
        val parsedData = device?.parseData(characteristicValue)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Card(
                modifier = Modifier
                    .wrapContentSize(Alignment.Center),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.dev_name,
                        devName ?: stringResource(R.string.no_data)
                    ),
                    style = Typography.displayMedium
                )

                Text(
                    text = stringResource(
                        R.string.dev_type,
                        devTyp ?: stringResource(R.string.no_data)
                    ),
                    style = Typography.bodyMedium
                )

                device?.getDisplayData()?.forEach { key ->
                    Text(
                        text = "$key: ${parsedData?.get(key) ?: stringResource(R.string.no_data)}",
                        style = Typography.bodyMedium
                    )
                }
            }

            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                name = stringResource(R.string.manufacturer),
                value = characteristicValues[BluetoothUUIDs.UUID_MANUFACTURER]
                    ?: stringResource(R.string.no_data)
            )
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                name = stringResource(R.string.device_model),
                value = characteristicValues[BluetoothUUIDs.UUID_MODEL_NUMBER]
                    ?: stringResource(R.string.no_data)
            )
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                name = stringResource(R.string.battery_level),
                value = characteristicValues[BluetoothUUIDs.UUID_BATTERY_LEVEL]
                    ?: stringResource(R.string.no_data)
            )
        }

    } else {
        //bluetoothHandler.connectToGattServer(device)
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
                        style = Typography.bodyMedium
                    )
                } else {
                    Text(
                        text = value, //.hex
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
