package com.looker.droidify.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.looker.droidify.compose.appDetail.navigation.appDetail
import com.looker.droidify.compose.appDetail.navigation.navigateToAppDetail
import com.looker.droidify.compose.appList.navigation.AppList
import com.looker.droidify.compose.appList.navigation.appList
import com.looker.droidify.compose.appList.navigation.navigateToAppList
import com.looker.droidify.compose.home.navigation.home
import com.looker.droidify.compose.repoDetail.navigation.navigateToRepoDetail
import com.looker.droidify.compose.repoDetail.navigation.repoDetail
import com.looker.droidify.compose.repoEdit.navigation.navigateToRepoEdit
import com.looker.droidify.compose.repoEdit.navigation.repoEdit
import com.looker.droidify.compose.repoList.navigation.navigateToRepoList
import com.looker.droidify.compose.repoList.navigation.repoList
import com.looker.droidify.compose.settings.navigation.navigateToSettings
import com.looker.droidify.compose.settings.navigation.settings
import com.looker.droidify.compose.theme.DroidifyTheme
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.requestNotificationPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {

    @Inject
    lateinit var repository: RepoRepository

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (repository.repos.first().isEmpty()) {
                Repository.defaultRepositories.forEach {
                    repository.insertRepo(it.address, it.fingerprint, null, null, it.name, it.description)
                }
            }
        }
        enableEdgeToEdge()
        requestNotificationPermission(request = notificationPermission::launch)
        setContent {
            DroidifyTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main content
                        NavHost(
                            modifier = Modifier.padding(innerPadding),
                            navController = navController,
                            startDestination = AppList,
                        ) {
                            home(
                                onNavigateToApps = { navController.navigateToAppList() },
                                onNavigateToRepos = { navController.navigateToRepoList() },
                                onNavigateToSettings = { navController.navigateToSettings() },
                            )
                            appList(
                                onAppClick = { packageName ->
                                    navController.navigateToAppDetail(packageName)
                                },
                                onNavigateToRepos = { navController.navigateToRepoList() },
                                onNavigateToSettings = { navController.navigateToSettings() },
                            )

                            repoList(
                                onRepoClick = { repoId -> navController.navigateToRepoDetail(repoId) },
                                onBackClick = { navController.popBackStack() }
                            )

                            appDetail(
                                onBackClick = { navController.popBackStack() },
                            )

                            repoDetail(
                                onBackClick = { navController.popBackStack() },
                                onEditClick = { repoId ->
                                    navController.navigateToRepoEdit(repoId)
                                },
                            )

                            repoEdit(onBackClick = { navController.popBackStack() })

                            settings(onBackClick = { navController.popBackStack() })
                        }
                        
                        // Simple swipe back detection
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    var startX = 0f
                                    var startY = 0f
                                    
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull() ?: continue
                                        
                                        if (change.pressed && startX == 0f) {
                                            startX = change.position.x
                                            startY = change.position.y
                                        }
                                        
                                        if (!change.pressed && startX != 0f) {
                                            val endX = change.position.x
                                            val deltaX = endX - startX
                                            val deltaY = kotlin.math.abs(change.position.y - startY)
                                            
                                            // Swipe right > 100px and not too much vertical movement
                                            if (deltaX > 100 && deltaY < 200) {
                                                if (!navController.popBackStack()) {
                                                    finish()
                                                }
                                            }
                                            startX = 0f
                                            startY = 0f
                                        }
                                        
                                        change.consume()
                                    }
                                }
                        )
                    }
                    
                    // System back button compatibility
                    BackHandler(enabled = true) {
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    }
                }
            }
        }
    }
}
