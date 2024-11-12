package com.example.healthmate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ParametrPomiaru")
data class ParametrPomiaru(
    @PrimaryKey(autoGenerate = true) val parametrId: Long = 0,
    val pomiarId: Long,
    val nazwa: String,
    val wartosc: Float,
    val jednostka: String
)