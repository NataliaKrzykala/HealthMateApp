package com.example.healthmate.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.healthmate.ui.theme.Typography
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.R
import com.example.healthmate.data.DataSource
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.ui.theme.HealthMateTheme

@Composable
fun StatisticsScreen(
    healthMateUiState: HealthMateUiState,
    //onCancelButtonClicked: () -> Unit,
    //onSendButtonClicked: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources

    val selectedDevice = healthMateUiState.device

    /*Text(
        text = "Selected device: ${stringResource(selectedDevice.first)}",
        modifier = modifier
    )*/

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
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
                    text = "${stringResource(selectedDevice.first)}",
                    style = Typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center)
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
            ) {
                Text(
                    text = stringResource(R.string.last_measurement),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(R.string.value),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = Typography.displayMedium
                )
                Divider(thickness = dimensionResource(R.dimen.thickness_divider))
                Text(
                    text = stringResource(R.string.date),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = Typography.displayMedium
                )
                Divider(thickness = dimensionResource(R.dimen.thickness_divider))
                Text(
                    text = stringResource(R.string.time),
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = Typography.displayMedium
                )
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

@Preview
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
}
