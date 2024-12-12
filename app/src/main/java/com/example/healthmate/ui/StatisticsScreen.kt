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
                Text(
                    text = stringResource(R.string.history_of_measurements),
                    style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                )

                if (selectedDevice.rodzaj == "termometr") {
                    TemperatureChartScreen(
                        viewModel = bluetoothViewModel,
                        urzadzenieId = selectedDevice.urzadzenieId
                    )
                } else if (selectedDevice.rodzaj == "waga") {

                } else {

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


// VICO

//@Composable
//fun TemperatureChartScreen(viewModel: BluetoothViewModel, urzadzenieId: Long) {
//    // Uruchamiamy ładowanie danych
//    LaunchedEffect(urzadzenieId) {
//        viewModel.loadAllPomiaryWithParameters(urzadzenieId)
//    }
//
//    // Pobieramy dane i stan ładowania
//    val pomiary by viewModel.allPomiaryWithParameters.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//
//    when {
//        isLoading -> {
//            // Wyświetlamy stan ładowania
//            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Text(text = "Ładowanie danych...", style = MaterialTheme.typography.bodyLarge)
//            }
//        }
//        pomiary.isEmpty() -> {
//            // Wyświetlamy informację o braku danych
//            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Text(text = "Brak danych do wyświetlenia.", style = MaterialTheme.typography.bodyLarge)
//            }
//        }
//        else -> {
//            // Wyświetlamy wykres
//            val modelProducer = remember { CartesianChartModelProducer() }
//
//
//            LaunchedEffect(pomiary) {
//                val (temperatury, czasy) = transformPomiaryToChartData(pomiary)
//
//                // Aktualizujemy model wykresu
//                modelProducer.runTransaction {
//                    lineSeries {
//                        series(
//                            x = czasy, //.map { it.toFloat() }, // Konwersja Long do Float dla wykresu
//                            y = temperatury
//                        )
//                    }
//                }
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .heightIn(min = 200.dp, max = 400.dp)
//            ) {
//
//                CartesianChartHost(
//                    rememberCartesianChart(
//                        rememberLineCartesianLayer(),
//                        startAxis = VerticalAxis.rememberStart(
//                            itemPlacer = VerticalAxis.ItemPlacer.count(
//                                count = { 8 } // Liczba etykiet do wyświetlenia na osi Y - MAX 8???
//                            ),
//                            valueFormatter = CartesianValueFormatter { _, y, _ ->
//                                "%.1f".format(y) // Formatowanie wartości z jedną cyfrą po przecinku
//                            }
//                        ),
//                        bottomAxis = HorizontalAxis.rememberBottom(
//                            valueFormatter = CartesianValueFormatter { _, x, _ ->
//                                // Formatowanie wartości osi X na HH:mm:ss dd-MM-yyyy
//                                val date =
//                                    Date(x.toLong()) // Konwersja wartości Float na Long i Date
//                                val dateFormat =
//                                    SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
//                                dateFormat.format(date) // Zwracamy sformatowaną datę
//                            }
//                        )
//                    ),
//                    modelProducer = modelProducer
//                )
//            }
//        }
//    }
//}

// COMPOSE CHARTS

//@Composable
//fun TemperatureChartScreen(viewModel: BluetoothViewModel, urzadzenieId: Long) {
//
//    // Uruchamiamy ładowanie danych
//    LaunchedEffect(urzadzenieId) {
//        viewModel.loadAllPomiaryWithParameters(urzadzenieId)
//    }
//
//    // Pobieramy dane i stan ładowania
//    val pomiary by viewModel.allPomiaryWithParameters.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//
//    when {
//        isLoading -> {
//            // Wyświetlamy stan ładowania
//            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Text(text = "Ładowanie danych...", style = MaterialTheme.typography.bodyLarge)
//            }
//        }
//
//        pomiary.isEmpty() -> {
//            // Wyświetlamy informację o braku danych
//            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Text(
//                    text = "Brak danych do wyświetlenia.",
//                    style = MaterialTheme.typography.bodyLarge
//                )
//            }
//        }
//
//        else -> {
//            val (temperatury, czasy) = transformPomiaryToChartData(pomiary)
//
////            val chartData = czasy.mapIndexed { index, time ->
////                Bars(
////                    label = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault()).format(Date(time)),
////                    values = listOf(
////                        Bars.Data(value = temperatury[index].toDouble(), color = SolidColor(Color(0xFF6495ED)))
////                    )
////                )
////            }
//
//            val lineData = listOf(
//                Line(
//                    label = "Temperatura",
//                    values = temperatury.map { it.toDouble() }, // Temperatura na osi Y
//                    color = SolidColor(Color(0xFF6495ED)),
//                    firstGradientFillColor = Color(0xFF6495ED).copy(alpha = .5f),
//                    secondGradientFillColor = Color.Transparent,
//                    strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
//                    gradientAnimationDelay = 1000,
//                    drawStyle = DrawStyle.Stroke(width = 2.dp),
//                )
//            )
//
//            val labelProperties = LabelProperties(
//                enabled = true,
//                textStyle = MaterialTheme.typography.bodySmall,
//                labels = czasy.map { time ->
//                    // Formatowanie dat na osi X
//                    val date = Date(time)
//                    val hour = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
//                    val day = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date)
//                    "$hour\n$day"
//                },
//                builder = { modifier, label, _, _ ->
//                    // Własne formatowanie etykiet
//                    Text(modifier = modifier, text = label)
//                }
//            )
//
//            val dotProperties = DotProperties(
//                enabled = true,
//                color = SolidColor(Color(0xFF6495ED)),
//                strokeWidth = 4.dp,
//                radius = 7.dp,
//                strokeColor = SolidColor(Color(0xFF6495ED)),
//            )
//
//            val dividerProperties = DividerProperties(
//                enabled = true,
//                xAxisProperties = LineProperties(color = SolidColor(Color.Gray), thickness = 2.dp),
//                yAxisProperties = LineProperties(color = SolidColor(Color.Gray), thickness = 2.dp)
//            )
//
//            val axisProperties = GridProperties.AxisProperties(
//                enabled = true,
//                style = StrokeStyle.Normal, // lub Dashed, zależnie od tego, co chcesz
//                color = SolidColor(Color.Gray),
//                thickness = 0.5.dp,
//                lineCount = 2 // zmniejsz liczbę linii na osi, dostosuj według swoich danych
//            )
//
//            val gridProperties = GridProperties(
//                xAxisProperties = axisProperties
//            )
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .heightIn(min = 200.dp, max = 400.dp)
//            ) {
////                RowChart(
////                    modifier = Modifier
////                        .fillMaxSize()
////                        .padding(horizontal = 22.dp),
////                    data = remember { chartData },
////                    barProperties = BarProperties(
////                        cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
////                        spacing = 3.dp,
////                        thickness = 20.dp
////                    ),
////                    animationSpec = spring(
////                        dampingRatio = Spring.DampingRatioMediumBouncy,
////                        stiffness = Spring.StiffnessLow
////                    ),
////                    minValue = 0.0,
////                    maxValue = temperatury.map { it.toDouble() }.maxOrNull() ?: 50.0,
////                )
//
//
//                LineChart(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(horizontal = 22.dp),
//                    data = remember { lineData },
//                    animationMode = AnimationMode.Together(delayBuilder = {
//                        it * 500L
//                    }),
//                    dotsProperties = dotProperties,
//                    dividerProperties = dividerProperties,
//                    gridProperties = gridProperties,
//                    minValue = temperatury.map { it.toDouble() }.minOrNull() ?: 30.0,
//                    maxValue = temperatury.map { it.toDouble() }.maxOrNull() ?: 50.0,
//                    labelProperties = labelProperties
//                )
//
//            }
//        }
//    }
//}

// YCHARTS
@Composable
fun TemperatureChartScreen(viewModel: BluetoothViewModel, urzadzenieId: Long) {
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
            //val steps = 5
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            //LaunchedEffect(pomiary) {
            //val ppointsData: List<Point> = transformPomiaryToChartData(pomiary)
            //}

            val (pointsData, timestamps) = transformPomiaryToChartData(pomiary)

            val temperatures = pointsData.map { it.y }
            val minTemperature = temperatures.minOrNull() ?: 0f
            val maxTemperature = temperatures.maxOrNull() ?: 100f
            //val pointsData: List<Point> =
              //  listOf(Point(0f, 40f), Point(1f, 90f), Point(2f, 0f), Point(3f, 60f), Point(4f, 10f))
            //val yStep = (maxTemperature - minTemperature) / steps
            val steps = 6

            val xAxisData = AxisData.Builder()
                .axisStepSize(100.dp)
                .backgroundColor(Color.Transparent)
                .steps(pointsData.size - 1)
                .labelData { i -> val timestamp = timestamps.getOrNull(i) ?: 0L
                    dateFormat.format(Date(timestamp)) }
                .labelAndAxisLinePadding(15.dp)
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
                .build()

            val yAxisData = AxisData.Builder()
                .steps(steps)
                .backgroundColor(Color.Transparent)
                .labelAndAxisLinePadding(20.dp)
                .labelData { i ->
                    val stepSize = (maxTemperature - minTemperature) / steps
                    String.format("%.1f", minTemperature + i * stepSize)
                }
                .axisLineColor(MaterialTheme.colorScheme.tertiary)
                .axisLabelColor(MaterialTheme.colorScheme.tertiary)
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
                            SelectionHighlightPopUp()
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(color = MaterialTheme.colorScheme.outline),
                backgroundColor = MaterialTheme.colorScheme.surface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
            ) {
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    lineChartData = lineChartData
                )
            }
        }
    }
}

fun transformPomiaryToChartData(pomiary: List<PomiarZParametrami>): Pair<List<Point>, List<Long>> {
    val dateFormat = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
    val pointsData = mutableListOf<Point>()
    val timestamps = mutableListOf<Long>()

    pomiary.forEach { pomiarZParametrami ->
        val timestamp = dateFormat.parse(pomiarZParametrami.pomiar.data)?.time ?: 0L
        pomiarZParametrami.parametry
            .filter { it.nazwa == "temperatura" }
            .forEach { parametr ->
                pointsData.add(Point(pointsData.size.toFloat(), parametr.wartosc))
                timestamps.add(timestamp)
            }
    }

    return Pair(pointsData, timestamps)
}


//fun transformPomiaryToChartData(pomiary: List<PomiarZParametrami>): List<Point> {
//    val dateFormat = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
//    val pointsData = mutableListOf<Point>()
//
//    pomiary.forEach { pomiarZParametrami ->
//
//        val timestamp = dateFormat.parse(pomiarZParametrami.pomiar.data)?.time?.toFloat() ?: 0f
//
//        pomiarZParametrami.parametry
//            .filter { it.nazwa == "temperatura" }
//            .forEach { parametr ->
//                pointsData.add(Point(timestamp, parametr.wartosc))
//            }
//    }
//
//    return pointsData
//}

// VICO i COMPOSE CHARTS
//fun transformPomiaryToChartData(pomiary: List<PomiarZParametrami>): Pair<List<Float>, List<Long>> {
//    val temperatury = mutableListOf<Float>()
//    val czasy = mutableListOf<Long>()
//
//    val dateFormat = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
//
//    pomiary.forEach { pomiarZParametrami ->
//
//        val timestamp = dateFormat.parse(pomiarZParametrami.pomiar.data)?.time ?: 0L
//
//        pomiarZParametrami.parametry
//            .filter { it.nazwa == "temperatura" }
//            .forEach { parametr ->
//                temperatury.add(parametr.wartosc)
//                czasy.add(timestamp)
//            }
//    }
//
//    return Pair(temperatury, czasy)
//}

