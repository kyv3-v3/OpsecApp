plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("ANDROID_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val appVersionName = System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"
val appVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
val hasReleaseSigning =
  !releaseKeystorePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
  namespace = "com.opsecapp.app"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.opsecapp.app"
    minSdk = 26
    targetSdk = 35
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "DEFAULT_CATALOG_BASE_URL", "\"https://raw.githubusercontent.com/kyv3-v3/opsec-catalog/main\"")
    buildConfigField(
      "String",
      "CATALOG_PUBLIC_KEY_PEM",
      "\"-----BEGIN PUBLIC KEY-----\\nMCowBQYDK2VwAyEAIa3HLalRyhxAxmYitD8cha8J7LxhmSQaGH2bgXlMvZw=\\n-----END PUBLIC KEY-----\""
    )
    buildConfigField(
      "String",
      "CATALOG_PUBLIC_KEY_FINGERPRINT_SHA256",
      "\"eca188f20889dbd9466fdabdc14a5836fb9cfe3b9328842dcffb1d299d9a2099\""
    )
    buildConfigField(
      "String",
      "APP_RELEASES_LATEST_API_URL",
      "\"https://api.github.com/repos/kyv3-v3/OpsecApp/releases/latest\""
    )
    buildConfigField(
      "String",
      "APP_RELEASES_PAGE_URL",
      "\"https://github.com/kyv3-v3/OpsecApp/releases\""
    )
  }

  signingConfigs {
    create("release") {
      if (hasReleaseSigning) {
        storeFile = file(releaseKeystorePath!!)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(project(":domain"))
  implementation(project(":data"))
  implementation(project(":network"))
  implementation(project(":security"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.material)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.coil.compose)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.truth)
  testImplementation(libs.turbine)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
