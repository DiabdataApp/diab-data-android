package com.diabdata

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.diabdata.core.database.DataRepository
import com.diabdata.core.database.DiabDataDatabase
import com.diabdata.core.model.Weight
import com.diabdata.core.model.newEntryUuid
import com.diabdata.core.model.nowMillis
import com.diabdata.core.model.toLocalDate
import com.diabdata.feature.weight.data.WeightDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class DataRepositoryImportTest {
    private lateinit var db: DiabDataDatabase
    private lateinit var repository: DataRepository

    private val older = Instant.fromEpochMilliseconds(1_000)
    private val newer = Instant.fromEpochMilliseconds(2_000)

    private fun weight(
        entryUuid: String,
        id: Int = 0,
        value: Float = 70f,
        date: LocalDate = LocalDate.of(2026, 1, 15),
        createdAt: Instant = older,
        updatedAt: Instant = older,
    ): Weight {
        return Weight(
            entryUuid = entryUuid,
            id = id,
            value = value,
            date = date,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isArchived = false,
        )
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            DiabDataDatabase::class.java
        ).build()
        repository = DataRepository(
            weightDao = db.weightDao(),
            hba1cDao = db.hba1cDao(),
            importantDateDao = db.importantDateDao(),
            appointmentDao = db.appointmentDao(),
            treatmentDao = db.treatmentDao(),
            medicationDao = db.medicationDao(),
            medicalDevicesDao = db.medicalDevicesDao(),
            medicalDeviceInfo = db.medicalDevicesInfoDao(),
            userDetailsDao = db.userDetailsDao(),
            userPreferencesDao = db.userPreferencesDao(),
            database = db,
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun reimportingSameBackupTwiceDoesNotDuplicate() = runBlocking {
        val backup = listOf(
            weight(entryUuid = "weight-1", id = 1),
            weight(entryUuid = "weight-2", id = 2, value = 76f),
        )

        repository.importWeights(backup)
        repository.importWeights(backup)

        val allWeights = db.weightDao().getAllWeightsFlow().first()

        assertEquals(2, allWeights.size)
    }

    @Test
    @Throws(Exception::class)
    fun keepMoreRecentLocalEntry() = runBlocking {
        db.weightDao().insert(weight(entryUuid = "weight-a", id = 1, value = 70f, updatedAt = newer))
        repository.importWeights(listOf(weight(entryUuid = "weight-a", id = 1, value = 65f, updatedAt = older)))

        val allWeights = db.weightDao().getAllWeightsFlow().first()

        assertEquals(70f, allWeights.single().value)
    }

    @Test
    @Throws(Exception::class)
    fun keepNewerBackupWithPreservedLocalId() = runBlocking {
        db.weightDao().insert(weight(entryUuid = "weight-a", id = 1, value = 70f, updatedAt = older))
        repository.importWeights(listOf(weight(entryUuid = "weight-a", id = 42, value = 65f, updatedAt = newer)))

        val allWeights = db.weightDao().getAllWeightsFlow().first()
        val result = allWeights.single()
        assertEquals(1, result.id)
        assertEquals(65f, result.value)
    }

    @Test
    @Throws(Exception::class)
    fun keepLocalAndBackupWithDifferentUuids() = runBlocking {
        db.weightDao().insert(weight(entryUuid = "weight-a", id = 1, value = 70f))
        repository.importWeights(listOf(weight(entryUuid = "weight-b", id = 1, value = 80f)))

        val allWeights = db.weightDao().getAllWeightsFlow().first()
        val weightsList = allWeights.map {it.value}.sorted()

        assertEquals(2, allWeights.size)
        assertEquals(listOf(70f, 80f), weightsList)
    }

    @Test
    @Throws(Exception::class)
    fun failedImportRollsBackEverything() = runBlocking {
        try {
            repository.inTransaction {
                repository.importWeights(listOf(weight(entryUuid = "weight-a")))
                throw IllegalStateException("simulated failure")
            }
        } catch (e: IllegalStateException) {
            // expected: simulated failure
        }

        val allWeights = db.weightDao().getAllWeightsFlow().first()

        assertEquals(0, allWeights.size)
    }
}