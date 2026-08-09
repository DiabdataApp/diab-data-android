package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Entity(tableName = "weight_entries")
data class Weight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("weigh_in_date", alternate = ["date"])
    val date: LocalDate,
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: LocalDate,
    @SerializedName("is_archived", alternate = ["isArchived"])
    val isArchived: Boolean,
    @SerializedName("weight_value", alternate = ["value"])
    val value: Float,
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updatedAt: LocalDate,
)