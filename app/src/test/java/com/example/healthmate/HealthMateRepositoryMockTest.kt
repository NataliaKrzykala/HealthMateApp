//package com.example.healthmate
//
//import com.example.healthmate.data.HealthMateRepository
//import com.example.healthmate.data.ParametrPomiaru
//import com.example.healthmate.data.ParametrPomiaruDAO
//import com.example.healthmate.data.Pomiar
//import com.example.healthmate.data.PomiarDAO
//import com.example.healthmate.data.PomiarZParametrami
//import com.example.healthmate.data.Urzadzenie
//import com.example.healthmate.data.UrzadzenieDAO
//import com.example.healthmate.data.Uzytkownik
//import com.example.healthmate.data.UzytkownikDAO
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.flowOf
//import kotlinx.coroutines.runBlocking
//import org.junit.Before
//import org.junit.runner.RunWith
//import org.mockito.Mock
//import org.mockito.junit.MockitoJUnitRunner
//import org.junit.Test
//import org.mockito.Mockito
//
//import org.junit.Assert.*
//
//@RunWith(MockitoJUnitRunner::class)
//class HealthMateRepositoryMockTest {
//
//    // Zmienna dla repozytorium
//    private lateinit var healthMateRepository: HealthMateRepository
//
//    // Mockowanie DAO
//    @Mock
//    private lateinit var urzadzenieDAO: UrzadzenieDAO
//
//    @Mock
//    private lateinit var pomiarDAO: PomiarDAO
//
//    @Mock
//    private lateinit var parametrPomiaruDAO: ParametrPomiaruDAO
//
//    @Mock
//    private lateinit var uzytkownikDAO: UzytkownikDAO
//
//    @Before
//    fun setup() {
//        // Instancja repozytorium z mockowanymi DAO
//        healthMateRepository = HealthMateRepository(urzadzenieDAO, pomiarDAO, parametrPomiaruDAO, uzytkownikDAO)
//    }
//
//    @Test
//    fun `addSensor should add sensor if no such sensor exists in database`() = runBlocking {
//        val sensor = Urzadzenie(
//            urzadzenieId = 0,
//            nazwa = "Sensor 1",
//            uzytkownikId = 1,
//            rodzaj = "a",
//            model = "b",
//            producent = "c"
//        )
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(urzadzenieDAO.getDeviceByNameAndUserId("Sensor 1", 1)).thenReturn(null)
//        Mockito.`when`(urzadzenieDAO.insertSensor(sensor)).thenReturn(1L)
//
//        // Testowanie metody addSensor
//        val deviceId = healthMateRepository.addSensor(sensor)
//
//        // Weryfikacja, czy odpowiedź jest zgodna z oczekiwaniami
//        Mockito.verify(urzadzenieDAO).insertSensor(sensor) // Weryfikacja czy, insertSensor został wywołany
//        assertEquals(1L, deviceId)  // Weryfikacja, czy ID jest poprawne
//    }
//
//    @Test
//    fun `addSensor should not add sensor if such sensor exists in database`() = runBlocking {
//        val sensor = Urzadzenie(
//            urzadzenieId = 5,
//            nazwa = "Sensor 1",
//            uzytkownikId = 1,
//            rodzaj = "a",
//            model = "b",
//            producent = "c"
//        )
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(urzadzenieDAO.getDeviceByNameAndUserId("Sensor 1", 1)).thenReturn(sensor)
//
//        // Testowanie metody addSensor
//        val deviceId = healthMateRepository.addSensor(sensor)
//
//        // Weryfikacja, czy insertSensor NIE został wywołany, ponieważ sensor już istnieje
//        Mockito.verify(urzadzenieDAO, Mockito.never()).insertSensor(sensor)
//
//        // Weryfikacja, czy id nie zostało zmienione, czyli metoda addSensor zwróciła odpowiednią wartość ID
//        assertEquals(5L, deviceId)
//    }
//
//    @Test
//    fun `getAllSensorsForUser should return all sensors for the specified user`() = runBlocking {
//        val userId = 1L
//        val expectedSensors = listOf(
//            Urzadzenie(urzadzenieId = 1, nazwa = "Sensor 1", uzytkownikId = userId, rodzaj = "a", model = "x", producent = "y"),
//            Urzadzenie(urzadzenieId = 2, nazwa = "Sensor 2", uzytkownikId = userId, rodzaj = "b", model = "y", producent = "z")
//        )
//
//        // Mockowanie DAO
//        Mockito.`when`(urzadzenieDAO.getAllSensorsForUser(userId)).thenReturn(flowOf(expectedSensors))
//
//        // Testowanie metody
//        val sensorsFlow = healthMateRepository.getAllSensorsForUser(userId)
//        val sensors = sensorsFlow.first() // Pobranie wartości z Flow
//
//        // Weryfikacja
//        Mockito.verify(urzadzenieDAO).getAllSensorsForUser(userId)
//        assertEquals(expectedSensors, sensors)
//    }
//
//    @Test
//    fun `addMeasurement should insert measurement and return id`() = runBlocking {
//        val measurement = Pomiar(
//            pomiarId = 0,
//            data = "2025-01-01T12:00:00Z",
//            urzadzenieId = 1L
//        )
//        val expectedId = 42L
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(pomiarDAO.insertMeasurement(measurement)).thenReturn(expectedId)
//
//        // Testowanie metody addMeasurement
//        val resultId = healthMateRepository.addMeasurement(measurement)
//
//        // Weryfikacja czy insertMeasurement zostało wywołane i czy zwrociło odpowiednią wartość ID
//        Mockito.verify(pomiarDAO).insertMeasurement(measurement)
//        assertEquals(expectedId, resultId)
//    }
//
//    @Test
//    fun `getMeasurementsBySensor should return list of measurements for a sensor`() = runBlocking {
//        val sensorId = 1
//        val expectedMeasurements = listOf(
//            Pomiar(
//                pomiarId = 1,
//                data = "2025-01-01T12:00:00Z",
//                urzadzenieId = sensorId.toLong()
//            ),
//            Pomiar(
//                pomiarId = 2,
//                data = "2025-01-02T12:00:00Z",
//                urzadzenieId = sensorId.toLong()
//            )
//        )
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(pomiarDAO.getMeasurementsBySensorId(sensorId)).thenReturn(expectedMeasurements)
//
//        // Testowanie metody getMeasurementsBySensor
//        val measurements = healthMateRepository.getMeasurementsBySensor(sensorId)
//
//        // Weryfikacja czy getMeasurementsBySensorId zostało wywołane i czy zwróciło oczekiwaną listę
//        Mockito.verify(pomiarDAO).getMeasurementsBySensorId(sensorId)
//        assertEquals(expectedMeasurements, measurements)
//    }
//
//    @Test
//    fun `getMeasurementsBySensor should return empty list of measurements for a sensor without assigned measurements`() = runBlocking {
//        val sensorId = 1
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(pomiarDAO.getMeasurementsBySensorId(sensorId)).thenReturn(null)
//
//        // Testowanie metody getMeasurementsBySensor
//        val measurements = healthMateRepository.getMeasurementsBySensor(sensorId)
//
//        // Weryfikacja czy getMeasurementsBySensorId zostało wywołane i czy zwróciło oczekiwaną listę
//        Mockito.verify(pomiarDAO).getMeasurementsBySensorId(sensorId)
//        assertNull(measurements)
//    }
//
//    @Test
//    fun `getLastPomiarWithParametersByUrzadzenieId should return the last measurement with parameters`() = runBlocking {
//        val urzadzenieId = 1L
//        val expectedPomiarZParametrami = PomiarZParametrami(
//            pomiar = Pomiar(pomiarId = 10, urzadzenieId = urzadzenieId, data = "2023-12-31"),
//            parametry = listOf(
//                ParametrPomiaru(parametrId = 1, pomiarId = 10, nazwa = "Temperature", wartosc = 36.6f, jednostka = "C"),
//                ParametrPomiaru(parametrId = 2, pomiarId = 10, nazwa = "Pressure", wartosc = 120.0f, jednostka = "mmHg")
//            )
//        )
//
//        // Mockowanie DAO
//        Mockito.`when`(pomiarDAO.getLastMeasWithParametersByDevId(urzadzenieId)).thenReturn(expectedPomiarZParametrami)
//
//        // Testowanie metody
//        val pomiar = healthMateRepository.getLastPomiarWithParametersByUrzadzenieId(urzadzenieId)
//
//        // Weryfikacja
//        Mockito.verify(pomiarDAO).getLastMeasWithParametersByDevId(urzadzenieId)
//        assertEquals(expectedPomiarZParametrami, pomiar)
//    }
//
//    @Test
//    fun `getAllMeasWithParametersByDevId should return all measurements with parameters for device`() = runBlocking {
//        val urzadzenieId = 1L
//        val expectedMeasurements = listOf(
//            PomiarZParametrami(
//                pomiar = Pomiar(pomiarId = 10, urzadzenieId = urzadzenieId, data = "2023-12-31"),
//                parametry = listOf(
//                    ParametrPomiaru(parametrId = 1, pomiarId = 10, nazwa = "Temperature", wartosc = 36.6f, jednostka = "C")
//                )
//            ),
//            PomiarZParametrami(
//                pomiar = Pomiar(pomiarId = 11, urzadzenieId = urzadzenieId, data = "2024-01-01"),
//                parametry = listOf(
//                    ParametrPomiaru(parametrId = 2, pomiarId = 11, nazwa = "Pressure", wartosc = 120.0f, jednostka = "mmHg")
//                )
//            )
//        )
//
//        // Mockowanie DAO
//        Mockito.`when`(pomiarDAO.getAllMeasWithParametersByDevId(urzadzenieId)).thenReturn(flowOf(expectedMeasurements))
//
//        // Testowanie metody
//        val measurementsFlow = healthMateRepository.getAllMeasWithParametersByDevId(urzadzenieId)
//        val measurements = measurementsFlow.first() // Pobranie wartości z Flow
//
//        // Weryfikacja
//        Mockito.verify(pomiarDAO).getAllMeasWithParametersByDevId(urzadzenieId)
//        assertEquals(expectedMeasurements, measurements)
//    }
//
//
//
//    @Test
//    fun `checkIfMeasurementExists should return true if measurement exists`() = runBlocking {
//        val deviceId = 1L
//        val timestamp = "2025-01-01T12:00:00Z"
//        val expectedExists = true
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(pomiarDAO.doesMeasurementExist(deviceId, timestamp)).thenReturn(expectedExists)
//
//        // Testowanie metody checkIfMeasurementExists
//        val exists = healthMateRepository.checkIfMeasurementExists(deviceId, timestamp)
//
//        // Weryfikacja czy doesMeasurementExist zostało wywołane i co zwróciło
//        Mockito.verify(pomiarDAO).doesMeasurementExist(deviceId, timestamp)
//        assertTrue(exists)
//    }
//
//    @Test
//    fun `checkIfMeasurementExists should return false if measurement does not exists`() = runBlocking {
//        val deviceId = 1L
//        val timestamp = "2025-01-01T12:00:00Z"
//        val expectedExists = false
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(pomiarDAO.doesMeasurementExist(deviceId, timestamp)).thenReturn(expectedExists)
//
//        // Testowanie metody checkIfMeasurementExists
//        val exists = healthMateRepository.checkIfMeasurementExists(deviceId, timestamp)
//
//        // Weryfikacja czy doesMeasurementExist zostało wywołane i co zwróciło
//        Mockito.verify(pomiarDAO).doesMeasurementExist(deviceId, timestamp)
//        assertFalse(exists)
//    }
//
//    @Test
//    fun `addMeasurementParameter should insert measurement parameter into the database`() = runBlocking {
//        val parameter = ParametrPomiaru(parametrId = 0, pomiarId = 10, nazwa = "Temperature", wartosc = 36.6f, jednostka = "C")
//
//        // Testowanie metody
//        healthMateRepository.addMeasurementParameter(parameter)
//
//        // Weryfikacja
//        Mockito.verify(parametrPomiaruDAO).insertMeasurementParameter(parameter)
//    }
//
//
//    @Test
//    fun `addUser should insert new user if user did not exist`() = runBlocking {
//        val newUser = Uzytkownik(
//            uzytkownikId = 0,
//            login = "newUser",
//            haslo = "password123",
//            imie = "name"
//        )
//        val expectedUserId = 42L
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(uzytkownikDAO.getUserByLogin(newUser.login)).thenReturn(null)
//        Mockito.`when`(uzytkownikDAO.insertUzytkownik(newUser)).thenReturn(expectedUserId)
//
//        // Testowanie metody addUser
//        val userId = healthMateRepository.addUser(newUser)
//
//        // Weryfikacja czy getUserByLogin i insertUzytkownik zostały wywołane i czy ID się zgadza
//        Mockito.verify(uzytkownikDAO).getUserByLogin(newUser.login)
//        Mockito.verify(uzytkownikDAO).insertUzytkownik(newUser)
//        assertEquals(expectedUserId, userId)
//    }
//
//    @Test
//    fun `addUser should return existing userId if user already exists`() = runBlocking {
//        val existingUser = Uzytkownik(
//            uzytkownikId = 42,
//            login = "existingUser",
//            haslo = "password123",
//            imie = "name"
//        )
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(uzytkownikDAO.getUserByLogin(existingUser.login)).thenReturn(existingUser)
//
//        // Testowanie metody addUser
//        val userId = healthMateRepository.addUser(existingUser)
//
//        // Weryfikacja czy getUserByLogin zostało wywołane i czy insertUzytkownik nie zostało wywołane bo dany użytkownik istnieje
//        Mockito.verify(uzytkownikDAO).getUserByLogin(existingUser.login)
//        Mockito.verify(uzytkownikDAO, Mockito.never()).insertUzytkownik(existingUser)
//        assertEquals(existingUser.uzytkownikId, userId)  // Weryfikacja, czy id nie zostało zmienione, czyli metoda addUser zwróciła ID istniejącego wcześniej użytkownika
//    }
//
//    @Test
//    fun `getUserByLogin should return user for existing login`() = runBlocking {
//        val login = "existingUser"
//        val expectedUser = Uzytkownik(
//            uzytkownikId = 42,
//            imie = "name",
//            login = login,
//            haslo = "password123"
//        )
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(uzytkownikDAO.getUserByLogin(login)).thenReturn(expectedUser)
//
//        // Testowanie metody getUserByLogin
//        val user = healthMateRepository.getUserByLogin(login)
//
//        // Weryfikacja czy getUserByLogin zostało wywołane i czy ID się zgadza
//        Mockito.verify(uzytkownikDAO).getUserByLogin(login)
//        assertEquals(expectedUser, user)
//    }
//
//    @Test
//    fun `getUserByLogin should return null for non-existing login`() = runBlocking {
//        val login = "nonExistingUser"
//
//        // Mockowanie odpowiedzi DAO
//        Mockito.`when`(uzytkownikDAO.getUserByLogin(login)).thenReturn(null)
//
//        // Testowanie metody getUserByLogin
//        val user = healthMateRepository.getUserByLogin(login)
//
//        // Weryfikacja czy getUserByLogin zostało wywołane i czy zwróciło null
//        Mockito.verify(uzytkownikDAO).getUserByLogin(login)
//        assertNull(user)
//    }
//
//}