package com.diabdata.feature.settings.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diabdata.core.model.UserDetails
import com.diabdata.core.model.UserPreferences
import com.diabdata.shared.utils.dataTypes.BackupFrequency
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getUserPreferences(): Flow<UserPreferences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: UserPreferences)

    @Query("UPDATE user_preferences SET automaticBackupEnabled = :enabled WHERE id = 1")
    suspend fun setAutoBackupEnabled(enabled: Boolean)

    @Query("UPDATE user_preferences SET frequency = :frequency WHERE id = 1")
    suspend fun setFrequency(frequency: String)

    @Query("UPDATE user_preferences SET backupPath = :path WHERE id = 1")
    suspend fun setBackupPath(path: String)

    @Query("UPDATE user_preferences SET lastBackupDate = :date WHERE id = 1")
    suspend fun setLastBackupDate(date: String)

    @Query("UPDATE user_preferences SET backupPath = NULL, lastBackupDate = NULL, frequency = :defaultFrequency, automaticBackupEnabled = 0 WHERE id = 1")
    suspend fun resetBackupPreferences(defaultFrequency: String = BackupFrequency.WEEKLY.key)

}