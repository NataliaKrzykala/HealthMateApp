package com.example.healthmate.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Database(entities = [Urzadzenie::class, Pomiar::class, ParametrPomiaru::class], version = 3, exportSchema = false)
public abstract class HealthMateRoomDatabase : RoomDatabase() {

    abstract fun urzadzenieDAO(): UrzadzenieDAO
    abstract fun pomiarDAO(): PomiarDAO
    abstract fun parametrPomiaruDAO(): ParametrPomiaruDAO

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: HealthMateRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): HealthMateRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthMateRoomDatabase::class.java,
                    "word_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(HealthMateDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        suspend fun clearDatabase(context: Context) {
            // Jeśli chcesz wykonać operację czyszczenia w tle:
            withContext(Dispatchers.IO) {
                getDatabase(context, CoroutineScope(Dispatchers.IO))
                INSTANCE?.clearAllTables()
            }
        }

        private class HealthMateDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDatabase(database.pomiarDAO(), database.urzadzenieDAO(), database.parametrPomiaruDAO())
                    }
                }
            }

            suspend fun populateDatabase(pomiarDAO: PomiarDAO, urzadzenieDAO: UrzadzenieDAO, parametrPomiaruDAO: ParametrPomiaruDAO) {
                // Delete all content here.
                //wordDao.deleteAll()

                // Add sample words.
                var urzadzenie = Urzadzenie(nazwa = "urzadzenie1", producent = "A&D", rodzaj = "termometr", model = "model1")
                urzadzenieDAO.insertSensor(urzadzenie)

                // TODO: Add your own words!
            }
        }

    }
}