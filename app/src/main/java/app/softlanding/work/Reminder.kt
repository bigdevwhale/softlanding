package app.softlanding.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.softlanding.MainActivity
import app.softlanding.R
import app.softlanding.container
import app.softlanding.domain.Days
import app.softlanding.domain.Norm
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** One gentle evening note when the day is still almost empty: the balloon is up there with nothing written down. */
class Reminder(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.now()
        if (!s.remind || !s.onboarded) return Result.success()
        val today = Days.of(System.currentTimeMillis(), s.dayStart)
        val eaten = c.entries.day(today).sumOf { it.kcal }
        val norm = Norm.of(s.profile, s.manualKcal)
        if (eaten < norm.lo * .6) notify(applicationContext, eaten == 0.0)
        schedule(applicationContext, true)
        return Result.success()
    }

    companion object {
        private const val NAME = "evening"
        private const val CHANNEL = "evening"
        const val HOUR = 20
        const val MINUTE = 30

        fun schedule(ctx: Context, on: Boolean) {
            val wm = WorkManager.getInstance(ctx)
            if (!on) { wm.cancelUniqueWork(NAME); return }
            val now = LocalDateTime.now()
            var at = now.withHour(HOUR).withMinute(MINUTE).withSecond(0)
            if (!at.isAfter(now)) at = at.plusDays(1)
            val delay = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
            wm.enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<Reminder>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build())
        }

        fun notify(ctx: Context, empty: Boolean) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.n_channel), NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA, MainActivity.ADD).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            nm.notify(1, NotificationCompat.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(ctx.getString(if (empty) R.string.n_fog_t else R.string.n_light_t))
                .setContentText(ctx.getString(if (empty) R.string.n_fog_b else R.string.n_light_b))
                .setContentIntent(open).setAutoCancel(true).build())
        }
    }
}
