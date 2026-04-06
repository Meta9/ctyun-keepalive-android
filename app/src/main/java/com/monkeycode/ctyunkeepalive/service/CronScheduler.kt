package com.monkeycode.ctyunkeepalive.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.monkeycode.ctyunkeepalive.core.AppConfig

class CronScheduler(
    private val context: Context,
) {

    companion object {
        private const val TAG = "CronScheduler"
        private const val REQUEST_CODE = 20020
    }

    fun schedule(targetContext: Context = context) {
        val alarmManager = targetContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(targetContext)
        val nextAt = System.currentTimeMillis() + AppConfig.fixedScheduleMinutes * 60_000L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(TAG, "schedule exact alarm at=$nextAt")
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextAt,
                        pendingIntent
                    )
                } else {
                    Log.w(TAG, "exact alarm not allowed, fallback to inexact alarm at=$nextAt")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextAt,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAt,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "failed to schedule exact alarm, fallback to inexact", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAt,
                    pendingIntent
                )
            } catch (fallbackError: Exception) {
                Log.e(TAG, "fallback alarm scheduling failed", fallbackError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "unexpected error while scheduling alarm", e)
        }
    }

    fun cancel(targetContext: Context = context) {
        val alarmManager = targetContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(targetContext))
    }

    fun canScheduleExactAlarms(targetContext: Context = context): Boolean {
        val alarmManager = targetContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
