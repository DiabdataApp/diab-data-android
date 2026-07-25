package com.diabdata.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diabdata.core.database.migrations.ALL_MIGRATIONS
import com.diabdata.feature.appointments.data.AppointmentDao
import com.diabdata.feature.dataMatrixScanner.data.MedicationDao
import com.diabdata.feature.dataMatrixScanner.utils.MedicalDevicesInitializer
import com.diabdata.feature.dataMatrixScanner.utils.MedicationInitializer
import com.diabdata.feature.devices.data.MedicalDeviceDao
import com.diabdata.feature.devices.data.MedicalDevicesInfoDao
import com.diabdata.feature.hba1c.data.HBA1CDao
import com.diabdata.feature.importantDates.data.ImportantDateDao
import com.diabdata.feature.settings.data.UserPreferencesDao
import com.diabdata.feature.treatments.data.TreatmentDao
import com.diabdata.feature.userProfile.data.UserDetailsDao
import com.diabdata.feature.weight.data.WeightDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDiabDataDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<DiabDataDatabase>
    ): DiabDataDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            DiabDataDatabase::class.java,
            "diabdata_database"
        )
            .fallbackToDestructiveMigration(false)
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(object : Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)

                    CoroutineScope(Dispatchers.IO).launch {
                        val database = databaseProvider.get()
                        val countMedicationInfo = database.medicationDao().countAll()
                        val countDeviceInfo = database.medicalDevicesInfoDao().countAll()

                        if (countMedicationInfo == 0) {
                            MedicationInitializer(context, database).initialize()
                        }

                        if (countDeviceInfo == 0) {
                            MedicalDevicesInitializer(context, database).initialize()
                        }
                    }
                }
            })
            .build()
    }

    @Provides
    fun weightDao(db: DiabDataDatabase): WeightDao {
        return db.weightDao()
    }

    @Provides
    fun hba1cDao(db: DiabDataDatabase): HBA1CDao {
        return db.hba1cDao()
    }

    @Provides
    fun appointmentDao(db: DiabDataDatabase): AppointmentDao {
        return db.appointmentDao()
    }

    @Provides
    fun treatmentDao(db: DiabDataDatabase): TreatmentDao {
        return db.treatmentDao()
    }

    @Provides
    fun importantDateDao(db: DiabDataDatabase): ImportantDateDao {
        return db.importantDateDao()
    }

    @Provides
    fun medicationDao(db: DiabDataDatabase): MedicationDao {
        return db.medicationDao()
    }

    @Provides
    fun medicalDevicesDao(db: DiabDataDatabase): MedicalDeviceDao {
        return db.medicalDevicesDao()
    }

    @Provides
    fun medicalDevicesInfoDao(db: DiabDataDatabase): MedicalDevicesInfoDao {
        return db.medicalDevicesInfoDao()
    }

    @Provides
    fun userDetailsDao(db: DiabDataDatabase): UserDetailsDao {
        return db.userDetailsDao()
    }

    @Provides
    fun userPreferencesDao(db: DiabDataDatabase): UserPreferencesDao {
        return db.userPreferencesDao()
    }

    @Provides
    @Singleton
    fun dataRepository (
        weightDao: WeightDao,
        hba1cDao: HBA1CDao,
        appointmentDao: AppointmentDao,
        treatmentDao: TreatmentDao,
        importantDateDao: ImportantDateDao,
        medicationDao: MedicationDao,
        medicalDevicesDao: MedicalDeviceDao,
        medicalDeviceInfo: MedicalDevicesInfoDao,
        userDetailsDao: UserDetailsDao,
        userPreferencesDao: UserPreferencesDao,
        database: DiabDataDatabase
    ): DataRepository {
        val repository = DataRepository(
            weightDao = weightDao,
            hba1cDao = hba1cDao,
            appointmentDao = appointmentDao,
            treatmentDao = treatmentDao,
            importantDateDao = importantDateDao,
            medicationDao = medicationDao,
            medicalDevicesDao = medicalDevicesDao,
            medicalDeviceInfo = medicalDeviceInfo,
            userDetailsDao = userDetailsDao,
            userPreferencesDao = userPreferencesDao,
            database = database,
        )
        return repository
    }
}