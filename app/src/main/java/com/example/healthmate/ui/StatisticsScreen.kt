package com.example.healthmate.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.healthmate.ui.theme.Typography
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.R
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.data.DataSource
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.data.PomiarZParametrami
import com.example.healthmate.ui.theme.HealthMateTheme

@Composable
fun StatisticsScreen(
    healthMateUiState: HealthMateUiState,
    //onCancelButtonClicked: () -> Unit,
    //onSendButtonClicked: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    bluetoothViewModel: BluetoothViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedDevice = healthMateUiState.device
    LaunchedEffect(selectedDevice) {
        bluetoothViewModel.loadLastPomiarWithParameters(selectedDevice.urzadzenieId)
    }
    val lastPomiar = bluetoothViewModel.lastPomiarWithParameters.collectAsState(initial = null).value

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
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
                    text = selectedDevice.nazwa,
                    style = Typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = selectedDevice.rodzaj,
                    style = Typography.displayMedium,
                    //modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ){
                Text(
                    text = stringResource(R.string.details_of_device),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                ShowDetailsButton(
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Divider(thickness = dimensionResource(R.dimen.thickness_divider), modifier = Modifier.height(0.dp))
                if (expanded) {
                    Text(
                        text = "Model: ${selectedDevice.model}",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = Typography.displayMedium
                    )
                    Divider(thickness = dimensionResource(R.dimen.thickness_divider))
                    Text(
                        text = "Producent: ${selectedDevice.producent}",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = Typography.displayMedium
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.last_measurement),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                if (lastPomiar != null) {
                    Text(
                        text = stringResource(R.string.date, lastPomiar.pomiar.data),
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = Typography.displayMedium
                    )
                    Divider(thickness = dimensionResource(R.dimen.thickness_divider))
                    DisplayMeasParams(lastPomiar)
                    } else {
                        Text("Brak danych dla ostatniego pomiaru")
                    }

            }

        }

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
                    text = stringResource(R.string.history_of_measurements),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ShowDetailsButton(
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

@Composable
fun DisplayMeasParams(lastPomiar: PomiarZParametrami) {

        lastPomiar.parametry.forEach { parametr ->
            Divider(thickness = dimensionResource(R.dimen.thickness_divider))
            Text(
                text ="Nazwa: ${parametr.nazwa}",
                modifier = Modifier.padding(vertical = 8.dp),
                style = Typography.displayMedium)
            //Divider(thickness = dimensionResource(R.dimen.thickness_divider))
            Text(
                text = "Wartość: ${parametr.wartosc} ${parametr.jednostka}",
                modifier = Modifier.padding(vertical = 8.dp),
                style = Typography.displayMedium
            )
        }

}
/*@Preview
@Composable
fun StatisticsScreenPreview() {
    HealthMateTheme {
        StatisticsScreen(
            healthMateUiState = HealthMateUiState(device = Pair(0, 0)),
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
        )
    }
}*/
