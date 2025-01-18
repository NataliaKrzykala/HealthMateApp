//package com.example.healthmate
//
//import app.cash.turbine.test
//import com.example.healthmate.ble.BluetoothDev
//import com.example.healthmate.ble.BluetoothHandler
//import com.example.healthmate.ble.BluetoothViewModel
//import com.example.healthmate.data.HealthMateRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.test.*
//import org.junit.jupiter.api.*
//import org.junit.jupiter.api.Assertions
//
//import org.mockito.Mockito.*
//import java.util.UUID
////import org.junit.Test
//
////import org.junit.jupiter.api.Test
////import org.junit.jupiter.api.BeforeEach
//
//
////@OptIn(ExperimentalCoroutinesApi::class)
//class BluetoothViewModelTest {
//
//    private val bluetoothHandler = mock(BluetoothHandler::class.java)
//    private val repository = mock(HealthMateRepository::class.java)
//    private lateinit var viewModel: BluetoothViewModel
//    //private val testDispatcher = StandardTestDispatcher()
//
//    @BeforeEach
//    fun setup() {
////        Dispatchers.setMain(testDispatcher)
//        viewModel = BluetoothViewModel(bluetoothHandler, repository)
//    }
////
////    @AfterEach
////    fun tearDown() {
////        Dispatchers.resetMain() // Resetuje `Dispatchers.Main` do domyślnej wartości
////        testDispatcher.cancel() // Anulowanie testowego kontekstu (opcja)
////    }
//
////    @Test
////    fun onConnectionStateChangedupdatesconnectionStateFlow()  {
////        // Act
////        viewModel.onConnectionStateChanged(BluetoothHandler.ConnectionState.CONNECTED)
////
////        // Assert
////        viewModel.connectionStateFlow.test {
////            Assertions.assertEquals(BluetoothHandler.ConnectionState.CONNECTED, awaitItem())
////        }
////    }
//
//
//    @Test
//    fun `updateCharacteristicValues updates characteristicValues`() = runTest {
//        // Arrange
//        val mockValues = mapOf(UUID.randomUUID() to "Value1", UUID.randomUUID() to "Value2")
//
//        // Act
//        viewModel.updateCharacteristicValues(mockValues)
//
//        // Assert
//        viewModel.characteristicValues.test {
//            Assertions.assertEquals(mockValues, awaitItem())
//        }
//    }
//
//    @Test
//    fun `saveDeviceAndMeasurement saves device and measurement`() = runTest {
//        // Arrange
//        val userId = 1L
//        val deviceType = mock(BluetoothDev::class.java)
//        val parsedData = mapOf("Key1" to "Value1")
//        val values = mapOf(UUID.randomUUID() to "Manufacturer")
//        `when`(repository.addSensor(any())).thenReturn(1L)
//
//
//
//        // Act
//        viewModel.saveDeviceAndMeasurement(
//            userId = userId,
//            values = values,
//            deviceType = deviceType,
//            devName = "Device",
//            parsedData = parsedData,
//            unknown = "unknown",
//            noInfo = "no info",
//            thermometerName = "thermometer",
//            weightScaleName = "scale",
//            bpmName = "bpm",
//            temperatureName = "temperature",
//            pulseName = "pulse",
//            timeOfMeas = "Time"
//        )
//
//        // Assert
//        verify(repository, times(1)).addSensor(any())
//    }
//}