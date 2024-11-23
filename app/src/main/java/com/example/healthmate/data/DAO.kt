package com.example.healthmate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UrzadzenieDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSensor(urzadzenie: Urzadzenie): Long

    @Query("SELECT * FROM Urzadzenie WHERE urzadzenieId = :urzadzenieId")
    suspend fun getSensorById(urzadzenieId: Int): Urzadzenie?

    @Query("SELECT * FROM urzadzenie WHERE nazwa = :nazwa")
    suspend fun getSensorByName(nazwa: String): Urzadzenie?

    @Query("SELECT * FROM Urzadzenie WHERE nazwa = :nazwa AND uzytkownikId = :uzytkownikId")
    suspend fun getDeviceByNameAndUserId(nazwa: String, uzytkownikId: Long): Urzadzenie?

    @Query("SELECT * FROM Urzadzenie")
    fun getAllSensors(): Flow<List<Urzadzenie>>

    @Query("SELECT * FROM Urzadzenie WHERE uzytkownikId =:uzytkownikId")
    fun getAllSensorsForUser(uzytkownikId: Long): Flow<List<Urzadzenie>>

    @Query("DELETE FROM Urzadzenie")
    suspend fun clearAllSensors()

    //@Query("DELETE FROM Urzadzenie WHERE nazwa = :nazwa")
    //suspend fun deleteSensor(nazwa: String)
}

@Dao
interface PomiarDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeasurement(pomiar: Pomiar): Long

    //@Query("SELECT * FROM Measurement WHERE userId = :userId")
    //suspend fun getMeasurementsByUserId(userId: Int): List<Measurement>

    @Query("SELECT * FROM Pomiar WHERE urzadzenieId = :urzadzenieId")
    suspend fun getMeasurementsBySensorId(urzadzenieId: Int): List<Pomiar>

    @Query("DELETE FROM Pomiar")
    suspend fun clearAllPomiary()

    @Query("SELECT EXISTS(SELECT 1 FROM Pomiar WHERE urzadzenieId = :urzadzenieId AND data = :data LIMIT 1)")
    suspend fun doesMeasurementExist(urzadzenieId: Long, data: String): Boolean

    @Transaction
    @Query("""
        SELECT * FROM Pomiar 
        WHERE urzadzenieId = :urzadzenieId 
        ORDER BY data DESC 
        LIMIT 1
    """)
    suspend fun getLastMeasWithParametersByDevId(urzadzenieId: Long): PomiarZParametrami?

    @Transaction
    @Query("""
    SELECT * FROM Pomiar 
    WHERE urzadzenieId = :urzadzenieId 
    ORDER BY data ASC
""")
    fun getAllMeasWithParametersByDevId(urzadzenieId: Long): Flow<List<PomiarZParametrami>>

//    @Query("SELECT * FROM Pomiar WHERE data BETWEEN :poczatek AND :koniec")
//    suspend fun getMeasurementsByTimeRange(poczatek: String, koniec: String): List<Pomiar> // JAKA JEDNOSTKA DLA TIMESTAMP'U
}

@Dao
interface ParametrPomiaruDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeasurementParameter(parametrPomiaru: ParametrPomiaru)

    @Query("SELECT * FROM ParametrPomiaru WHERE pomiarId = :pomiarId")
    suspend fun getParametersByMeasurementId(pomiarId: Int): List<ParametrPomiaru>

    @Query("DELETE FROM ParametrPomiaru")
    suspend fun clearAllParametryPomiaru()

    //@Query("DELETE FROM ParametrPomiaru WHERE pomiarId = :pomiarId")
    //suspend fun deleteParametersByMeasurementId(pomiarId: Int)
}

@Dao
interface UzytkownikDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUzytkownik(uzytkownik: Uzytkownik): Long

    @Query("SELECT * FROM Uzytkownik WHERE login = :login")
    suspend fun getUserByLogin(login: String): Uzytkownik

    @Query("DELETE FROM Uzytkownik")
    suspend fun clearAllUzytkownik()

}