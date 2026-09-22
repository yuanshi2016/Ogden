package com.example.ogdenkids

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * 每日复习提醒：AlarmManager + BroadcastReceiver，固定本地时区时刻（默认 19:00）。
 * 开关与小时存在 SharedPreferences（ogden-progress），重置进度时保留。
 */

object ReviewReminderPrefs {
    const val ENABLED_KEY = "reviewReminderEnabled"
    const val HOUR_KEY = "reviewReminderHour"
    const val DEFAULT_HOUR = 19

    fun isEnabled(prefs: android.content.SharedPreferences): Boolean =
        prefs.getBoolean(ENABLED_KEY, false)

    fun hour(prefs: android.content.SharedPreferences): Int =
        prefs.getInt(HOUR_KEY, DEFAULT_HOUR).coerceIn(0, 23)

    fun setEnabled(prefs: android.content.SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(ENABLED_KEY, enabled).commit()
    }

    fun setHour(prefs: android.content.SharedPreferences, hour: Int) {
        prefs.edit().putInt(HOUR_KEY, hour.coerceIn(0, 23)).commit()
    }
}

object ReviewReminderScheduler {
    const val ACTION = "com.example.ogdenkids.REVIEW_REMINDER"
    const val REQUEST_CODE = 1901
    const val CHANNEL_ID = "review_reminder"
    const val NOTIFICATION_ID = 1901

    fun schedule(context: Context, hour: Int = ReviewReminderPrefs.DEFAULT_HOUR) {
        val app = context.applicationContext
        ensureChannel(app)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(app)
        val triggerAt = nextTriggerMillis(System.currentTimeMillis(), hour)
        // setAndAllowWhileIdle：Doze 下仍可触发；不要求精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context.applicationContext))
    }

    fun rescheduleIfEnabled(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences("ogden-progress", Context.MODE_PRIVATE)
        if (ReviewReminderPrefs.isEnabled(prefs)) {
            schedule(context, ReviewReminderPrefs.hour(prefs))
        }
    }

    fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReviewReminderReceiver::class.java).setAction(ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /** 下一档本地 hour:00；若今天已过则明天 */
    fun nextTriggerMillis(nowMillis: Long, hour: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "每日复习提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "到点提醒复习单词"
        }
        nm.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

class ReviewReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReviewReminderScheduler.ACTION &&
            intent?.action != Intent.ACTION_BOOT_COMPLETED
        ) {
            return
        }
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("ogden-progress", Context.MODE_PRIVATE)
        if (!ReviewReminderPrefs.isEnabled(prefs)) return

        val hour = ReviewReminderPrefs.hour(prefs)
        // 开机或触发后都重新排下一档
        ReviewReminderScheduler.schedule(app, hour)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) return
        if (!ReviewReminderScheduler.hasNotificationPermission(app)) return

        val dueCount = dueCountFromPrefsSnapshot(prefs)
        val title = if (dueCount > 0) "今日有 $dueCount 个词待复习" else "该复习单词了"
        val text = if (dueCount > 0) "打开 Ogden Basic，把到期的词练一遍。" else "坚持每天一点点，单词记得更牢。"

        ReviewReminderScheduler.ensureChannel(app)
        val open = app.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: Intent(app, MainActivity::class.java)
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentPi = PendingIntent.getActivity(
            app,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val notification = NotificationCompat.Builder(app, ReviewReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ReviewReminderScheduler.NOTIFICATION_ID, notification)
    }

    /**
     * Receiver 里不宜再起 Room；用偏好里残留的旧键估算，或读不到时显示通用文案。
     * 真正精确计数在应用内 dueForReviewCount；通知文案允许近似。
     */
    private fun dueCountFromPrefsSnapshot(prefs: android.content.SharedPreferences): Int {
        // 轻量缓存：ProgressStore 每次 due 变化可写 reminderDueCount；没有则 0 → 通用文案
        return prefs.getInt("reminderDueCount", 0).coerceAtLeast(0)
    }
}

/** ProgressStore 调用：把待复习数写进 prefs，供通知文案使用 */
fun cacheReminderDueCount(prefs: android.content.SharedPreferences, count: Int) {
    prefs.edit().putInt("reminderDueCount", count.coerceAtLeast(0)).apply()
}
