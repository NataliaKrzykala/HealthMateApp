package com.example.healthmate.ui

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.healthmate.R
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ui.theme.Typography


@Composable
fun MeasureScreen(
    modifier: Modifier = Modifier,
    bluetoothHandler: BluetoothHandler
) {
    //val pairedDevices0: Set<BluetoothDevice> = mBtAdapter.getBondedDevices()
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

@Composable
fun PairedDevicesList(
    pairedDevices: Set<BluetoothDevice>?,
    bluetoothHandler: BluetoothHandler
    ) {
    //val gattCallback = bluetoothHandler.bluetoothGatt
    Column {
        Text(
            text = stringResource(R.string.paired_devices),
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold)
        )
        pairedDevices?.forEach { device ->
            // Sprawdzenie uprawnień przed wyświetleniem nazwy urządzenia
            if (bluetoothHandler.bluetoothEnabled() == true) {
                Text(
                    text = device.name ?: "Unknown Device",
                    modifier = Modifier.clickable {
                        bluetoothHandler.connectToGattServer(device)
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

/*@Preview
@Composable
fun MeasureScreenPreview(){
    val bluetoothHandler = BluetoothHandler()
    HealthMateTheme {
        MeasureScreen(bluetoothHandler = bluetoothHandler)
    }
}*/
