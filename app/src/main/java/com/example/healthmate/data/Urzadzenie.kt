package com.example.healthmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Urzadzenie")
data class Urzadzenie(
    @PrimaryKey(autoGenerate = true) val urzadzenieId: Long = 0,
    val uzytkownikId: Long,
    val nazwa: String,
    val rodzaj: String,
    val model: String,
    val producent: String
)