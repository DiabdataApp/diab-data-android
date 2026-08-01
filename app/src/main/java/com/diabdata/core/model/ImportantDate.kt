package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Entity(tableName = "important_date_entries")
data class ImportantDate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName(value = "important_date_date", alternate = ["date"])
    val date: LocalDate,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: LocalDate,
    @SerializedName(value = "is_archived", alternate = ["isArchived"])
    val isArchived: Boolean,
    @SerializedName(value = "important_date_value", alternate = ["importantDate"])
    val importantDate: String,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: LocalDate,
)