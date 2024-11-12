package com.example.healthmate.data

import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HealthMateRepository(private val urzadzenieDAO: UrzadzenieDAO, private val pomiarDAO: PomiarDAO, private val parametrPomiaruDAO: ParametrPomiaruDAO) {

    suspend fun clearDatabase() {
        urzadzenieDAO.clearAllSensors() // Wyczyści tabelę urządzeń
        pomiarDAO.clearAllPomiary() // Wyczyści tabelę pomiarów
        parametrPomiaruDAO.clearAllParametryPomiaru() // Wyczyści tabelę parametrów pomiaru
    }

    //region Sensor-related operations
    suspend fun addSensor(urzadzenie: Urzadzenie): Long {
        val existingDevice = urzadzenieDAO.getSensorByName(urzadzenie.nazwa)
        return if (existingDevice == null) {
            // Jeśli urządzenie nie istnieje, zapisz je i zwróć ID nowo dodanego urządzenia
            val deviceId = urzadzenieDAO.insertSensor(urzadzenie)
            Log.d("HealthMateRepository", "Davice saved, ID: $deviceId")
            deviceId
        } else {
            // Jeśli urządzenie już istnieje, zwróć jego ID
            Log.d("HealthMateRepository", "Device ${urzadzenie.nazwa} already exists, ID: ${existingDevice.urzadzenieId}")
            existingDevice.urzadzenieId
        }
    }

    fun getAllSensors(): Flow<List<Urzadzenie>> {
        return urzadzenieDAO.getAllSensors()
    }

//    @Suppress("RedundantSuspendModifier")
//    @WorkerThread
//    suspend fun insert(urzadzenie: Urzadzenie) {
//        urzadzenieDAO.insertSensor(urzadzenie)
//    }
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

//    suspend fun getMeasurementsByUser(userId: Int): List<Measurement> {
//        return withContext(Dispatchers.IO) {
//            measurementDao.getMeasurementsByUserId(userId)
//        }
//    }

//    suspend fun getMeasurementsByTimeRange(startTime: String, endTime: String): List<Pomiar> {
//        return withContext(Dispatchers.IO) {
//            pomiarDAO.getMeasurementsByTimeRange(startTime, endTime)
//        }
//    }
    //endregion

    //region MeasurementParameter-related operations
    suspend fun addMeasurementParameter(parametrPomiaru: ParametrPomiaru) {
        withContext(Dispatchers.IO) {
            parametrPomiaruDAO.insertMeasurementParameter(parametrPomiaru)
        }
    }

    suspend fun getParametersByMeasurementId(pomiarId: Int): List<ParametrPomiaru> {
        return withContext(Dispatchers.IO) {
            parametrPomiaruDAO.getParametersByMeasurementId(pomiarId)
        }
    }

//    suspend fun deleteParametersByMeasurementId(measurementId: Int) {
//        withContext(Dispatchers.IO) {
//            measurementParameterDao.deleteParametersByMeasurementId(measurementId)
//        }
//    }
    //endregion
}