package com.diabdata.workers.reminders

import android.content.Context
import androidx.work.WorkManager
import androidx.work.await
import com.diabdata.core.database.DataViewModel
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.diabdata.shared.R as shared

suspend fun scheduleAllReminders(context: Context, dataViewModel: DataViewModel) {
    val reminderOffsets = listOf(30, 14, 1)

    val appointments = dataViewModel.upcomingAppointment.first()
    val expirations = dataViewModel.upcomingExpiringTreatmentDates.first()

    appointments.forEach { appointment ->
        reminderOffsets.forEach { offset ->
            val notifyDate = appointment.date.minusDays(offset.toLong())

            val baseContent = context.getString(
                shared.string.appointments_notification_content_text,
                appointment.doctor,
                appointment.date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault())
                )
            )
            val content =
                if (appointment.notes?.isNotBlank() == true) "$baseContent\n- ${appointment.notes}" else baseContent

            scheduleNotification(
                context,
                title = context.getString(shared.string.appointments_notification_title_text),
                content = content,
                date = notifyDate,
                tag = "appointments"
            )
        }
    }

    expirations.forEach { treatment ->
        reminderOffsets.forEach { offset ->
            val notifyDate = treatment.expirationDate.minusDays(offset.toLong())
            scheduleNotification(
                context,
                title = context.getString(shared.string.medications_expiry_notification_title_text),
                content = context.getString(
                    shared.string.medications_expiry_notification_content_text,
                    treatment.name,
                    treatment.expirationDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(
                            Locale.getDefault()
                        )
                    )
                ),
                date = notifyDate.atTime(9, 0),
                tag = "treatments"
            )
        }
    }
}

suspend fun scheduleAppointmentReminders(context: Context, dataViewModel: DataViewModel) {
    val workManager = WorkManager.getInstance(context)
    workManager.cancelAllWorkByTag("appointments").await()
    workManager.pruneWork().await()

    val reminderOffsets = listOf(30, 14, 1)
    val appointments = dataViewModel.upcomingAppointment.first()

    appointments.forEach { appointment ->
        reminderOffsets.forEach { offset ->
            val notifyDate = appointment.date.minusDays(offset.toLong())
            val baseContent = context.getString(
                shared.string.appointments_notification_content_text,
                appointment.doctor,
                appointment.date.format(
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(Locale.getDefault())
                )
            )
            val content =
                if (appointment.notes?.isNotBlank() == true) "$baseContent\n- ${appointment.notes}" else baseContent

            scheduleNotification(
                context,
                title = context.getString(shared.string.appointments_notification_title_text),
                content = content,
                date = notifyDate,
                tag = "appointments"
            )
        }
    }
}

suspend fun scheduleMedicationExpirationReminders(context: Context, dataViewModel: DataViewModel) {
    val workManager = WorkManager.getInstance(context)
    workManager.cancelAllWorkByTag("treatments").await()
    workManager.pruneWork().await()

    val reminderOffsets = listOf(30, 14, 1)
    val expirations = dataViewModel.upcomingExpiringTreatmentDates.first()

    expirations.forEach { treatment ->
        reminderOffsets.forEach { offset ->
            val notifyDate = treatment.expirationDate.minusDays(offset.toLong())
            scheduleNotification(
                context,
                title = context.getString(shared.string.medications_expiry_notification_title_text),
                content = context.getString(
                    shared.string.medications_expiry_notification_content_text,
                    treatment.name,
                    treatment.expirationDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            .withLocale(Locale.getDefault())
                    )
                ),
                date = notifyDate.atTime(9, 0),
                tag = "treatments"
            )
        }
    }
}