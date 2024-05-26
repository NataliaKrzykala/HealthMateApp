package com.example.healthmate.ui

import android.bluetooth.BluetoothDevice
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.R
import com.example.healthmate.data.DataSource
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Typography

@Composable
fun MainPanelScreen(
    rememberedDevices: List<Pair<Int, Int>>,
    onStatisticsButtonClicked: (Pair<Int, Int>) -> Unit,
    onAccountButtonClicked: () -> Unit,
    onMeasureButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

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
            rememberedDevices.forEach { item ->
                SelectRememberedDeviceButton(
                    labelResourceId = item.first,
                    onClick = { onStatisticsButtonClicked(item) }
                )
            }
        }
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(mediumPadding),
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
    @StringRes labelResourceId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.widthIn(min = 250.dp)
    ) {
        Text(
            text = stringResource(labelResourceId),
            style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),)
    }
    Divider(thickness = dimensionResource(R.dimen.thickness_divider))
}

@Preview
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
}