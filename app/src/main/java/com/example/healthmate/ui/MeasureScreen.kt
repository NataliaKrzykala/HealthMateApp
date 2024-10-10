package com.example.healthmate.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.example.healthmate.ui.theme.Typography
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
            services = services,
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
                if(pairedDevices == null) {
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
                }else{
                    PairedDevicesList(pairedDevices, bluetoothHandler)
                }
            }
        }
    }
}

@Composable
fun PairedDevicesList(
    pairedDevices: Set<BluetoothDevice>?,
    bluetoothHandler: BluetoothHandler
    ) {
    Column {
        Text(
            text = stringResource(R.string.paired_devices),
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold)
        )
        pairedDevices?.forEach { device ->
            // Sprawdzenie uprawnień przed wyświetleniem nazwy urządzenia
            if (bluetoothHandler.bluetoothEnabled()) {
                if (device.name == "nRF Connect" || device.name == "A&D_UT201BLEA_90F83") {
                    Text(
                        text = device.name ?: "Unknown Device",
                        modifier = Modifier.clickable {
                            bluetoothHandler.connectToGattServer(device)
                            //bluetoothHandler.connectedDevice = device
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
    services: List<BluetoothGattService>,
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel,
    onBack: () -> Unit
) {


    val characteristicValues by bluetoothViewModel.characteristicValues.collectAsState()
    val device by bluetoothViewModel.currentDevice.collectAsState()
    val characteristicValue by bluetoothViewModel.characteristicValue.collectAsState()

    LaunchedEffect(Unit) {
        val services = bluetoothHandler.getServices()
        if (services.isNotEmpty()) {
            val values = bluetoothHandler.readAllCharacteristics(services)
            bluetoothViewModel.updateCharacteristicValues(values)
            bluetoothHandler.handleDeviceActions(services, device)
        }
    }

    bluetoothHandler.onCharacteristicChangedCallback = { value ->
        bluetoothViewModel.updateCharacteristicValue(value) // Odebranie wartości indicate
    }

    bluetoothHandler.onDeviceConnectedCallback = { deviceType ->
        bluetoothViewModel.setCurrentDevice(deviceType) // Przekazanie typu urządzenia do ViewModel
    }

    var devName = bluetoothHandler.getConnectedDeviceName()

    if (device != null) {
        val parsedData = device?.parseData(characteristicValue)

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Device name: $devName",
                style = Typography.bodyMedium
            )

            device?.getDisplayData()?.forEach { key ->
                Text(
                    text = "$key: ${parsedData?.get(key) ?: "No data"}",
                    style = Typography.bodyMedium
                )
            }

            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                name = stringResource(R.string.manufacturer),
                value = characteristicValues[BluetoothUUIDs.UUID_MANUFACTURER] ?: "No data"
            )
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                name = stringResource(R.string.device_model),
                value = characteristicValues[BluetoothUUIDs.UUID_MODEL_NUMBER] ?: "No data"
            )
            CharacteristicRead(
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
                name = stringResource(R.string.battery_level),
                value = characteristicValues[BluetoothUUIDs.UUID_BATTERY_LEVEL] ?: "No data"
            )
        }

    } else {
        Text(text = "No device connected")
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
                if(value.length > 2){
                    Text(
                        text = value, //hexToString(value)
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

fun hexToString(hex: String): String {
    val output = StringBuilder("")

    // Przechodzimy przez hex parami (każde dwie cyfry reprezentują jeden znak)
    for (i in hex.indices step 2) {
        val str = hex.substring(i, i + 2)
        // Konwersja z hex do wartości liczbowej, a następnie na znak
        val char = str.toInt(16).toChar()
        output.append(char)
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
