package com.example.healthmate

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthmate.data.HealthMateRepository
import com.example.healthmate.data.HealthMateRoomDatabase
import com.example.healthmate.data.ParametrPomiaru
import com.example.healthmate.data.ParametrPomiaruDAO
import com.example.healthmate.data.Pomiar
import com.example.healthmate.data.PomiarDAO
import com.example.healthmate.data.Urzadzenie
import com.example.healthmate.data.UrzadzenieDAO
import com.example.healthmate.data.Uzytkownik
import com.example.healthmate.data.UzytkownikDAO
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.runner.RunWith

import org.junit.After
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue


@RunWith(AndroidJUnit4::class)
class HealthMateRepositoryTest {

    private lateinit var database: HealthMateRoomDatabase
    private lateinit var urzadzenieDAO: UrzadzenieDAO
    private lateinit var pomiarDAO: PomiarDAO
    private lateinit var parametrPomiaruDAO: ParametrPomiaruDAO
    private lateinit var uzytkownikDAO: UzytkownikDAO
    private lateinit var healthMateRepository: HealthMateRepository

    @Before
    fun setup() {
        // Tworzymy bazę danych w pamięci
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthMateRoomDatabase::class.java
        ).build()

        urzadzenieDAO = database.urzadzenieDAO()
        pomiarDAO = database.pomiarDAO()
        parametrPomiaruDAO = database.parametrPomiaruDAO()
        uzytkownikDAO = database.uzytkownikDAO()

        healthMateRepository = HealthMateRepository(urzadzenieDAO, pomiarDAO, parametrPomiaruDAO, uzytkownikDAO)
    }

    @Test
    fun testAddSensor() = runBlocking {
        // Przygotowanie danych
        val sensor = Urzadzenie(
            urzadzenieId = 0, nazwa = "Sensor 1", uzytkownikId = 1,
            rodzaj = "a",
            model = "b",
            producent = "c"
        )

        // Dodajemy urządzenie do bazy
        val deviceId = healthMateRepository.addSensor(sensor)

        // Sprawdzamy, czy urządzenie zostało dodane poprawnie
        val addedDevice = urzadzenieDAO.getSensorById(deviceId.toInt())
        assertNotNull(addedDevice)
        assertEquals(sensor.nazwa, addedDevice?.nazwa)
    }

    @Test
    fun testAddMeasurement() = runBlocking {
        // Przygotowanie danych
        val pomiar = Pomiar(urzadzenieId = 1, data = "2025-01-01")

        // Dodajemy pomiar do bazy
        val measurementId = healthMateRepository.addMeasurement(pomiar)

        // Sprawdzamy, czy pomiar został dodany
        val addedMeasurement = pomiarDAO.getMeasurementsBySensorId(1)
        assertTrue(addedMeasurement.isNotEmpty())
    }

    @Test
    fun testGetAllSensorsForUser() = runBlocking {
        val sensor1 = Urzadzenie(
            urzadzenieId = 0, nazwa = "Sensor 1", uzytkownikId = 1,
            rodzaj = "a",
            model = "b",
            producent = "c"
        )
        val sensor2 = Urzadzenie(
            urzadzenieId = 0, nazwa = "Sensor 2", uzytkownikId = 1,
            rodzaj = "d",
            model = "e",
            producent = "f"
        )
        urzadzenieDAO.insertSensor(sensor1)
        urzadzenieDAO.insertSensor(sensor2)

        val sensors = healthMateRepository.getAllSensorsForUser(1).first()

        assertEquals(2, sensors.size)
        assertTrue(sensors.any { it.nazwa == "Sensor 1" })
    }

    @Test
    fun testGetLastPomiarWithParametersByUrzadzenieId() = runBlocking {
        val pomiar1 = Pomiar(pomiarId = 1, urzadzenieId = 1, data = "2025-01-01")
        val pomiar2 = Pomiar(pomiarId = 2, urzadzenieId = 1, data = "2025-01-02")
        pomiarDAO.insertMeasurement(pomiar1)
        pomiarDAO.insertMeasurement(pomiar2)

        val lastMeasurement = healthMateRepository.getLastPomiarWithParametersByUrzadzenieId(1)

        assertNotNull(lastMeasurement)
        assertEquals(pomiar2.data, lastMeasurement?.pomiar?.data)
    }

    @Test
    fun testGetAllMeasWithParametersByDevId() = runBlocking {
        val pomiar = Pomiar(pomiarId = 1, urzadzenieId = 1, data = "2025-01-01")
        pomiarDAO.insertMeasurement(pomiar)

        val parametrPomiaru = ParametrPomiaru(
            parametrId = 1, pomiarId = 1, nazwa = "Temperature", wartosc = 36.6f, jednostka = "C"
        )
        parametrPomiaruDAO.insertMeasurementParameter(parametrPomiaru)

        val measurements = healthMateRepository.getAllMeasWithParametersByDevId(1).first()

        assertEquals(1, measurements.size)
        assertEquals("Temperature", measurements.first().parametry.first().nazwa)
    }

    @Test
    fun testAddMeasurementParameter() = runBlocking {
        val parameter = ParametrPomiaru(parametrId = 0, pomiarId = 1, nazwa = "Pressure", wartosc = 120.0f, jednostka = "mmHg")
        healthMateRepository.addMeasurementParameter(parameter)

        val parameters = parametrPomiaruDAO.getParametersByMeasurementId(1)

        assertTrue(parameters.isNotEmpty())
        assertEquals("Pressure", parameters.first().nazwa)
    }

    @Test
    fun testAddUser() = runBlocking {
        val user = Uzytkownik(uzytkownikId = 0, login = "test_user", imie = "name", haslo = "password")
        val userId = healthMateRepository.addUser(user)
        val addedUser = uzytkownikDAO.getUserByLogin("test_user")

        assertNotNull(addedUser)
        assertEquals(user.login, addedUser?.login)
    }

    @Test
    fun testGetUserByLogin() = runBlocking {
        val user = Uzytkownik(uzytkownikId = 0, login = "test_user", imie = "name", haslo = "password")
        uzytkownikDAO.insertUzytkownik(user)

        val fetchedUser = healthMateRepository.getUserByLogin("test_user")

        assertNotNull(fetchedUser)
        assertEquals(user.login, fetchedUser?.login)
    }

    @Test
    fun testCheckIfMeasurementExists() = runBlocking {
        val measurement = Pomiar(urzadzenieId = 1, data = "2025-01-01")
        pomiarDAO.insertMeasurement(measurement)

        val exists = healthMateRepository.checkIfMeasurementExists(1, "2025-01-01")

        assertTrue(exists)
    }

    @Test
    fun testGetMeasurementsBySensor() = runBlocking {
        val measurement = Pomiar(urzadzenieId = 1, data = "2025-01-01")
        pomiarDAO.insertMeasurement(measurement)

        val measurements = healthMateRepository.getMeasurementsBySensor(1)

        assertEquals(1, measurements.size)
        assertEquals(measurement.data, measurements.first().data)
    }
    
    @After
    fun tearDown() {
        // Zamykamy bazę danych po zakończeniu testów
        database.close()
    }
}