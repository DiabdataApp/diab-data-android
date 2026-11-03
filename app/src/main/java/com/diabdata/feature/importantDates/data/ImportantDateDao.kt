package com.diabdata.feature.importantDates.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.diabdata.core.model.ImportantDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportantDateDao {
    @Insert
    suspend fun insert(importantDate: ImportantDate)

    @Update
    suspend fun update(importantDate: ImportantDate)

    @Query("SELECT * FROM important_date_entries WHERE entryUuid = :uuid")
    suspend fun getByUuid(uuid: String): ImportantDate?

    @Query("DELETE FROM important_date_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM important_date_entries ORDER BY date DESC")
    fun getAllImportantDates(): Flow<List<ImportantDate>>
}