package com.opsecapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.opsecapp.app.navigation.OpsecAppNavHost
import com.opsecapp.app.ui.theme.OpsecTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val container = (application as OpsecApplication).appContainer

    setContent {
      OpsecTheme {
        Surface {
          OpsecAppNavHost(container)
        }
      }
    }
  }
}
