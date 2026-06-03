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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileTabUiState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lfc.consumer.ui.theme.XhsRed
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isOnMainRoute = navBackStackEntry?.destination?.route == "main"
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingPost by remember { mutableStateOf<PostDto?>(null) }
    var editingActivity by remember { mutableStateOf<ActivityDto?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showPublishHub by remember { mutableStateOf(false) }
    var showProfileSearch by remember { mutableStateOf(false) }
    var showProfileSideMenu by remember { mutableStateOf(false) }
    var profileContentTab by remember { mutableIntStateOf(0) }
    var userProfileContentTab by remember { mutableIntStateOf(0) }

    val uiState by viewModel.uiState.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val context = LocalContext.current
    val searchHistory by viewModel.searchHistory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val launchProfileQrScan = rememberProfileQrScanner(
        onProfileScanned = { target ->
            coroutineScope.launch {
                val userId = when (target) {
                    is ProfileScanTarget.ById -> target.userId
                    is ProfileScanTarget.ByLfcNo -> viewModel.resolveUserIdByLfcNo(target.lfcNo)
                }
                if (userId != null) {
                    navController.navigate("user_profile/$userId")
                } else {
                    snackbarHostState.showSnackbar("无法识别该用户")
                }
            }
        },
        onError = { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
    )

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

    LaunchedEffect(selectedTab, uiState.myProfile?.id, uiState.profileTabs.targetUserId) {
        val myId = uiState.myProfile?.id ?: return@LaunchedEffect
        if (selectedTab == 3 && uiState.profileTabs.targetUserId != myId) {
            viewModel.loadProfileTab(profileContentTab, myId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.fillMaxSize(),
        ) {
            composable("main") {
                Box(modifier = Modifier.fillMaxSize()) {
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
                                    showPublishHub = true
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
                            feedState = uiState.activityFeed,
                            onRefresh = { viewModel.loadActivityFeed(refresh = true) },
                            onLoadMore = viewModel::loadMoreActivityFeed,
                            onJoin = viewModel::joinActivity,
                            onActivityClick = { activityId ->
                                navController.navigate("activity_detail/$activityId")
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                        2 -> ConversationListScreen(
                            conversations = uiState.conversations,
                            notifications = uiState.notifications,
                            unreadCount = uiState.unreadCount,
                            onConversationClick = { conversation ->
                                navController.navigate("chat/${conversation.id}")
                            },
                            onNotificationClick = { notification ->
                                navController.navigate("notification_detail/${notification.id}")
                            },
                            onMarkAllNotificationsRead = viewModel::markAllNotificationsRead,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                        3 -> ProfileScreen(
                            profile = uiState.myProfile,
                            profileNotes = uiState.profileNotes,
                            profileActivities = uiState.profileActivities,
                            favoritePosts = uiState.profileFavoritePosts,
                            likedPosts = uiState.profileLikedPosts,
                            comments = uiState.profileComments,
                            tabUiState = uiState.profileTabs.tabs.getOrElse(profileContentTab) {
                                ProfileTabUiState()
                            },
                            selectedContentTab = profileContentTab,
                            onRefresh = {
                                viewModel.refreshProfileTab(profileContentTab)
                            },
                            onLoadMore = {
                                viewModel.loadMoreProfileTab(profileContentTab)
                            },
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
                            onEditProfile = { navController.navigate("edit_profile") },
                            onShare = {
                                uiState.myProfile?.let { ProfileShareHelper.shareProfile(context, it) }
                            },
                            onShowQr = { navController.navigate("my_qrcode") },
                            onScanProfile = launchProfileQrScan,
                            onSearch = { showProfileSearch = true },
                            onSettings = { navController.navigate("settings") },
                            onGoToMessages = { selectedTab = 2 },
                            onOpenSideMenu = { showProfileSideMenu = true },
                            unreadCount = uiState.unreadCount,
                            onTabSelected = { tab ->
                                profileContentTab = tab
                                viewModel.loadProfileTab(tab)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding()),
                        )
                        }
                    }
                    if (showProfileSearch && uiState.myProfile != null) {
                        ProfileSearchDialog(
                            keyword = uiState.profileSearchKeyword,
                            onDismiss = { showProfileSearch = false },
                            onSearch = { keyword ->
                                showProfileSearch = false
                                viewModel.searchMyPosts(keyword)
                            },
                        )
                    }
                    if (showPublishHub) {
                        PublishHubScreen(
                            onBack = { showPublishHub = false },
                            onPublishPost = {
                                showPublishHub = false
                                navController.navigate("publish_post")
                            },
                            onPublishActivity = {
                                showPublishHub = false
                                navController.navigate("publish_activity")
                            },
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
                route = "notification_detail/{notificationId}",
                arguments = listOf(navArgument("notificationId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val notificationId = backStackEntry.arguments?.getInt("notificationId") ?: return@composable
                LaunchedEffect(notificationId) {
                    viewModel.loadNotificationDetail(notificationId)
                }
                MessageDetailScreen(
                    message = uiState.selectedNotification,
                    isLoading = uiState.isNotificationLoading,
                    onBack = {
                        viewModel.clearSelectedNotification()
                        navController.popBackStack()
                    },
                    onActivityClick = { activityId ->
                        navController.navigate("activity_detail/$activityId")
                    },
                )
            }

            composable(
                route = "chat/{conversationId}",
                arguments = listOf(navArgument("conversationId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getInt("conversationId") ?: return@composable
                LaunchedEffect(conversationId) {
                    viewModel.loadChat(conversationId)
                }
                ChatScreen(
                    conversation = uiState.selectedConversation,
                    messages = uiState.chatMessages,
                    currentUserId = userSession?.userId,
                    isLoading = uiState.isChatLoading,
                    isSending = uiState.isChatSending,
                    onBack = {
                        viewModel.clearChat()
                        navController.popBackStack()
                    },
                    onSend = { content ->
                        viewModel.sendChatMessage(conversationId, content)
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
                    comments = uiState.postComments,
                    currentUserLabel = userSession?.studentId ?: "我",
                    isLoading = uiState.isPostLoading,
                    isCommentsLoading = uiState.isPostCommentsLoading,
                    isSocialSubmitting = uiState.isPostSocialSubmitting,
                    onBack = {
                        viewModel.clearSelectedPost()
                        navController.popBackStack()
                    },
                    onLike = { viewModel.togglePostLike(postId) },
                    onFavorite = { viewModel.togglePostFavorite(postId) },
                    onSubmitComment = { content, parentId ->
                        viewModel.submitPostComment(postId, content, parentId)
                    },
                    onAuthorClick = { authorId ->
                        navController.navigate("user_profile/$authorId")
                    },
                    isAuthorFollowing = uiState.detailAuthorFollowing ?: false,
                    onFollowToggle = {
                        uiState.selectedPost?.authorId?.let { viewModel.toggleDetailAuthorFollow(it) }
                    },
                    currentUserId = userSession?.userId,
                )
            }

            composable(
                route = "user_profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
                LaunchedEffect(userId) {
                    userProfileContentTab = 0
                    viewModel.loadUserProfile(userId)
                }
                UserProfileScreen(
                    profile = uiState.selectedUserProfile,
                    isLoading = uiState.isUserProfileLoading,
                    isSelf = userSession?.userId == userId,
                    profileNotes = uiState.profileNotes,
                    profileActivities = uiState.profileActivities,
                    favoritePosts = uiState.profileFavoritePosts,
                    likedPosts = uiState.profileLikedPosts,
                    comments = uiState.profileComments,
                    tabUiState = uiState.profileTabs.tabs.getOrElse(userProfileContentTab) {
                        ProfileTabUiState()
                    },
                    selectedContentTab = userProfileContentTab,
                    onRefresh = { viewModel.refreshProfileTab(userProfileContentTab, userId) },
                    onLoadMore = { viewModel.loadMoreProfileTab(userProfileContentTab, userId) },
                    onBack = {
                        viewModel.clearUserProfile()
                        navController.popBackStack()
                    },
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    },
                    onActivityClick = { activityId ->
                        navController.navigate("activity_detail/$activityId")
                    },
                    onMessage = {
                        viewModel.startConversation(userId) { conversationId ->
                            navController.navigate("chat/$conversationId")
                        }
                    },
                    onFollowToggle = { viewModel.toggleFollow(userId) },
                    onShare = {
                        uiState.selectedUserProfile?.let { ProfileShareHelper.shareProfile(context, it) }
                    },
                    onTabSelected = { tab ->
                        userProfileContentTab = tab
                        viewModel.loadProfileTab(tab, userId)
                    },
                )
            }

            composable("settings") {
                SettingsScreen(
                    profile = uiState.myProfile,
                    isUpdating = uiState.isPrivacyUpdating,
                    onBack = { navController.popBackStack() },
                    onPrivacyChange = viewModel::updatePrivacySettings,
                )
            }

            composable("my_qrcode") {
                MyQrCodeScreen(
                    profile = uiState.myProfile,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("edit_profile") {
                EditProfileScreen(
                    profile = uiState.myProfile,
                    isSubmitting = uiState.isProfileUpdating,
                    onBack = { navController.popBackStack() },
                    onSubmit = { nickname, bio, avatarUri, coverUri ->
                        viewModel.updateProfile(
                            nickname = nickname,
                            bio = bio,
                            avatarUri = avatarUri,
                            coverUri = coverUri,
                            onSuccess = { navController.popBackStack() },
                            onComplete = { },
                        )
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
                    currentUserId = uiState.myProfile?.id,
                    isAuthorFollowing = uiState.detailAuthorFollowing ?: false,
                    onBack = {
                        viewModel.clearSelectedActivity()
                        navController.popBackStack()
                    },
                    onJoin = { viewModel.joinActivity(activityId) },
                    onAuthorClick = { authorId ->
                        navController.navigate("user_profile/$authorId")
                    },
                    onFollowToggle = {
                        uiState.selectedActivity?.authorId?.let { viewModel.toggleDetailAuthorFollow(it) }
                    },
                )
            }

            composable("publish_post") {
                PublishPostScreen(
                    isSubmitting = isSubmitting,
                    onBack = { navController.popBackStack() },
                    onSubmit = { title, content, imageUris ->
                        isSubmitting = true
                        viewModel.createPost(
                            title = title,
                            content = content,
                            imageUris = imageUris,
                            onSuccess = {
                                navController.popBackStack("main", inclusive = false)
                                selectedTab = 0
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
                    onSubmit = { title, description, location, startTime, endTime, maxParticipants, imageUris ->
                        isSubmitting = true
                        viewModel.createActivity(
                            title = title,
                            description = description,
                            location = location,
                            startTime = startTime,
                            endTime = endTime,
                            maxParticipants = maxParticipants,
                            imageUris = imageUris,
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
                    onSubmit = { title, content, imageUris ->
                        editingPost?.let { post ->
                            isSubmitting = true
                            viewModel.updatePost(
                                id = post.id,
                                title = title,
                                content = content,
                                imageUris = imageUris,
                                existingImages = post.images.orEmpty(),
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
                    onSubmit = { title, description, location, startTime, endTime, maxParticipants, imageUris ->
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
                                imageUris = imageUris,
                                existingImages = activity.images.orEmpty(),
                                onSuccess = { navController.popBackStack() },
                                onComplete = { isSubmitting = false },
                            )
                        }
                    },
                )
            }
        }

        ProfileSideMenuOverlay(
            visible = showProfileSideMenu && selectedTab == 3 && isOnMainRoute,
            profile = uiState.myProfile?.toXhsProfileData(),
            unreadCount = uiState.unreadCount,
            onDismiss = { showProfileSideMenu = false },
            onScan = launchProfileQrScan,
            onShowMyQr = {
                showProfileSideMenu = false
                navController.navigate("my_qrcode")
            },
            onSettings = {
                showProfileSideMenu = false
                navController.navigate("settings")
            },
            onEditProfile = {
                showProfileSideMenu = false
                navController.navigate("edit_profile")
            },
            onShare = {
                uiState.myProfile?.let { ProfileShareHelper.shareProfile(context, it) }
            },
            onSearch = { showProfileSearch = true },
            onLogout = onLogout,
            onSelectProfileTab = { tab ->
                profileContentTab = tab
                viewModel.loadProfileTab(tab)
            },
            onGoToMessages = { selectedTab = 2 },
            modifier = Modifier.zIndex(200f),
        )
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
