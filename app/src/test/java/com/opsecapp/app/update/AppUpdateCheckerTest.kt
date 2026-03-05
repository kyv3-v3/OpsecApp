package com.opsecapp.app.update

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateCheckerTest {

  @Test
  fun returns_update_available_when_remote_tag_is_newer() = runTest {
    val body =
      """
      {
        "tag_name": "v1.2.3",
        "html_url": "https://github.com/kyv3-v3/OpsecApp/releases/tag/v1.2.3",
        "published_at": "2026-02-28T12:00:00Z",
        "assets": [
          {
            "name": "OpsecApp-1.2.3.apk",
            "browser_download_url": "https://github.com/kyv3-v3/OpsecApp/releases/download/v1.2.3/app.apk",
            "content_type": "application/vnd.android.package-archive"
          }
        ]
      }
      """.trimIndent()

    val checker = AppUpdateChecker(
      latestReleaseApiUrl = "https://example.com/releases/latest",
      currentVersionName = "1.0.0",
      fallbackReleasesPageUrl = "https://github.com/kyv3-v3/OpsecApp/releases",
      client = fakeClient(body)
    )

    val result = checker.checkLatestRelease()

    assertThat(result).isInstanceOf(AppUpdateCheckResult.UpdateAvailable::class.java)
    val update = result as AppUpdateCheckResult.UpdateAvailable
    assertThat(update.release.tagName).isEqualTo("v1.2.3")
    assertThat(update.release.apkDownloadUrl)
      .isEqualTo("https://github.com/kyv3-v3/OpsecApp/releases/download/v1.2.3/app.apk")
  }

  @Test
  fun returns_up_to_date_when_remote_tag_is_not_newer() = runTest {
    val body =
      """
      {
        "tag_name": "v1.0.0",
        "html_url": "https://github.com/kyv3-v3/OpsecApp/releases/tag/v1.0.0",
        "assets": []
      }
      """.trimIndent()

    val checker = AppUpdateChecker(
      latestReleaseApiUrl = "https://example.com/releases/latest",
      currentVersionName = "1.0.0",
      fallbackReleasesPageUrl = "https://github.com/kyv3-v3/OpsecApp/releases",
      client = fakeClient(body)
    )

    val result = checker.checkLatestRelease()

    assertThat(result).isInstanceOf(AppUpdateCheckResult.UpToDate::class.java)
    val upToDate = result as AppUpdateCheckResult.UpToDate
    assertThat(upToDate.latestVersion).isEqualTo("v1.0.0")
  }

  private fun fakeClient(responseBody: String): OkHttpClient {
    val mediaType = "application/json".toMediaType()
    return OkHttpClient.Builder()
      .addInterceptor { chain ->
        Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(responseBody.toResponseBody(mediaType))
          .build()
      }
      .build()
  }
}
