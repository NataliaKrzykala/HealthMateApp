package com.example.healthmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Uzytkownik")
data class Uzytkownik(
    @PrimaryKey(autoGenerate = true) val uzytkownikId: Long = 0,
    val imie: String,
    val login: String,
    val haslo: String
)