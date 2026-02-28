package com.opsecapp.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.opsecapp.app.di.AppContainer
import com.opsecapp.app.work.CatalogSyncWorker
import java.util.concurrent.TimeUnit

class OpsecApplication : Application(), Configuration.Provider {
  lateinit var appContainer: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    appContainer = AppContainer(this)
    ensureNotificationChannel()
    schedulePeriodicSync()
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().build()

  private fun schedulePeriodicSync() {
    val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(6, TimeUnit.HOURS)
      .setBackoffCriteria(
        androidx.work.BackoffPolicy.EXPONENTIAL,
        30,
        TimeUnit.SECONDS
      )
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      CatalogSyncWorker.UNIQUE_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request
    )
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val manager = getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
      CATALOG_SYNC_CHANNEL,
      getString(R.string.catalog_sync_channel),
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = getString(R.string.catalog_sync_channel_desc)
    }
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val CATALOG_SYNC_CHANNEL = "catalog_sync"
  }
}
