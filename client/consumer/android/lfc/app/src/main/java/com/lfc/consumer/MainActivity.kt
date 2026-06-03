package com.lfc.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lfc.consumer.data.local.SearchHistoryStore
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.ui.auth.AuthViewModel
import com.lfc.consumer.ui.auth.AuthViewModelFactory
import com.lfc.consumer.ui.auth.LoginScreen
import com.lfc.consumer.ui.auth.RegisterScreen
import com.lfc.consumer.ui.home.HomeScreen
import com.lfc.consumer.ui.home.HomeViewModel
import com.lfc.consumer.ui.home.HomeViewModelFactory
import com.lfc.consumer.ui.theme.LfcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val searchHistoryStore = SearchHistoryStore(applicationContext)

        setContent {
            LfcTheme {
                val navController = rememberNavController()
                val isLoggedIn by tokenManager.isLoggedInFlow.collectAsState(initial = false)
                val scope = rememberCoroutineScope()

                val startDestination = if (isLoggedIn) "home" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    composable("login") {
                        val authViewModel: AuthViewModel = viewModel(
                            factory = AuthViewModelFactory(tokenManager, applicationContext),
                        )
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            },
                            viewModel = authViewModel,
                        )
                    }
                    composable("register") {
                        val authViewModel: AuthViewModel = viewModel(
                            factory = AuthViewModelFactory(tokenManager, applicationContext),
                        )
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() },
                            viewModel = authViewModel,
                        )
                    }
                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModelFactory(
                                tokenManager,
                                searchHistoryStore,
                                applicationContext,
                            ),
                        )
                        HomeScreen(
                            onLogout = {
                                scope.launch {
                                    tokenManager.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            },
                            viewModel = homeViewModel,
                        )
                    }
                }
            }
        }
    }
}
