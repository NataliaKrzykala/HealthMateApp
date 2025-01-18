package com.example.healthmate.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.healthmate.data.HealthMateUiState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import com.example.healthmate.ble.BluetoothHandler
import com.example.healthmate.ble.BluetoothViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


@Composable
fun SettingsScreen(
    healthMateUiState: HealthMateUiState,
    bluetoothViewModel: BluetoothViewModel,
    bluetoothHandler: BluetoothHandler
) {
    val loggedUser = healthMateUiState.user
    val backupState by bluetoothViewModel.backupState.collectAsState()
    val revertBackupState by bluetoothViewModel.restoreState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Witaj, ${loggedUser.login}!")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                if (bluetoothHandler.isNetworkAvailable()) {
                    bluetoothViewModel.backupData(loggedUser)
                }
            }
        }) {
            Text(text = "Wykonaj backup")
        }

        Spacer(modifier = Modifier.height(16.dp))


        when (val state = backupState) {
            is BluetoothViewModel.BackupState.InProgress -> Text("Backup w toku...")
            is BluetoothViewModel.BackupState.Progress -> Text("Postęp: ${state.completedSteps}/${state.totalSteps}")
            is BluetoothViewModel.BackupState.Success -> Text("Sukces: ${state.message}")
            is BluetoothViewModel.BackupState.Error -> Text("Błąd: ${state.error}")
            is BluetoothViewModel.BackupState.Idle -> {}
        }

        if (backupState is BluetoothViewModel.BackupState.InProgress || backupState is BluetoothViewModel.BackupState.Progress) {
            LinearProgressIndicator(
                progress = {
                    (backupState as? BluetoothViewModel.BackupState.Progress)?.let {
                        it.completedSteps.toFloat() / it.totalSteps
                    } ?: 0f
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                if (bluetoothHandler.isNetworkAvailable()) {
                    bluetoothViewModel.restoreData(loggedUser)
                }
            }
        }) {
            Text(text = "Przywróć dane z backup'u")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val revState = revertBackupState) {
            is BluetoothViewModel.RevertBackupState.InProgress -> Text("Backup w toku...")
            is BluetoothViewModel.RevertBackupState.Progress -> Text("Postęp: ${revState.completedSteps}/${revState.totalSteps}")
            is BluetoothViewModel.RevertBackupState.Success -> Text("Sukces: ${revState.message}")
            is BluetoothViewModel.RevertBackupState.Error -> Text("Błąd: ${revState.error}")
            is BluetoothViewModel.RevertBackupState.Idle -> {}
        }

        if (revertBackupState is BluetoothViewModel.RevertBackupState.InProgress || revertBackupState is BluetoothViewModel.RevertBackupState.Progress) {
            LinearProgressIndicator(
                progress = {
                    (revertBackupState as? BluetoothViewModel.RevertBackupState.Progress)?.let {
                        it.completedSteps.toFloat() / it.totalSteps
                    } ?: 0f
                },
            )
        }

    }
}