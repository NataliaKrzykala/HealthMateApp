package com.example.healthmate

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.healthmate.ui.theme.HealthMateTheme
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.healthmate.ble.BleObserver
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.ble.HealthMateViewModelFactory
//import com.example.healthmate.data.DatabaseSyncWorker
import com.example.healthmate.data.HMApp
import java.util.concurrent.TimeUnit
import androidx.lifecycle.Observer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest

class MainActivity : ComponentActivity(

) {
    private lateinit var syncWorkRequest: PeriodicWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        //setContent {

        //WorkManager.getInstance(applicationContext).cancelAllWork()
        //scheduleSyncJob()

            val bluetoothHandler = BluetoothHandler(
                this,
                activityResultRegistry,
                onScanResult = ::btScan
            )

            val bleObserver = BleObserver(this, bluetoothHandler)
            this.lifecycle.addObserver(bleObserver)

            val healthMateViewModelFactory = HealthMateViewModelFactory(bluetoothHandler, repository = (application as HMApp).repository)
            val bluetoothViewModel: BluetoothViewModel = ViewModelProvider(this, healthMateViewModelFactory)
                .get(BluetoothViewModel::class.java)

            bluetoothHandler.checkAndRequestBluetoothPermission()

        setContent {
            HealthMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Przekazanie handlera i ViewModel do głównej aplikacji
                    HealthMateApp(
                        bluetoothHandler = bluetoothHandler,
                        bluetoothViewModel = bluetoothViewModel,
                        bleObserver = bleObserver
                    )
                }
            }
        }

        //observeSyncJobResult()
        //}
    }



    private fun btScan(){
        //Toast.makeText(this, R.string.ble_connected_succesfully, Toast.LENGTH_LONG).show()
    }

//    private fun scheduleSyncJob() {
//        // Tworzenie zaplanowanego zadania WorkManager
//        syncWorkRequest = PeriodicWorkRequestBuilder<DatabaseSyncWorker>(15, TimeUnit.MINUTES) // co 15 minut
//            .build()
//        Log.e("FirestoreError", "Errorro 1")
//        // Zarejestruj zadanie w WorkManager
//        WorkManager.getInstance(applicationContext).enqueue(syncWorkRequest)
//
////        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
////            "DatabaseSync",
////            ExistingPeriodicWorkPolicy.KEEP, // UPDATE Lub KEEP, jeśli chcesz zachować istniejące
////            syncWorkRequest
////        )
//    }
//
//    private fun observeSyncJobResult() {
//        Log.e("FirestoreError", "Errorro 2")
//        // Nasłuchujemy stanu zakończenia zaplanowanego zadania w WorkManager
//        WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(syncWorkRequest.id).observe(this, Observer { workInfo ->
//            if (workInfo != null) {
//                when (workInfo.state) {
//                    WorkInfo.State.SUCCEEDED -> {
//                        // Synchronizacja zakończona sukcesem
//                        Log.e("FirestoreError", "Errorro 3")
//                        Toast.makeText(applicationContext, "Synchronizacja zakończona sukcesem", Toast.LENGTH_SHORT).show()
//                    }
//                    WorkInfo.State.FAILED -> {
//                        // Synchronizacja nie powiodła się
//                        Log.e("FirestoreError", "Errorro 4")
//                        Toast.makeText(applicationContext, "Synchronizacja nie powiodła się", Toast.LENGTH_SHORT).show()
//                    }
//                    WorkInfo.State.RUNNING -> {
//                        Log.e("FirestoreError", "Errorro 5")
//                        // Zadanie jest w trakcie
//                        // Możesz dodać komunikat informujący użytkownika o trwającej synchronizacji
//                    }
//                    WorkInfo.State.CANCELLED -> {
//                        Log.e("FirestoreError", "Errorro 6")
//                        // Zadanie zostało anulowane
//                        Toast.makeText(applicationContext, "Synchronizacja została anulowana", Toast.LENGTH_SHORT).show()
//                    }
//                    else -> {
//                        // Inne stany
//                    }
//                }
//            }
//        })
//    }

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