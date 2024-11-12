package com.example.healthmate.data

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class HMApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { HealthMateRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { HealthMateRepository(database.urzadzenieDAO(), database.pomiarDAO(), database.parametrPomiaruDAO()) }
}