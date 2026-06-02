package com.lfc.consumer.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.ui.theme.XhsRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingPost by remember { mutableStateOf<PostDto?>(null) }
    var editingActivity by remember { mutableStateOf<ActivityDto?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
            isSubmitting = false
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
            isSubmitting = false
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) {
            viewModel.refreshMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.fillMaxSize(),
        ) {
            composable("main") {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        XhsBottomBar(
                            selectedTab = selectedTab,
                            unreadCount = uiState.unreadCount,
                            onTabSelected = { selectedTab = it },
                            onPublishClick = {
                                editingPost = null
                                editingActivity = null
                                navController.navigate("publish_hub")
                            },
                        )
                    },
                ) { padding ->
                    when (selectedTab) {
                        0 -> DiscoverFeedScreen(
                            feedState = uiState.feed,
                            onRefresh = { viewModel.loadFeed(refresh = true) },
                            onLoadMore = viewModel::loadMoreFeed,
                            onTabSelected = viewModel::selectFeedTab,
                            onSearchClick = { navController.navigate("search") },
                            onPostClick = { postId ->
                                navController.navigate("post_detail/$postId")
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                        1 -> ActivityFeedScreen(
                            activities = uiState.activities,
                            onJoin = viewModel::joinActivity,
                            onActivityClick = { activityId ->
                                navController.navigate("activity_detail/$activityId")
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                        2 -> MessageListScreen(
                            messages = uiState.messages,
                            unreadCount = uiState.unreadCount,
                            onMessageClick = { message ->
                                navController.navigate("message_detail/${message.id}")
                            },
                            onMarkAllRead = viewModel::markAllMessagesRead,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                        3 -> ProfileScreen(
                            userSession = userSession,
                            myPosts = uiState.myPosts,
                            myActivities = uiState.myActivities,
                            participationCount = uiState.myParticipations.size,
                            onEditPost = { post ->
                                editingPost = post
                                navController.navigate("edit_post")
                            },
                            onViewPost = { postId ->
                                navController.navigate("post_detail/$postId")
                            },
                            onDeletePost = viewModel::deletePost,
                            onEditActivity = { activity ->
                                editingActivity = activity
                                navController.navigate("edit_activity")
                            },
                            onViewActivity = { activityId ->
                                navController.navigate("activity_detail/$activityId")
                            },
                            onDeleteActivity = viewModel::deleteActivity,
                            onLogout = onLogout,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                    }
                }
            }

            composable("search") {
                SearchPageScreen(
                    searchInput = uiState.searchInput,
                    onSearchInputChange = viewModel::updateSearchInput,
                    searchHistory = searchHistory,
                    onSearch = { keyword ->
                        viewModel.submitSearch(keyword) {
                            navController.navigate("search_result") {
                                popUpTo("search") { inclusive = true }
                            }
                        }
                    },
                    onClearHistory = viewModel::clearSearchHistory,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("search_result") {
                SearchResultScreen(
                    searchState = uiState.search,
                    searchInput = uiState.searchInput,
                    onSearchInputChange = viewModel::updateSearchInput,
                    onSearch = { keyword ->
                        viewModel.submitSearch(keyword) { }
                    },
                    onTabSelected = viewModel::selectSearchTab,
                    onRefresh = { viewModel.loadSearchResults(refresh = true) },
                    onLoadMore = viewModel::loadMoreSearchResults,
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    },
                    onActivityClick = { activityId ->
                        navController.navigate("activity_detail/$activityId")
                    },
                    onBack = {
                        viewModel.clearSearch()
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = "message_detail/{messageId}",
                arguments = listOf(navArgument("messageId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val messageId = backStackEntry.arguments?.getInt("messageId") ?: return@composable
                LaunchedEffect(messageId) {
                    viewModel.loadMessageDetail(messageId)
                }
                MessageDetailScreen(
                    message = uiState.selectedMessage,
                    isLoading = uiState.isMessageLoading,
                    onBack = {
                        viewModel.clearSelectedMessage()
                        navController.popBackStack()
                    },
                    onActivityClick = { activityId ->
                        navController.navigate("activity_detail/$activityId")
                    },
                )
            }

            composable(
                route = "post_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId") ?: return@composable
                LaunchedEffect(postId) {
                    viewModel.loadPostDetail(postId)
                }
                PostDetailScreen(
                    post = uiState.selectedPost,
                    isLoading = uiState.isPostLoading,
                    onBack = {
                        viewModel.clearSelectedPost()
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = "activity_detail/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getInt("activityId") ?: return@composable
                LaunchedEffect(activityId) {
                    viewModel.loadActivityDetail(activityId)
                }
                ActivityDetailScreen(
                    activity = uiState.selectedActivity,
                    isLoading = uiState.isActivityLoading,
                    isJoining = uiState.isJoiningActivity,
                    onBack = {
                        viewModel.clearSelectedActivity()
                        navController.popBackStack()
                    },
                    onJoin = { viewModel.joinActivity(activityId) },
                )
            }

            composable("publish_hub") {
                PublishHubScreen(
                    onBack = { navController.popBackStack() },
                    onPublishPost = { navController.navigate("publish_post") },
                    onPublishActivity = { navController.navigate("publish_activity") },
                )
            }

            composable("publish_post") {
                PublishPostScreen(
                    isSubmitting = isSubmitting,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, content ->
                        isSubmitting = true
                        viewModel.createPost(
                            title = title,
                            content = content,
                            onSuccess = {
                                navController.popBackStack("main", inclusive = false)
                                selectedTab = 3
                            },
                            onComplete = { isSubmitting = false },
                        )
                    },
                )
            }

            composable("publish_activity") {
                PublishActivityScreen(
                    isSubmitting = isSubmitting,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, description, location, startTime, endTime, maxParticipants ->
                        isSubmitting = true
                        viewModel.createActivity(
                            title = title,
                            description = description,
                            location = location,
                            startTime = startTime,
                            endTime = endTime,
                            maxParticipants = maxParticipants,
                            onSuccess = {
                                navController.popBackStack("main", inclusive = false)
                                selectedTab = 2
                            },
                            onComplete = { isSubmitting = false },
                        )
                    },
                )
            }

            composable("edit_post") {
                PublishPostScreen(
                    initial = editingPost,
                    isSubmitting = isSubmitting,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, content ->
                        editingPost?.let { post ->
                            isSubmitting = true
                            viewModel.updatePost(
                                id = post.id,
                                title = title,
                                content = content,
                                onSuccess = { navController.popBackStack() },
                                onComplete = { isSubmitting = false },
                            )
                        }
                    },
                )
            }

            composable("edit_activity") {
                PublishActivityScreen(
                    initial = editingActivity,
                    isSubmitting = isSubmitting,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, description, location, startTime, endTime, maxParticipants ->
                        editingActivity?.let { activity ->
                            isSubmitting = true
                            viewModel.updateActivity(
                                id = activity.id,
                                title = title,
                                description = description,
                                location = location,
                                startTime = startTime,
                                endTime = endTime,
                                maxParticipants = maxParticipants,
                                onSuccess = { navController.popBackStack() },
                                onComplete = { isSubmitting = false },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun XhsPageTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = XhsRed,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
        )
    }
}
