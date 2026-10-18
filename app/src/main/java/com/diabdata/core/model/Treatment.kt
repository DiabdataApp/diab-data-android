package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diabdata.shared.utils.dataTypes.TreatmentType
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import kotlin.time.Instant

@Entity(
    tableName = "treatments",
    indices = [Index(value = ["entryUuid"], unique = true)]
)
data class Treatment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("treatment_expiration_date", alternate = ["expirationDate"])
    val expirationDate: LocalDate,
    @SerializedName("treatment_name", alternate = ["name"])
    val name: String,
    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: Instant,
    @SerializedName("is_archived", alternate = ["isArchived"])
    val isArchived: Boolean,
    @SerializedName("treatment_type", alternate = ["type"])
    val type: TreatmentType,
    @SerializedName("updated_at", alternate = ["updatedAt"])
    val updatedAt: Instant,
    @SerializedName("entry_uuid")
    val entryUuid: String
)