package com.diabdata

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.diabdata.core.database.DiabDataDatabase
import com.diabdata.core.database.migrations.ALL_MIGRATIONS
import com.diabdata.core.database.migrations.MIGRATION_20_21
import com.diabdata.core.database.migrations.MIGRATION_22_23
import com.diabdata.core.database.migrations.MIGRATION_23_24
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val diabDataTestDb = "migration-test"

    // Array of all migrations.
    private val migrationsList = ALL_MIGRATIONS

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        assetsFolder = DiabDataDatabase::class.java.canonicalName!!,
        openFactory = FrameworkSQLiteOpenHelperFactory()
    )

    fun SupportSQLiteDatabase.insertRow(table: String, vararg values: Pair<String, Any?>) {
        val cv = ContentValues()
        values.forEach { (k, v) ->
            when (v) {
                null -> cv.putNull(k)
                is Int -> cv.put(k, v)
                is String -> cv.put(k, v)
                is Long -> cv.put(k, v)
            }
        }
        insert(
            table = table, conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE, values = cv
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(diabDataTestDb, 19).use {}

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DiabDataDatabase::class.java,
            diabDataTestDb
        ).addMigrations(*migrationsList).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate20To21Test() {
        helper.createDatabase(diabDataTestDb, 20).use { db ->
            db.execSQL(
                """
                    INSERT INTO appointments 
                    (id, date, doctor, type, createdAt, isArchived, notes, updatedAt)
                    VALUES (1, '2026-01-15', 'Dr. Dre', 'APPOINTMENT', '2026-01-15', 0, '', '2026-01-15')
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(diabDataTestDb, 21, true, MIGRATION_20_21)
            .use { updatedDb ->
                updatedDb.query(
                    """
                    SELECT date FROM appointments
                    """.trimIndent()
                ).use { updatedAppointment ->
                    updatedAppointment.moveToFirst()

                    assertEquals(1, updatedAppointment.count)
                    assertEquals("2026-01-15T00:00", updatedAppointment.getString(0))
                }
            }
    }

    @Test
    @Throws(IOException::class)
    fun migrate22To23Test() {
        helper.createDatabase(diabDataTestDb, 22).use { db ->
            db.execSQL(
                """
                INSERT INTO user_preferences 
                (id, automaticBackupEnabled, frequency, lastBackupDate, backupPath)
                VALUES (1, 0, 'WEEKLY', NULL, NULL)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(diabDataTestDb, 23, true, MIGRATION_22_23)
            .use { updatedDb ->
                updatedDb.execSQL(
                    """
                    UPDATE user_preferences 
                    SET expirationReminder = 1, appointmentReminder = 0 
                    WHERE id = 1
                    """.trimIndent()
                )
                updatedDb.query(
                    """
                    SELECT expirationReminder, appointmentReminder 
                    FROM user_preferences 
                    WHERE id = 1
                    """.trimIndent()
                ).use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(1, cursor.count)
                    assertEquals(1, cursor.getInt(0))
                    assertEquals(0, cursor.getInt(1))
                }
            }
    }

    @Test
    @Throws(IOException::class)
    fun migrate23To24Test() {
        helper.createDatabase(diabDataTestDb, 23).use { db ->
            db.insertRow(
                "user_preferences",
                "id" to 1,
                "automaticBackupEnabled" to 0,
                "frequency" to "WEEKLY",
                "lastBackupDate" to null,
                "backupPath" to null,
                "expirationReminder" to 0,
                "appointmentReminder" to 0,
            )
        }

        helper.runMigrationsAndValidate(diabDataTestDb, 24, true, MIGRATION_23_24)
            .use { updatedDb ->
                updatedDb.query(
                    """
                SELECT id, backupEncryptionEnabled 
                FROM user_preferences
                """.trimIndent()
                ).use { cursor ->
                    assertEquals(1, cursor.count)
                    cursor.moveToFirst()
                    assertEquals(0, cursor.getInt(0))  // id corrigé à 0
                    assertEquals(0, cursor.getInt(1))  // valeur par défaut
                }
            }
    }
}