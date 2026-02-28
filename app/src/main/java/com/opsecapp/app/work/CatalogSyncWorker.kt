package com.opsecapp.app.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opsecapp.app.OpsecApplication
import com.opsecapp.app.R
import com.opsecapp.domain.model.TrustStatus

class CatalogSyncWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val container = (applicationContext as OpsecApplication).appContainer
    val status = container.syncCatalogUseCase(force = false)

    return when (status) {
      TrustStatus.TRUSTED -> {
        notify("Catalog updated", "Signed catalog verified and synced.")
        Result.success()
      }

      TrustStatus.INVALID_SIGNATURE,
      TrustStatus.INVALID_FINGERPRINT -> {
        notify("Catalog rejected", "Signature verification failed. Keeping last trusted catalog.")
        Result.failure()
      }

      TrustStatus.NETWORK_ERROR -> Result.retry()
      else -> Result.failure()
    }
  }

  private fun notify(title: String, body: String) {
    val manager = applicationContext.getSystemService(NotificationManager::class.java)
    val notification = NotificationCompat.Builder(applicationContext, OpsecApplication.CATALOG_SYNC_CHANNEL)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .build()

    manager.notify(NOTIFICATION_ID, notification)
  }

  companion object {
    const val UNIQUE_WORK_NAME = "catalog_periodic_sync"
    private const val NOTIFICATION_ID = 44
  }
}
