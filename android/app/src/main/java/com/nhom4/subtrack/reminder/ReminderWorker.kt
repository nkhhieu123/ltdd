package com.nhom4.subtrack.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nhom4.subtrack.MainActivity
import com.nhom4.subtrack.R
import com.nhom4.subtrack.SubTrackApp
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.domain.BillingMath
import com.nhom4.subtrack.ui.formatDate
import com.nhom4.subtrack.ui.formatMoney
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Chạy mỗi ngày: gửi thông báo cho gói đang trong khoảng nhắc trước hạn. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SubTrackApp
        if (!app.settings.settings.value.remindersEnabled) return Result.success()
        return runCatching {
            app.repository.refresh()
            val today = LocalDate.now()
            app.repository.subscriptions.value.orEmpty()
                .filter { shouldRemind(it, today) }
                .filter { app.settings.markReminded(it.id, it.nextPaymentDate.toEpochDay()) }
                .forEach { Reminders.notifyDue(applicationContext, it, BillingMath.daysUntil(it.nextPaymentDate, today)) }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        fun shouldRemind(sub: Subscription, today: LocalDate): Boolean {
            val before = sub.remindDaysBefore ?: return false
            if (sub.status != SubscriptionStatus.ACTIVE) return false
            val days = BillingMath.daysUntil(sub.nextPaymentDate, today)
            return days in 0..before.toLong()
        }
    }
}

object Reminders {
    private const val CHANNEL_ID = "payment_reminders"
    private const val PERIODIC_WORK = "daily_reminders"
    private const val ONE_TIME_WORK = "check_reminders_now"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    /** Kiểm tra ngay, ví dụ sau khi lưu một gói sắp đến hạn. */
    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notifyDue(context: Context, sub: Subscription, days: Long) {
        val money = formatMoney(sub.amount, sub.currency)
        val title = if (days == 0L) context.getString(R.string.notif_title_today, sub.name)
        else context.getString(R.string.notif_title, sub.name)
        val body = if (days == 0L) context.getString(R.string.notif_body_today, money)
        else context.resources.getQuantityString(R.plurals.notif_body, days.toInt(), money, days.toInt(), formatDate(sub.nextPaymentDate))
        post(context, sub.id.toInt(), title, body)
    }

    fun notifyTest(context: Context): Boolean =
        post(context, 0, context.getString(R.string.notif_test_title), context.getString(R.string.notif_test_body))

    @SuppressLint("MissingPermission") // đã kiểm tra bằng hasPermission ngay bên dưới
    private fun post(context: Context, id: Int, title: String, body: String): Boolean {
        if (!hasPermission(context)) return false
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        return runCatching { NotificationManagerCompat.from(context).notify(id, notification) }.isSuccess
    }
}
