package com.opsecapp.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opsecapp.app.di.AppContainer
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.ui.category.CategoryScreen
import com.opsecapp.app.ui.category.CategoryViewModel
import com.opsecapp.app.ui.detail.DetailScreen
import com.opsecapp.app.ui.detail.DetailViewModel
import com.opsecapp.app.ui.home.HomeScreen
import com.opsecapp.app.ui.home.HomeViewModel
import com.opsecapp.app.ui.search.SearchScreen
import com.opsecapp.app.ui.search.SearchViewModel
import com.opsecapp.app.ui.settings.SettingsScreen
import com.opsecapp.app.ui.settings.SettingsViewModel
import com.opsecapp.domain.model.CatalogItem
import kotlinx.coroutines.flow.collectLatest

private object Routes {
  private const val ARG_ID = "id"
  private const val ARG_TITLE = "title"
  private const val ARG_BADGE = "badge"

  const val HOME = "home"
  const val SEARCH = "search"
  const val SETTINGS = "settings"
  const val CATEGORY = "category/{$ARG_ID}"
  const val DETAIL = "detail/{$ARG_ID}?$ARG_TITLE={$ARG_TITLE}&$ARG_BADGE={$ARG_BADGE}"

  fun categoryRoute(id: String): String = "category/${Uri.encode(id)}"

  fun detailRoute(item: CatalogItem): String {
    val encodedId = Uri.encode(item.id)
    val title = Uri.encode(item.titleExact)
    val badge = Uri.encode(item.badgeExact)
    return "detail/$encodedId?$ARG_TITLE=$title&$ARG_BADGE=$badge"
  }
}

@Composable
fun OpsecAppNavHost(container: AppContainer) {
  val navController = rememberNavController()
  val context = LocalContext.current

  fun launchBrowserIfHttps(url: String) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    if (uri.scheme != "https") return
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
  }

  NavHost(navController = navController, startDestination = Routes.HOME) {
    composable(Routes.HOME) {
      val vm: HomeViewModel = viewModel(factory = container.viewModelFactory)
      HomeScreen(
        stateFlow = vm.state,
        events = vm.events,
        onRefresh = vm::refresh,
        onCategoryClick = { id -> navController.navigate(Routes.categoryRoute(id)) },
        onItemClick = { item -> navController.navigate(Routes.detailRoute(item)) },
        onSearchClick = { navController.navigate(Routes.SEARCH) },
        onSettingsClick = { navController.navigate(Routes.SETTINGS) }
      )
    }

    composable(
      route = Routes.CATEGORY,
      arguments = listOf(navArgument("id") { type = NavType.StringType }),
      enterTransition = {
        slideIntoContainer(
          AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(180))
      },
      popExitTransition = {
        slideOutOfContainer(
          AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(260)
        ) + fadeOut(animationSpec = tween(180))
      }
    ) { backStackEntry ->
      val categoryId = backStackEntry.arguments?.getString("id").orEmpty()
      val vm: CategoryViewModel = viewModel(factory = container.viewModelFactory)
      LaunchedEffect(categoryId) { vm.load(categoryId) }
      CategoryScreen(
        stateFlow = vm.state,
        onBack = navController::popBackStack,
        onItemClick = { item -> navController.navigate(Routes.detailRoute(item)) }
      )
    }

    composable(
      route = Routes.SEARCH,
      enterTransition = {
        slideIntoContainer(
          AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(180))
      },
      popExitTransition = {
        slideOutOfContainer(
          AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(260)
        ) + fadeOut(animationSpec = tween(180))
      }
    ) {
      val vm: SearchViewModel = viewModel(factory = container.viewModelFactory)
      SearchScreen(
        stateFlow = vm.state,
        onBack = navController::popBackStack,
        onQueryChange = vm::onQueryChange,
        onSourceConfidenceChange = vm::onSourceConfidenceChange,
        onInstallTypeChange = vm::onInstallTypeChange,
        onItemClick = { item -> navController.navigate(Routes.detailRoute(item)) }
      )
    }

    composable(
      route = Routes.DETAIL,
      arguments = listOf(
        navArgument("id") { type = NavType.StringType },
        navArgument("title") { type = NavType.StringType; defaultValue = "" },
        navArgument("badge") { type = NavType.StringType; defaultValue = "" }
      ),
      enterTransition = {
        slideIntoContainer(
          AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(320)
        ) + fadeIn(animationSpec = tween(220))
      },
      popEnterTransition = {
        slideIntoContainer(
          AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(180))
      },
      popExitTransition = {
        slideOutOfContainer(
          AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(180))
      }
    ) { backStackEntry ->
      val vm: DetailViewModel = viewModel(factory = container.viewModelFactory)
      val id = backStackEntry.arguments?.getString("id").orEmpty()
      val previewTitle = backStackEntry.arguments?.getString("title")
      val previewBadge = backStackEntry.arguments?.getString("badge")
      LaunchedEffect(id, previewTitle, previewBadge) { vm.load(id, previewTitle, previewBadge) }

      LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
          when (event) {
            is InstallResult.NeedsUserAction -> {
              event.actionIntent?.let {
                context.startActivity(it)
              }
            }

            InstallResult.FdroidMissing -> {
              vm.promptInstallFdroid()
            }

            else -> Unit
          }
        }
      }

      DetailScreen(
        stateFlow = vm.state,
        events = vm.events,
        onBack = navController::popBackStack,
        onInstallClick = vm::installPreferred,
        onOpenLink = vm::openUpstreamLink,
        onOpenGithubRepo = vm::openGithubRepository,
        onOpenGithubReleases = vm::openGithubReleases
      )
    }

    composable(
      route = Routes.SETTINGS,
      enterTransition = {
        slideIntoContainer(
          AnimatedContentTransitionScope.SlideDirection.Left,
          animationSpec = tween(280)
        ) + fadeIn(animationSpec = tween(180))
      },
      popExitTransition = {
        slideOutOfContainer(
          AnimatedContentTransitionScope.SlideDirection.Right,
          animationSpec = tween(260)
        ) + fadeOut(animationSpec = tween(180))
      }
    ) {
      val vm: SettingsViewModel = viewModel(factory = container.viewModelFactory)
      LaunchedEffect(Unit) {
        vm.events.collectLatest { event ->
          when (event) {
            is InstallResult.NeedsUserAction -> {
              event.actionIntent?.let {
                context.startActivity(it)
              }
            }

            else -> Unit
          }
        }
      }

      SettingsScreen(
        stateFlow = vm.state,
        events = vm.events,
        onBack = navController::popBackStack,
        onCatalogUrlSave = vm::saveCatalogBaseUrl,
        onSyncNow = vm::syncNow,
        onCheckAppUpdates = vm::checkAppUpdates,
        onInstallAppUpdate = vm::installAppUpdate,
        onOpenReleasePage = { url -> launchBrowserIfHttps(url) }
      )
    }
  }
}
