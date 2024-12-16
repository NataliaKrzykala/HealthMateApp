package com.example.healthmate.ui

import android.graphics.PointF
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.healthmate.ui.theme.Typography
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.healthmate.R
import com.example.healthmate.ble.BluetoothViewModel
import com.example.healthmate.data.DataSource
import com.example.healthmate.data.HealthMateUiState
import com.example.healthmate.data.PomiarZParametrami
import com.example.healthmate.ui.theme.HealthMateTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.auto
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
//import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
//import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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
    val lastPomiar =
        bluetoothViewModel.lastPomiarWithParameters.collectAsState(initial = null).value

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
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
            ) {
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
                Divider(
                    thickness = dimensionResource(R.dimen.thickness_divider),
                    modifier = Modifier.height(0.dp)
                )
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
                    val formattedDate = convertIsoToCustomFormat(lastPomiar.pomiar.data) ?: "Nieznana data"
                    Text(
                        text = stringResource(R.string.date, formattedDate),
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
                .padding(8.dp)
                .fillMaxWidth(),
                //.heightIn(min = 300.dp)
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
                Text(
                    text = stringResource(R.string.history_of_measurements),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 4.dp),
                )

                if (selectedDevice.rodzaj == "termometr") {
                    TemperatureChartScreen(
                        viewModel = bluetoothViewModel,
                        urzadzenieId = selectedDevice.urzadzenieId,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else if (selectedDevice.rodzaj == "waga") {
                    WeightScaleChartScreen(
                        viewModel = bluetoothViewModel,
                        urzadzenieId = selectedDevice.urzadzenieId,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else if (selectedDevice.rodzaj == "ciśnieniomierz") {
                    BPMChartScreen(
                        viewModel = bluetoothViewModel,
                        urzadzenieId = selectedDevice.urzadzenieId,
                        modifier = Modifier.padding(vertical = 8.dp),
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
            text = "Nazwa: ${parametr.nazwa}",
            modifier = Modifier.padding(vertical = 8.dp),
            style = Typography.displayMedium
        )
        //Divider(thickness = dimensionResource(R.dimen.thickness_divider))
        Text(
            text = "Wartość: ${parametr.wartosc} ${parametr.jednostka}",
            modifier = Modifier.padding(vertical = 8.dp),
            style = Typography.displayMedium
        )
    }

}

//region temperature YCHARTS
@Composable
fun TemperatureChartScreen(viewModel: BluetoothViewModel, urzadzenieId: Long, modifier: Modifier) {
    // Uruchamiamy ładowanie danych
    LaunchedEffect(urzadzenieId) {
        viewModel.loadAllPomiaryWithParameters(urzadzenieId)
    }

    // Pobieramy dane i stan ładowania
    val pomiary by viewModel.allPomiaryWithParameters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    when {
        isLoading -> {
            // Wyświetlamy stan ładowania
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Ładowanie danych...", style = MaterialTheme.typography.bodyLarge)
            }
        }
        pomiary.isEmpty() -> {
            // Wyświetlamy informację o braku danych
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Brak danych do wyświetlenia.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        else -> {
            val (pointsData, timestamps) = transformTempMeasToChartData(pomiary)
            val steps = 10

            val xAxisData = AxisData.Builder()
                .axisStepSize(150.dp)
                .axisLabelAngle(8f)
                .bottomPadding(120.dp)
                .backgroundColor(Color.Transparent)
                .steps(pointsData.size - 1)
                .labelData { i ->
                    val timestamp = timestamps.getOrNull(i) ?: ""
                    timestamp
                }
                .labelAndAxisLinePadding(5.dp)
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .shouldDrawAxisLineTillEnd(true)
                .build()

            val yAxisData = AxisData.Builder()
                .steps(steps)
                .backgroundColor(Color.Transparent)
                .labelAndAxisLinePadding(20.dp)
                .labelData { i ->
                    val stepSize = (43f - 33f) / steps // Rozmiar kroku
                    val value = 33f + i * stepSize
                    String.format("%.1f °C", value) // Formatowanie z jednym miejscem po przecinku
                }
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .shouldDrawAxisLineTillEnd(true)
                .bottomPadding(20.dp)
                .build()

            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                lineType = LineType.SmoothCurve(isDotted = false)
                            ),
                            IntersectionPoint(
                                color = MaterialTheme.colorScheme.tertiary,
                            ),
                            SelectionHighlightPoint(color = MaterialTheme.colorScheme.primary),
                            ShadowUnderLine(
                                alpha = 0.5f,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.inversePrimary,
                                        Color.Transparent
                                    )
                                )
                            ),
                            SelectionHighlightPopUp(
                                popUpLabel = { _, y ->
                                    "${String.format("%.1f", y)} °C"
                                }
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(color = MaterialTheme.colorScheme.outline),
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomPadding = 30.dp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 1000.dp)
                    .padding(bottom = 24.dp)
            ) {
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    lineChartData = lineChartData
                )
            }
        }
    }
}

fun transformTempMeasToChartData(pomiary: List<PomiarZParametrami>): Pair<List<Point>, List<String>> {
    val pointsData = mutableListOf<Point>()
    val timestamps = mutableListOf<String>()

    pomiary.forEach { pomiarZParametrami ->
        val timestamp = convertIsoToCustomFormat(pomiarZParametrami.pomiar.data)

        pomiarZParametrami.parametry
            .filter { it.nazwa == "temperatura" }
            .forEach { parametr ->
                pointsData.add(Point(pointsData.size.toFloat(), parametr.wartosc))
                if (timestamp != null) {
                    timestamps.add(timestamp)
                }
            }
    }

    val minGhostPoint = Point(-1f, 33f) // Punkt "widmo" z minimalną temperaturą
    val maxGhostPoint = Point(pointsData.size.toFloat(), 43f) // Punkt "widmo" z maksymalną temperaturą
    pointsData.add(0, minGhostPoint) // Dodanie na początek listy
    pointsData.add(maxGhostPoint)    // Dodanie na koniec listy

    timestamps.add(0, "") // Pusta etykieta dla punktu "widmo"
    timestamps.add("")    // Pusta etykieta dla drugiego punktu "widmo"

    return Pair(pointsData, timestamps)
}
//endregion

//region weight scale YCHARTS
@Composable
fun WeightScaleChartScreen(viewModel: BluetoothViewModel, urzadzenieId: Long, modifier: Modifier) {
    // Uruchamiamy ładowanie danych
    LaunchedEffect(urzadzenieId) {
        viewModel.loadAllPomiaryWithParameters(urzadzenieId)
    }

    // Pobieramy dane i stan ładowania
    val pomiary by viewModel.allPomiaryWithParameters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    when {
        isLoading -> {
            // Wyświetlamy stan ładowania
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Ładowanie danych...", style = MaterialTheme.typography.bodyLarge)
            }
        }
        pomiary.isEmpty() -> {
            // Wyświetlamy informację o braku danych
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Brak danych do wyświetlenia.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        else -> {
            val (pointsData, timestamps) = transformWeightMeasToChartData(pomiary)
            val minWeight = pointsData.minOfOrNull { it.y } ?: 0f
            val maxWeight = pointsData.maxOfOrNull { it.y } ?: 0f
            val steps = 5

            val xAxisData = AxisData.Builder()
                .axisStepSize(150.dp)
                .axisLabelAngle(8f)
                .bottomPadding(120.dp)
                .backgroundColor(Color.Transparent)
                .steps(pointsData.size - 1)
                .labelData { i ->
                    val timestamp = timestamps.getOrNull(i) ?: ""
                    timestamp
                }
                .labelAndAxisLinePadding(5.dp)
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .shouldDrawAxisLineTillEnd(true)
                .build()

            val yAxisData = AxisData.Builder()
                .steps(steps)
                .backgroundColor(Color.Transparent)
                .labelAndAxisLinePadding(20.dp)
                .labelData { i ->
                    // Rozmiar kroku w zależności od min i max
                    val stepSize = (maxWeight - minWeight) / steps // Krok na osi Y
                    val value = minWeight + i * stepSize // Wyliczanie wartości dla i-tego kroku na osi Y

                    // Formatowanie wartości, dodanie jednostki "kg"
                    String.format("%.1f kg", value)
                }
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .shouldDrawAxisLineTillEnd(true)
                .bottomPadding(20.dp)
                .build()

            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                lineType = LineType.SmoothCurve(isDotted = false)
                            ),
                            IntersectionPoint(
                                color = MaterialTheme.colorScheme.tertiary,
                            ),
                            SelectionHighlightPoint(color = MaterialTheme.colorScheme.primary),
                            ShadowUnderLine(
                                alpha = 0.5f,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.inversePrimary,
                                        Color.Transparent
                                    )
                                )
                            ),
                            SelectionHighlightPopUp(
                                popUpLabel = { _, y ->
                                    "${String.format("%.1f", y)} kg"
                                }
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(color = MaterialTheme.colorScheme.outline),
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomPadding = 30.dp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 1000.dp)
                    .padding(bottom = 24.dp)
            ) {
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    lineChartData = lineChartData
                )
            }
        }
    }
}

fun transformWeightMeasToChartData(pomiary: List<PomiarZParametrami>): Pair<List<Point>, List<String>> {
    val pointsData = mutableListOf<Point>()
    val timestamps = mutableListOf<String>()

    pomiary.forEach { pomiarZParametrami ->
        val timestamp = convertIsoToCustomFormat(pomiarZParametrami.pomiar.data)

        pomiarZParametrami.parametry
            .filter { it.nazwa == "waga" }
            .forEach { parametr ->
                pointsData.add(Point(pointsData.size.toFloat(), parametr.wartosc))
                if (timestamp != null) {
                    timestamps.add(timestamp)
                }
            }
    }

    // Znalezienie pierwszej i ostatniej wagi z punktów danych
    val firstWeight = pointsData.firstOrNull()?.y ?: 0f
    val lastWeight = pointsData.lastOrNull()?.y ?: 0f

    // Utworzenie punktów widmo
    val minGhostPoint = Point(-1f, firstWeight)
    val maxGhostPoint = Point(pointsData.size.toFloat(), lastWeight)

    // Dodanie punktów widmo do listy
    pointsData.add(0, minGhostPoint) // Dodanie na początek listy
    pointsData.add(maxGhostPoint)    // Dodanie na koniec listy

    timestamps.add(0, "") // Pusta etykieta dla punktu "widmo"
    timestamps.add("")    // Pusta etykieta dla drugiego punktu "widmo"

    return Pair(pointsData, timestamps)
}
//endregion

//region bpm YCHARTS
@Composable
fun BPMChartScreen(viewModel: BluetoothViewModel, urzadzenieId: Long, modifier: Modifier) {
    // Uruchamiamy ładowanie danych
    LaunchedEffect(urzadzenieId) {
        viewModel.loadAllPomiaryWithParameters(urzadzenieId)
    }

    // Pobieramy dane i stan ładowania
    val pomiary by viewModel.allPomiaryWithParameters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    when {
        isLoading -> {
            // Wyświetlamy stan ładowania
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Ładowanie danych...", style = MaterialTheme.typography.bodyLarge)
            }
        }
        pomiary.isEmpty() -> {
            // Wyświetlamy informację o braku danych
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Brak danych do wyświetlenia.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        else -> {

            val (allPointsData, timestamps) = transformBPMMeasToChartData(pomiary)

            // Rozdzielanie list punktów (każda lista zawiera punkty dla jednego parametru)
            val pointsDataSYS = allPointsData[0] // Punkty dla SYS
            val pointsDataDIA = allPointsData[1] // Punkty dla DIA
            val pointsDataMAP = allPointsData[2] // Punkty dla MAP
            val pointsDataPulse = allPointsData[3] // Punkty dla Pulse

            // Wersja ze zmiennym zakresem
//            val minSYS = pointsDataSYS.minOfOrNull { it.y } ?: 0f
//            val maxSYS = pointsDataSYS.maxOfOrNull { it.y } ?: 0f
//            val minDIA = pointsDataDIA.minOfOrNull { it.y } ?: 0f
//            val maxDIA = pointsDataDIA.maxOfOrNull { it.y } ?: 0f
//            val minMAP = pointsDataMAP.minOfOrNull { it.y } ?: 0f
//            val maxMAP = pointsDataMAP.maxOfOrNull { it.y } ?: 0f
//            val minPulse = pointsDataPulse.minOfOrNull { it.y } ?: 0f
//            val maxPulse = pointsDataPulse.maxOfOrNull { it.y } ?: 0f

            val steps = 13

            val xAxisData = AxisData.Builder()
                .axisStepSize(150.dp)
                .axisLabelAngle(8f)
                .bottomPadding(120.dp)
                .backgroundColor(Color.Transparent)
                .steps(pointsDataSYS.size - 1)
                .labelData { i ->
                    val timestamp = timestamps.getOrNull(i) ?: ""
                    timestamp
                }
                .labelAndAxisLinePadding(5.dp)
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .shouldDrawAxisLineTillEnd(true)
                .build()

            val yAxisData = AxisData.Builder()
                .steps(steps)
                .backgroundColor(Color.Transparent)
                .labelAndAxisLinePadding(20.dp)
                .labelData { i ->
                    val stepSize = (299f - 0f) / steps // Rozmiar kroku
                    val value = 0f + i * stepSize
                    String.format("%.0f mmHg", value)
                }
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .shouldDrawAxisLineTillEnd(true)
                .bottomPadding(20.dp)
                .build()

            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsDataSYS,
                            LineStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                lineType = LineType.SmoothCurve(isDotted = false)
                            ),
                            IntersectionPoint(
                                color = MaterialTheme.colorScheme.tertiary,
                            ),
                            SelectionHighlightPoint(color = MaterialTheme.colorScheme.primary),
                            ShadowUnderLine(
                                alpha = 0.5f,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.inversePrimary,
                                        Color.Transparent
                                    )
                                )
                            ),
                            SelectionHighlightPopUp(
                                popUpLabel = { _, y ->
                                    "${String.format("%.0f", y)} mmHg"
                                }
                            )
                        ),
                        Line(
                            dataPoints = pointsDataDIA,
                            LineStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                lineType = LineType.SmoothCurve(isDotted = false)
                            ),
                            IntersectionPoint(
                                color = MaterialTheme.colorScheme.tertiary,
                            ),
                            SelectionHighlightPoint(color = MaterialTheme.colorScheme.primary),
                            ShadowUnderLine(
                                alpha = 0.5f,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.inversePrimary,
                                        Color.Transparent
                                    )
                                )
                            ),
                            SelectionHighlightPopUp(
                                popUpLabel = { _, y ->
                                    "${String.format("%.0f", y)} mmHg"
                                }
                            )
                        ),
                        Line(
                            dataPoints = pointsDataMAP,
                            LineStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                lineType = LineType.SmoothCurve(isDotted = false)
                            ),
                            IntersectionPoint(
                                color = MaterialTheme.colorScheme.tertiary,
                            ),
                            SelectionHighlightPoint(color = MaterialTheme.colorScheme.primary),
                            ShadowUnderLine(
                                alpha = 0.5f,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.inversePrimary,
                                        Color.Transparent
                                    )
                                )
                            ),
                            SelectionHighlightPopUp(
                                popUpLabel = { _, y ->
                                    "${String.format("%.0f", y)} mmHg"
                                }
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(color = MaterialTheme.colorScheme.outline),
                backgroundColor = MaterialTheme.colorScheme.surface,
                bottomPadding = 30.dp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 1000.dp)
                    .padding(bottom = 24.dp)
            ) {
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    lineChartData = lineChartData
                )
            }
        }
    }
}

fun transformBPMMeasToChartData(pomiary: List<PomiarZParametrami>): Pair<List<List<Point>>, List<String>> {
    val pointsDataSYS = mutableListOf<Point>()
    val pointsDataDIA = mutableListOf<Point>()
    val pointsDataMAP = mutableListOf<Point>()
    val pointsDataPulse = mutableListOf<Point>()
    val timestamps = mutableListOf<String>()

    pomiary.forEach { pomiarZParametrami ->
        val timestamp = convertIsoToCustomFormat(pomiarZParametrami.pomiar.data)
        if (timestamp != null) {
            timestamps.add(timestamp)
        }

        pomiarZParametrami.parametry.forEach { parametr ->
            // Filtr dla "SYS"
            if (parametr.nazwa == "SYS") {
                pointsDataSYS.add(Point(pointsDataSYS.size.toFloat(), parametr.wartosc))
            }
            // Filtr dla "DIA"
            if (parametr.nazwa == "DIA") {
                pointsDataDIA.add(Point(pointsDataDIA.size.toFloat(), parametr.wartosc))
            }
            // Filtr dla "MAP"
            if (parametr.nazwa == "MAP") {
                pointsDataMAP.add(Point(pointsDataMAP.size.toFloat(), parametr.wartosc))
            }
            // Filtr dla "Pulse"
            if (parametr.nazwa == "puls") {
                pointsDataPulse.add(Point(pointsDataPulse.size.toFloat(), parametr.wartosc))
            }
        }
    }

    // Wersja z dynamicznym dostosowaniem osi Y
//    // Znalezienie pierwszej i ostatniej SYS z punktów danych
//    val firstSYS = pointsDataSYS.firstOrNull()?.y ?: 0f
//    val lastSYS = pointsDataSYS.lastOrNull()?.y ?: 0f
//    // Znalezienie pierwszej i ostatniej DIA z punktów danych
//    val firstDIA = pointsDataDIA.firstOrNull()?.y ?: 0f
//    val lastDIA = pointsDataDIA.lastOrNull()?.y ?: 0f
//    // Znalezienie pierwszej i ostatniej MAP z punktów danych
//    val firstMAP = pointsDataMAP.firstOrNull()?.y ?: 0f
//    val lastMAP = pointsDataMAP.lastOrNull()?.y ?: 0f
//    // Znalezienie pierwszej i ostatniej Pulse z punktów danych
//    val firstPulse = pointsDataPulse.firstOrNull()?.y ?: 0f
//    val lastPulse = pointsDataPulse.lastOrNull()?.y ?: 0f
//
//    // Utworzenie punktów widmo
//    val minGhostPointSYS = Point(-1f, firstSYS)
//    val maxGhostPointSYS = Point(pointsDataSYS.size.toFloat(), lastSYS)
//    val minGhostPointDIA = Point(-1f, firstDIA)
//    val maxGhostPointDIA = Point(pointsDataDIA.size.toFloat(), lastDIA)
//    val minGhostPointMAP = Point(-1f, firstMAP)
//    val maxGhostPointMAP = Point(pointsDataMAP.size.toFloat(), lastMAP)
//    val minGhostPointPulse = Point(-1f, firstPulse)
//    val maxGhostPointPulse = Point(pointsDataPulse.size.toFloat(), lastPulse)
//
//    // Dodanie punktów widmo do list
//    pointsDataSYS.add(0, minGhostPointSYS)
//    pointsDataSYS.add(maxGhostPointSYS)
//    pointsDataDIA.add(0, minGhostPointDIA)
//    pointsDataDIA.add(maxGhostPointDIA)
//    pointsDataMAP.add(0, minGhostPointMAP)
//    pointsDataMAP.add(maxGhostPointMAP)
//    pointsDataPulse.add(0, minGhostPointPulse)
//    pointsDataPulse.add(maxGhostPointPulse)

    // Wersja ze stałym zakresem
    val minGhostPoint = Point(-1f, 0f) // Punkt "widmo" minimalny
    val maxGhostPoint = Point(pointsDataSYS.size.toFloat(), 299f) // Punkt "widmo" maksymalny
    val minGhostPointPulse = Point(-1f, 40f)
    val maxGhostPointPulse = Point(pointsDataPulse.size.toFloat(), 180f)

    pointsDataSYS.add(0, minGhostPoint) // Dodanie na początek listy
    pointsDataSYS.add(maxGhostPoint)    // Dodanie na koniec listy
    pointsDataDIA.add(0, minGhostPoint)
    pointsDataDIA.add(maxGhostPoint)
    pointsDataMAP.add(0, minGhostPoint)
    pointsDataMAP.add(maxGhostPoint)
    pointsDataPulse.add(0, minGhostPointPulse)
    pointsDataPulse.add(maxGhostPointPulse)

    timestamps.add(0, "") // Pusta etykieta dla punktu "widmo"
    timestamps.add("")    // Pusta etykieta dla drugiego punktu "widmo"

    val allPointsData = listOf(pointsDataSYS, pointsDataDIA, pointsDataMAP, pointsDataPulse)

    return Pair(allPointsData, timestamps)
}
//endregion

fun convertIsoToCustomFormat(isoDate: String): String? {
    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME // ISO 8601
    val customFormatter = DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy") // Pożądany format

    // Parsowanie daty ISO i konwersja na wymagany format
    return try {
        val dateTime = LocalDateTime.parse(isoDate, isoFormatter)
        dateTime.format(customFormatter)
    } catch (e: Exception) {
        null // W przypadku niepoprawnego formatu
    }
}

