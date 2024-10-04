package com.example.healthmate

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthmate.ui.theme.HealthMateTheme
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ble.BluetoothViewModelFactory
import com.example.healthmate.ui.MeasureScreen

class MainActivity : ComponentActivity() { /*ComponentActivity*/
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {

            val bluetoothHandler = BluetoothHandler(
                this,
                activityResultRegistry,
                onScanResult = ::btScan
            )

            val bluetoothViewModelFactory = BluetoothViewModelFactory(bluetoothHandler)
            val bluetoothViewModel: BluetoothViewModel = ViewModelProvider(this, bluetoothViewModelFactory)
                .get(BluetoothViewModel::class.java)

            bluetoothHandler.checkAndRequestBluetoothPermission()

            HealthMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ){
                    HealthMateApp(bluetoothHandler = bluetoothHandler, bluetoothViewModel = bluetoothViewModel)
                    //MeasureScreen(bluetoothHandler = bluetoothHandler)
                }
            }
        }
    }

    private fun btScan(){
        //Toast.makeText(this, R.string.ble_connected_succesfully, Toast.LENGTH_LONG).show()
    }
}

/**
 * Composable that displays what the UI of the app looks like in light theme in the design tab.
 */
/*@Preview
@Composable
fun HealthMatePreview() {
    HealthMateTheme(darkTheme = false) {
        HealthMateApp(bluetoothHandler = bluetoothHandler)
    }
}

/**
 * Composable that displays what the UI of the app looks like in dark theme in the design tab.
 */
@Preview
@Composable
fun HealthMateDarkThemePreview() {
    HealthMateTheme(darkTheme = true) {
        HealthMateApp(bluetoothHandler = bluetoothHandler)
    }
}*/