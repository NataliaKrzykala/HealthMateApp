package com.example.healthmate.data

import android.util.Log
import androidx.annotation.WorkerThread
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HealthMateRepository(
    private val urzadzenieDAO: UrzadzenieDAO,
    private val pomiarDAO: PomiarDAO,
    private val parametrPomiaruDAO: ParametrPomiaruDAO,
    private val uzytkownikDAO: UzytkownikDAO
){

    suspend fun clearDatabase() {
        urzadzenieDAO.clearAllSensors()
        pomiarDAO.clearAllPomiary()
        parametrPomiaruDAO.clearAllParametryPomiaru()
        uzytkownikDAO.clearAllUzytkownik()
    }

    //region Sensor-related operations
    suspend fun addSensor(urzadzenie: Urzadzenie): Long {
        val existingDevice = urzadzenieDAO.getDeviceByNameAndUserId(urzadzenie.nazwa, urzadzenie.uzytkownikId)
        return if (existingDevice == null) {
            // Jeśli urządzenie nie istnieje, zapisz je i zwróć ID nowo dodanego urządzenia
            val deviceId = urzadzenieDAO.insertSensor(urzadzenie)
            //Log.d("HealthMateRepository", "Davice saved, ID: $deviceId")
            deviceId
        } else {
            // Jeśli urządzenie już istnieje, zwróć jego ID
            //Log.d("HealthMateRepository", "Device ${urzadzenie.nazwa} already exists, ID: ${existingDevice.urzadzenieId}")
            existingDevice.urzadzenieId
        }
    }

    fun getAllSensorsForUser(userId: Long): Flow<List<Urzadzenie>> {
        return urzadzenieDAO.getAllSensorsForUser(userId)
    }

    fun getAllSensors(): List<Urzadzenie> {
        return urzadzenieDAO.getAllSensors()
    }
    //endregion

    //region Measurement-related operations
    suspend fun addMeasurement(pomiar: Pomiar): Long {
        val pomiarId = pomiarDAO.insertMeasurement(pomiar)
        return pomiarId
    }

    suspend fun getMeasurementsBySensor(urzadzenieId: Int): List<Pomiar> {
        return withContext(Dispatchers.IO) {
            pomiarDAO.getMeasurementsBySensorId(urzadzenieId)
        }
    }

    suspend fun getLastPomiarWithParametersByUrzadzenieId(urzadzenieId: Long): PomiarZParametrami? {
        return pomiarDAO.getLastMeasWithParametersByDevId(urzadzenieId)
    }

    fun getAllMeasWithParametersByDevId(urzadzenieId: Long): Flow<List<PomiarZParametrami>> {
        return pomiarDAO.getAllMeasWithParametersByDevId(urzadzenieId)
    }

    suspend fun checkIfMeasurementExists(deviceId: Long, timestamp: String): Boolean {
        return pomiarDAO.doesMeasurementExist(deviceId, timestamp)
    }

//    suspend fun getAllMeasurements(): List<Pomiar> {
//        return pomiarDAO.getAllMeasurements()
//    }

    fun getLastMeasurementTimestamp(deviceId: Long): String {
        return pomiarDAO.getLastMeasurementTimestamp(deviceId)
    }
    //endregion

    //region MeasurementParameter-related operations
    suspend fun addMeasurementParameter(parametrPomiaru: ParametrPomiaru) {
        withContext(Dispatchers.IO) {
            parametrPomiaruDAO.insertMeasurementParameter(parametrPomiaru)
        }
    }

    fun getAllMeasurementParameters(): List<ParametrPomiaru> {
        return parametrPomiaruDAO.getAllMeasurementParameters()
    }
    //endregion

    //region User-related operations
    suspend fun addUser(uzytkownik: Uzytkownik): Long {
        val existingUser = uzytkownikDAO.getUserByLogin(uzytkownik.login)
        return if (existingUser == null) {
             val userId = uzytkownikDAO.insertUzytkownik(uzytkownik)
             userId
        }else{
            existingUser.uzytkownikId
        }
    }
    suspend fun getUserByLogin(login: String): Uzytkownik? {
        return withContext(Dispatchers.IO) {
            uzytkownikDAO.getUserByLogin(login)
        }
    }

    fun getAllUsers(): List<Uzytkownik> {
        return uzytkownikDAO.getAllUsers()
    }
    //endregion
}