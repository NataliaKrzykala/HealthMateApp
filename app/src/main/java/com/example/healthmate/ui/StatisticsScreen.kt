package com.example.healthmate.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.healthmate.R
import com.example.healthmate.data.HealthMateUiState

@Composable
fun StatisticsScreen(
    healthMateUiState: HealthMateUiState,
    //onCancelButtonClicked: () -> Unit,
    //onSendButtonClicked: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources

    val selectedDevice = healthMateUiState.device

    Text(
        text = "Selected device: $selectedDevice",
        modifier = modifier
    )
}