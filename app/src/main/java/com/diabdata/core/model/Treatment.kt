package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diabdata.shared.utils.dataTypes.TreatmentType
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "treatments")
data class Treatment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("treatment_expiration_date", alternate = ["expirationDate"])
    val expirationDate: LocalDate,
    @SerializedName("treatment_name", alternate = ["name"])
    val name: String,
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: LocalDate,
    @SerializedName("is_archived", alternate = ["isArchived"])
    val isArchived: Boolean,
    @SerializedName("treatment_type", alternate = ["type"])
    val type: TreatmentType,
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updatedAt: LocalDate
)