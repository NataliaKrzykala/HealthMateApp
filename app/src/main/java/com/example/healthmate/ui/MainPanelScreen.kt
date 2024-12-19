package com.example.healthmate.ui

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.R
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.data.DataSource
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.data.Urzadzenie
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Typography
import kotlin.math.log

@Composable
fun MainPanelScreen(
    onStatisticsButtonClicked: (Urzadzenie) -> Unit,
    healthMateUiState: HealthMateUiState,
    onAccountButtonClicked: () -> Unit,
    onMeasureButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    bluetoothViewModel: BluetoothViewModel
) {
    //bluetoothViewModel.clearDatabase()
    //val sensors = bluetoothViewModel.allSensors.collectAsState(initial = emptyList()).value
    val loggedUser = healthMateUiState.user
    //Log.e("Logged User11111", "$loggedUser")

    LaunchedEffect(loggedUser.uzytkownikId) {
        bluetoothViewModel.loadSensorsForUser(loggedUser.uzytkownikId)
    }

    // Obserwuj stan czujników
    val sensors = bluetoothViewModel.sensorsForUser.collectAsState(initial = emptyList()).value

    bluetoothViewModel.resetLoginState()
    bluetoothViewModel.resetRegisterState()
    //bluetoothViewModel.resetSaveFlag()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
//            Box(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth()
//            ) {
//                Text(
//                    text = "Cześć ${loggedUser.imie}, twój id to ${loggedUser.uzytkownikId} !",
//                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
//                    modifier = Modifier.align(Alignment.Center)
//                )
//            }
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.your_devices),
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
            sensors.forEach { item ->
                SelectRememberedDeviceButton(
                    labelResourceId = item.nazwa,
                    onClick = { onStatisticsButtonClicked(item) }
                )
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
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onMeasureButtonClicked() }
            ) {
                Text(text = stringResource(R.string.measure))
            }
        }

    }
}

@Composable
fun SelectRememberedDeviceButton(
    labelResourceId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.widthIn(min = 250.dp)
    ) {
        Text(
            text = labelResourceId,
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),)
    }
    Divider(thickness = dimensionResource(R.dimen.thickness_divider))
}


/*@Preview
@Composable
fun MainPanelPreview() {
    HealthMateTheme {
        MainPanelScreen(
            rememberedDevices = DataSource.rememberedDevices,
            onStatisticsButtonClicked = {},
            onAccountButtonClicked = {},
            onMeasureButtonClicked = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
        )
    }
}*/