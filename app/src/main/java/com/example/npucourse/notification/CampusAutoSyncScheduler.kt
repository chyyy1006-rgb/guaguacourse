package com.example.npucourse.notification

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * 由 JobScheduler 持久化校园服务刷新任务。它在 Android 12+ 可以合法地从后台运行，
 * 不再依赖“非精确闹钟 -> 启动前台服务”这条会被系统拦截的链路。
 *
 * Android 会根据 Doze、网络与厂商省电策略合并任务，因此 30 分钟是目标间隔，
 * 不是秒级保证。每次任务都会检查各数据源自己的到期时间：电费 30 分钟，课表 24 小时。
 */
object CampusAutoSyncScheduler {
    private const val JOB_ID = 52_030
    private const val INTERVAL_MILLIS = 30L * 60L * 1000L
    private const val FLEX_MILLIS = 5L * 60L * 1000L

    fun schedule(context: Context): Boolean {
        // 后台刷新属于增强功能；任何厂商 JobScheduler 兼容问题都不能阻止主界面启动。
        return runCatching {
            val app = context.applicationContext
            val scheduler = app.getSystemService(JobScheduler::class.java)
            val alreadyScheduled = scheduler.allPendingJobs.any { it.id == JOB_ID }
            if (alreadyScheduled) return@runCatching true

            val builder = JobInfo.Builder(
                JOB_ID,
                ComponentName(app, CampusAutoSyncService::class.java)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setPeriodic(INTERVAL_MILLIS, FLEX_MILLIS)
            } else {
                builder.setPeriodic(INTERVAL_MILLIS)
            }

            scheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS
        }.getOrElse { error ->
            Log.e(TAG, "Unable to schedule campus auto sync", error)
            false
        }
    }

    private const val TAG = "CampusAutoSync"
}
