package com.example.healthmate.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Pomiar")
data class Pomiar(
    @PrimaryKey(autoGenerate = true) val pomiarId: Long = 0,
    val urzadzenieId: Long,
    val data: String
)