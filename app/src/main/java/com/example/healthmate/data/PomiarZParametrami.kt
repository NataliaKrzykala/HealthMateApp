package com.example.healthmate.data

import androidx.room.Embedded
import androidx.room.Relation

data class PomiarZParametrami(
    @Embedded val pomiar: Pomiar,
    @Relation(
        parentColumn = "pomiarId",
        entityColumn = "pomiarId"
    )
    val parametry: List<ParametrPomiaru>
)