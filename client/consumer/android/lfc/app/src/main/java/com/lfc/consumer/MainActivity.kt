package com.lfc.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lfc.consumer.ui.navigation.weChatEnterTransition
import com.lfc.consumer.ui.navigation.weChatExitTransition
import com.lfc.consumer.ui.navigation.weChatPopEnterTransition
import com.lfc.consumer.ui.navigation.weChatPopExitTransition
import com.lfc.consumer.data.local.LegalConsentStore
import com.lfc.consumer.data.local.FeedChannelStore
import com.lfc.consumer.data.local.SearchHistoryStore
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.location.AmapLocationHelper
import com.lfc.consumer.ui.auth.AuthViewModel
import com.lfc.consumer.ui.auth.AuthViewModelFactory
import com.lfc.consumer.ui.auth.LoginScreen
import com.lfc.consumer.ui.auth.RegisterScreen
import com.lfc.consumer.ui.home.HomeScreen
import com.lfc.consumer.ui.home.HomeViewModel
import com.lfc.consumer.ui.home.HomeViewModelFactory
import com.lfc.consumer.ui.legal.LegalConsentScreen
import com.lfc.consumer.ui.legal.LegalDocumentId
import com.lfc.consumer.ui.legal.LegalDocumentScreen
import com.lfc.consumer.ui.theme.LfcTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        com.lfc.consumer.payment.AlipayPaymentHost.attach(this)
    }

    override fun onDestroy() {
        com.lfc.consumer.payment.AlipayPaymentHost.detach(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.lfc.consumer.payment.AlipayPaymentHost.attach(this)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val searchHistoryStore = SearchHistoryStore(applicationContext)
        val feedChannelStore = FeedChannelStore(applicationContext)
        val legalConsentStore = LegalConsentStore(applicationContext)

        setContent {
            LfcTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isLoggedIn by tokenManager.isLoggedInFlow.collectAsState(initial = null)
                val agreedVersion by legalConsentStore.agreedVersionFlow.collectAsState(initial = null)
                val scope = rememberCoroutineScope()

                val agreedVersionState = agreedVersion
                val isLoggedInState = isLoggedIn
                if (agreedVersionState == null || isLoggedInState == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                    return@LfcTheme
                }

                val resolvedAgreedVersion = agreedVersionState
                val loggedIn = isLoggedInState
                val needsLegalConsent = when {
                    resolvedAgreedVersion >= LegalConsentStore.CURRENT_VERSION -> false
                    loggedIn && resolvedAgreedVersion == 0 -> false
                    else -> true
                }

                if (!needsLegalConsent) {
                    AmapLocationHelper.agreePrivacy(applicationContext)
                }

                LaunchedEffect(loggedIn, resolvedAgreedVersion) {
                    if (loggedIn && resolvedAgreedVersion < LegalConsentStore.CURRENT_VERSION) {
                        legalConsentStore.markAgreed()
                    }
                }

                val startDestination = when {
                    needsLegalConsent -> "legal_consent"
                    loggedIn -> "home"
                    else -> "login"
                }

                fun navigateToLogin(clearBackStack: Boolean = true) {
                    navController.navigate("login") {
                        if (clearBackStack) {
                            popUpTo(0) { inclusive = true }
                        }
                        launchSingleTop = true
                    }
                }

                LaunchedEffect(Unit) {
                    tokenManager.sessionExpiredEvents.collect {
                        navigateToLogin()
                    }
                }

                LaunchedEffect(loggedIn, needsLegalConsent, currentRoute) {
                    if (!needsLegalConsent && !loggedIn && currentRoute == "home") {
                        navigateToLogin()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = { weChatEnterTransition() },
                    exitTransition = { weChatExitTransition() },
                    popEnterTransition = { weChatPopEnterTransition() },
                    popExitTransition = { weChatPopExitTransition() },
                ) {
                    composable("legal_consent") {
                        LegalConsentScreen(
                            onAgreed = {
                                scope.launch {
                                    legalConsentStore.markAgreed()
                                    AmapLocationHelper.agreePrivacy(applicationContext)
                                    val target = if (tokenManager.getToken() != null) "home" else "login"
                                    navController.navigate(target) {
                                        popUpTo("legal_consent") { inclusive = true }
                                    }
                                }
                            },
                            onOpenDocument = { docId ->
                                navController.navigate("legal_document/${docId.routeKey}")
                            },
                        )
                    }
                    composable("legal_document/{docKey}") { backStackEntry ->
                        val docKey = backStackEntry.arguments?.getString("docKey")
                        val docId = docKey?.let { LegalDocumentId.fromRouteKey(it) }
                        if (docId == null) {
                            navController.popBackStack()
                        } else {
                            LegalDocumentScreen(
                                documentId = docId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                    composable("login") {
                        val authViewModel: AuthViewModel = viewModel(
                            factory = AuthViewModelFactory(tokenManager, applicationContext),
                        )
                        LoginScreen(
                            onLoginSuccess = {
                                scope.launch {
                                    legalConsentStore.markAgreed()
                                    AmapLocationHelper.agreePrivacy(applicationContext)
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            },
                            onOpenLegalDocument = { docId ->
                                navController.navigate("legal_document/${docId.routeKey}")
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
                                scope.launch {
                                    legalConsentStore.markAgreed()
                                    AmapLocationHelper.agreePrivacy(applicationContext)
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() },
                            onOpenLegalDocument = { docId ->
                                navController.navigate("legal_document/${docId.routeKey}")
                            },
                            viewModel = authViewModel,
                        )
                    }
                    composable("home") {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModelFactory(
                                tokenManager,
                                searchHistoryStore,
                                feedChannelStore,
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
