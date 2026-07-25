package com.diabdata.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import java.time.LocalDateTime

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    val automaticBackupEnabled: Boolean = false,
    val frequency: String = BackupFrequency.WEEKLY.key,
    val lastBackupDate: String? = null,
    val backupPath: String? = null,
)