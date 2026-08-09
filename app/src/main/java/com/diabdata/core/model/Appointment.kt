package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diabdata.shared.utils.dataTypes.AppointmentType
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName(value = "appointment_date", alternate = ["date"])
    val date: LocalDateTime,
    @SerializedName("doctor_name", alternate = ["doctor"])
    val doctor: String,
    @SerializedName("appointment_type", alternate = ["type"])
    val type: AppointmentType,
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: LocalDate,
    @SerializedName("is_archived", alternate = ["isArchived"])
    val isArchived: Boolean,
    val notes: String?,
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updatedAt: LocalDate
)