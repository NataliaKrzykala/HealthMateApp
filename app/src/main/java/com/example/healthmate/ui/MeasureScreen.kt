package com.example.healthmate.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
//import com.example.healthmate.ble.BluetoothDetailsScreen
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothUUIDs
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ui.theme.Typography
import java.util.Calendar
import java.util.UUID


@Composable
fun MeasureScreen(
    modifier: Modifier = Modifier,
    bluetoothHandler: BluetoothHandler,
    bluetoothViewModel: BluetoothViewModel
) {
    //val pairedDevices0: Set<BluetoothDevice> = mBtAdapter.getBondedDevices()
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
                Text(
                    text = device.name ?: "Unknown Device",
                    modifier = Modifier.clickable {
                        bluetoothHandler.connectToGattServer(device)
                        //bluetoothHandler.connectedDevice = device
                    }
                )
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

    val device by bluetoothViewModel.currentDevice.collectAsState()
    val characteristicValue by bluetoothViewModel.characteristicValue.collectAsState()

    bluetoothHandler.onCharacteristicChangedCallback = { value ->
        bluetoothViewModel.updateCharacteristicValue(value)
    }

    bluetoothHandler.onDeviceConnectedCallback = { deviceType ->
        bluetoothViewModel.setCurrentDevice(deviceType) // Przekazanie typu urządzenia do ViewModel
    }


    //val resultTemp: Float? = parseTemperatureFromByte(characteristicValue?.copyOfRange(1, 5))
    //val resultDate: Calendar? = parseTimestampFromByte(characteristicValue?.copyOfRange(5, 12))

    var receivedData by remember { mutableStateOf("") }
    bluetoothHandler.setOnCharacteristicReadCallback { value ->
        receivedData = value.toHexString()
    }

    /*var receivedDataDescriptor by remember { mutableStateOf("") }
    bluetoothHandler.setOnDescriptorReadCallback { value ->
        receivedDataDescriptor = value.toHexString()
    }*/

    //var devName = bluetoothHandler.getConnectedDeviceName()

    if (device != null) {
        val parsedData = device?.parseData(characteristicValue)

        Column(modifier = Modifier.padding(16.dp)) {
            device?.getDisplayData()?.forEach { key ->
                Text(
                    text = "$key: ${parsedData?.get(key) ?: "No data"}",
                    style = Typography.bodyMedium
                )
            }
            /*Text(
                text = "Device name: $devName",
                style = Typography.bodyMedium
            )*/

            Button(onClick = { bluetoothHandler.readCharacteristicByUUID(BluetoothUUIDs.UUID_SERVICE_DEVICE_INFO, BluetoothUUIDs.UUID_MANUFACTURER) }) {
                Text(text = stringResource(R.string.details_of_settings))
            }
            Text(
                text = "Received Data Characteristic: $receivedData",
                style = Typography.bodyMedium
            )
        }

    } else {
        Text(text = "No device connected")
    }

    /*Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ){
        Text(
            text = stringResource(R.string.temperature),
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "${resultTemp ?: "No data"}", //.toHexString() ?: "No Data" characteristicValue?.contentToString()
            style = Typography.bodyMedium
        )
        Text(
            text = "${resultDate?.time ?: "No data"}", //.toHexString() ?: "No Data" characteristicValue?.contentToString()
            style = Typography.bodyMedium
        )

       /* Button(onClick = { bluetoothHandler.readCharacteristic() }) {
            Text(text = stringResource(R.string.details_of_settings))
        }*/

        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Back")
        }
    }*/
}

fun ByteArray.toHexString(): String = joinToString(separator = " ") { byte -> "%02X".format(byte) }

/*@Preview
@Composable
fun MeasureScreenPreview(){
    val bluetoothHandler = BluetoothHandler()
    HealthMateTheme {
        MeasureScreen(bluetoothHandler = bluetoothHandler)
    }
}*/
