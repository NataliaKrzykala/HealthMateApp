package com.example.healthmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Uzytkownik")
data class Uzytkownik(
    @PrimaryKey(autoGenerate = false) val login: String,
    val imie: String,
    val haslo: String
)