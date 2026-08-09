package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Entity(tableName = "hba1c_entries")
data class Hba1c(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName(value = "hba1c_measure_date", alternate = ["date"])
    val date: LocalDate,
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: LocalDate,
    @SerializedName("is_archived", alternate = ["isArchived"])
    val isArchived: Boolean,
    @SerializedName("hba1c_value", alternate = ["value"])
    val value: Float,
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updatedAt: LocalDate,
)