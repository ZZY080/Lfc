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
import com.lfc.consumer.ui.navigation.weChatEnterTransition
import com.lfc.consumer.ui.navigation.weChatExitTransition
import com.lfc.consumer.ui.navigation.weChatPopEnterTransition
import com.lfc.consumer.ui.navigation.weChatPopExitTransition
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileTabUiState
import com.lfc.consumer.data.model.activityPromoteBidHint
import com.lfc.consumer.data.model.activityPromotePriceHint
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.postBoostBidHint
import com.lfc.consumer.data.model.postBoostPriceHint
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
    var showProfileSideMenu by remember { mutableStateOf(false) }
    var profileContentTab by remember { mutableIntStateOf(0) }
    var userProfileContentTab by remember { mutableIntStateOf(0) }

    val uiState by viewModel.uiState.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val context = LocalContext.current
    val searchHistory by viewModel.searchHistory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun openProfileQrScan() {
        showProfileSideMenu = false
        navController.navigate("profile_qr_scan")
    }

    fun handleProfileScanTarget(target: ProfileScanTarget) {
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
    }

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
            enterTransition = { weChatEnterTransition() },
            exitTransition = { weChatExitTransition() },
            popEnterTransition = { weChatPopEnterTransition() },
            popExitTransition = { weChatPopExitTransition() },
        ) {
            composable("main") {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { },
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
                            currentUserId = uiState.myProfile?.id,
                            isPaymentProcessing = uiState.isPaymentProcessing,
                            payingActivityId = uiState.payingActivityId,
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
                            favoriteActivities = uiState.profileFavoriteActivities,
                            likedPosts = uiState.profileLikedPosts,
                            likedActivities = uiState.profileLikedActivities,
                            comments = uiState.profileComments,
                            tabUiState = profileTabUiStateFor(
                                selectedTab = profileContentTab,
                                profileTabs = uiState.profileTabs,
                            ),
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
                            onScanProfile = ::openProfileQrScan,
                            onSettings = { navController.navigate("settings") },
                            onBindAlipay = { navController.navigate("settings") },
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
                    currentUserId = uiState.myProfile?.id ?: userSession?.userId,
                    currentUserLabel = uiState.myProfile?.displayName() ?: userSession?.studentId ?: "我",
                    currentUserAvatarUrl = uiState.myProfile?.avatarUrl,
                    isLoading = uiState.isChatLoading,
                    isSending = uiState.isChatSending,
                    onBack = {
                        viewModel.clearChat()
                        navController.popBackStack()
                    },
                    onSend = { content ->
                        viewModel.sendChatMessage(conversationId, content)
                    },
                    onSendMedia = { uri, type ->
                        viewModel.sendChatMedia(conversationId, uri, type)
                    },
                    onProductClick = { postId ->
                        navController.navigate("product_detail/$postId")
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
                    currentUserLabel = uiState.myProfile?.displayName() ?: userSession?.studentId ?: "我",
                    currentUserAvatarUrl = uiState.myProfile?.avatarUrl,
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
                    currentUserId = uiState.myProfile?.id ?: userSession?.userId,
                    onProductClick = { id -> navController.navigate("product_detail/$id") },
                    isPromotionSubmitting = uiState.isPromotionSubmitting,
                    onBoost = { viewModel.boostPost(postId) },
                    postBoostActionLabel = uiState.promotionConfig?.postActionLabel ?: "擦亮笔记",
                    postBoostActiveHint = "擦亮 ${uiState.promotionConfig?.postBoostHours ?: 48} 小时内推荐流优先展示",
                    postBoostPriceHint = uiState.selectedPost?.promotion?.price?.let { price ->
                        if (uiState.promotionConfig?.postSlotsFull == true) {
                            uiState.promotionConfig?.postBoostPriceHint()
                        } else {
                            "¥$price"
                        }
                    } ?: uiState.promotionConfig?.postBoostPriceHint(),
                    postBoostBidHint = uiState.promotionConfig?.postBoostBidHint(),
                )
            }

            composable(
                route = "product_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId") ?: return@composable
                LaunchedEffect(postId) {
                    viewModel.loadProductDetail(postId)
                }
                ProductDetailScreen(
                    post = uiState.selectedPost,
                    isLoading = uiState.isPostLoading,
                    isPurchasing = uiState.isPurchasingProduct,
                    isPaymentProcessing = uiState.isPaymentProcessing,
                    isConfirmingReceipt = uiState.isConfirmingReceipt,
                    platformFeeRateLabel = uiState.paymentConfig?.platformFeeRateLabel,
                    autoConfirmDays = uiState.paymentConfig?.autoConfirmDays ?: 7,
                    purchaseOrder = uiState.productPurchaseOrder,
                    currentUserId = uiState.myProfile?.id ?: userSession?.userId,
                    onBack = { navController.popBackStack() },
                    onViewNote = { navController.navigate("post_detail/$postId") },
                    onAuthorClick = { authorId -> navController.navigate("user_profile/$authorId") },
                    onContactSeller = {
                        uiState.selectedPost?.let { post ->
                            viewModel.startConversationWithProductFromPost(post) { conversationId ->
                                navController.navigate("chat/$conversationId")
                            }
                        }
                    },
                    onPurchase = { viewModel.purchasePostProduct(postId) },
                    onConfirmReceipt = {
                        uiState.productPurchaseOrder?.outTradeNo?.let { outTradeNo ->
                            viewModel.confirmProductReceipt(outTradeNo, postId)
                        }
                    },
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
                    favoriteActivities = uiState.profileFavoriteActivities,
                    likedPosts = uiState.profileLikedPosts,
                    likedActivities = uiState.profileLikedActivities,
                    comments = uiState.profileComments,
                    tabUiState = profileTabUiStateFor(
                        selectedTab = userProfileContentTab,
                        profileTabs = uiState.profileTabs,
                    ),
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

            composable("profile_search") {
                LaunchedEffect(Unit) {
                    viewModel.openProfileSearch()
                }
                ProfileMyNotesSearchScreen(
                    state = uiState.profileSearch,
                    onBack = { navController.popBackStack() },
                    onKeywordChange = viewModel::updateProfileSearchKeyword,
                    onSearch = viewModel::searchProfileNotes,
                    onClearKeyword = viewModel::clearProfileSearchKeyword,
                    onPostClick = { postId ->
                        navController.navigate("post_detail/$postId")
                    },
                )
            }

            composable("settings") {
                SettingsScreen(
                    profile = uiState.myProfile,
                    isUpdating = uiState.isPrivacyUpdating,
                    platformFeeRateLabel = uiState.paymentConfig?.platformFeeRateLabel,
                    onBack = { navController.popBackStack() },
                    onOpenOrders = {
                        navController.navigate("orders")
                    },
                    onOpenTransactions = {
                        navController.navigate("payment_transactions")
                    },
                    onPrivacyChange = viewModel::updatePrivacySettings,
                    onAuthorizeAlipay = viewModel::authorizeAlipayAccount,
                    onUnbindAlipay = viewModel::unbindAlipayAccount,
                )
            }

            composable("orders") {
                LaunchedEffect(Unit) {
                    viewModel.refreshOrderCenter()
                }
                OrderCenterScreen(
                    state = uiState.orderCenter,
                    isPaymentProcessing = uiState.isPaymentProcessing,
                    onBack = { navController.popBackStack() },
                    onOpenTransactions = {
                        navController.navigate("payment_transactions")
                    },
                    onTabSelected = viewModel::selectOrderTab,
                    onRefresh = { viewModel.loadOrders(refresh = true) },
                    onLoadMore = viewModel::loadMoreOrders,
                    onOrderClick = { order ->
                        when (order.bizType) {
                            "POST_PRODUCT_PURCHASE" -> navController.navigate("product_detail/${order.bizId}")
                            "ACTIVITY_JOIN" -> navController.navigate("activity_detail/${order.bizId}")
                            "POST_BOOST" -> navController.navigate("post_detail/${order.bizId}")
                            "ACTIVITY_PROMOTE" -> navController.navigate("activity_detail/${order.bizId}")
                        }
                    },
                    onPayOrder = viewModel::payOrderFromList,
                    onCancelOrder = viewModel::cancelOrderFromList,
                    onConfirmReceipt = viewModel::confirmReceiptFromList,
                    onReviewOrder = viewModel::submitOrderReview,
                    onApplyAfterSales = viewModel::applyOrderAfterSales,
                )
            }

            composable("payment_transactions") {
                LaunchedEffect(Unit) {
                    viewModel.refreshPaymentTransactionLedger()
                }
                PaymentTransactionLedgerScreen(
                    state = uiState.paymentTransactionLedger,
                    onBack = { navController.popBackStack() },
                    onRefresh = { viewModel.refreshPaymentTransactionLedger() },
                    onLoadMore = viewModel::loadMorePaymentTransactions,
                )
            }

            composable("my_qrcode") {
                MyQrCodeScreen(
                    profile = uiState.myProfile,
                    onBack = { navController.popBackStack() },
                )
            }

            composable("profile_qr_scan") {
                ProfileQrScanScreen(
                    onBack = { navController.popBackStack() },
                    onProfileScanned = { target ->
                        navController.popBackStack()
                        handleProfileScanTarget(target)
                    },
                    onInvalidCode = { message ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    onShowMyQr = {
                        navController.popBackStack()
                        navController.navigate("my_qrcode")
                    },
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
                    isPaymentProcessing = uiState.isPaymentProcessing,
                    isSocialSubmitting = uiState.isActivitySocialSubmitting,
                    currentUserId = uiState.myProfile?.id,
                    isAuthorFollowing = uiState.detailAuthorFollowing ?: false,
                    onBack = {
                        viewModel.clearSelectedActivity()
                        navController.popBackStack()
                    },
                    onJoin = { viewModel.joinActivity(activityId) },
                    onLike = { viewModel.toggleActivityLike(activityId) },
                    onFavorite = { viewModel.toggleActivityFavorite(activityId) },
                    onAuthorClick = { authorId ->
                        navController.navigate("user_profile/$authorId")
                    },
                    onFollowToggle = {
                        uiState.selectedActivity?.authorId?.let { viewModel.toggleDetailAuthorFollow(it) }
                    },
                    isPromotionSubmitting = uiState.isPromotionSubmitting,
                    onPromote = { viewModel.promoteActivity(activityId) },
                    activityPromoteActionLabel = uiState.promotionConfig?.activityActionLabel ?: "推广活动",
                    activityPromoteActiveHint = "推广 ${uiState.promotionConfig?.activityPromoteHours ?: 72} 小时内活动 Tab 优先展示",
                    activityPromotePriceHint = uiState.selectedActivity?.promotion?.price?.let { price ->
                        if (uiState.promotionConfig?.activitySlotsFull == true) {
                            uiState.promotionConfig?.activityPromotePriceHint()
                        } else {
                            "¥$price"
                        }
                    } ?: uiState.promotionConfig?.activityPromotePriceHint(),
                    activityPromoteBidHint = uiState.promotionConfig?.activityPromoteBidHint(),
                )
            }

            composable("publish_post") {
                PublishPostScreen(
                    isSubmitting = isSubmitting,
                    platformFeeRateLabel = uiState.paymentConfig?.platformFeeRateLabel,
                    alipayBound = uiState.myProfile?.alipayBound == true,
                    alipayLoginIdMasked = uiState.myProfile?.alipayLoginIdMasked,
                    onBack = { navController.popBackStack() },
                    onBindAlipay = { navController.navigate("settings") },
                    onSubmit = { title, content, imageUris, product ->
                        isSubmitting = true
                        viewModel.createPost(
                            title = title,
                            content = content,
                            imageUris = imageUris,
                            product = product,
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
                    platformFeeRateLabel = uiState.paymentConfig?.platformFeeRateLabel,
                    alipayBound = uiState.myProfile?.alipayBound == true,
                    onBack = { navController.popBackStack() },
                    onBindAlipay = { navController.navigate("settings") },
                    onSubmit = { title, description, location, latitude, longitude, startTime, endTime, maxParticipants, fee, imageUris ->
                        isSubmitting = true
                        viewModel.createActivity(
                            title = title,
                            description = description,
                            location = location,
                            latitude = latitude,
                            longitude = longitude,
                            startTime = startTime,
                            endTime = endTime,
                            maxParticipants = maxParticipants,
                            fee = fee,
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
                    platformFeeRateLabel = uiState.paymentConfig?.platformFeeRateLabel,
                    alipayBound = uiState.myProfile?.alipayBound == true,
                    alipayLoginIdMasked = uiState.myProfile?.alipayLoginIdMasked,
                    onBack = { navController.popBackStack() },
                    onBindAlipay = { navController.navigate("settings") },
                    onSubmit = { title, content, imageUris, _ ->
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
                    platformFeeRateLabel = uiState.paymentConfig?.platformFeeRateLabel,
                    alipayBound = uiState.myProfile?.alipayBound == true,
                    onBack = { navController.popBackStack() },
                    onBindAlipay = { navController.navigate("settings") },
                    onSubmit = { title, description, location, latitude, longitude, startTime, endTime, maxParticipants, _, imageUris ->
                        editingActivity?.let { activity ->
                            isSubmitting = true
                            viewModel.updateActivity(
                                id = activity.id,
                                title = title,
                                description = description,
                                location = location,
                                latitude = latitude,
                                longitude = longitude,
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
            onScan = ::openProfileQrScan,
            onShowMyQr = {
                showProfileSideMenu = false
                navController.navigate("my_qrcode")
            },
            onSettings = {
                showProfileSideMenu = false
                navController.navigate("settings")
            },
            onOrders = {
                showProfileSideMenu = false
                navController.navigate("orders")
            },
            onBindAlipay = {
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
            onSearch = {
                showProfileSideMenu = false
                navController.navigate("profile_search")
            },
            onLogout = onLogout,
            onSelectProfileTab = { tab ->
                profileContentTab = tab
                viewModel.loadProfileTab(tab)
            },
            onGoToMessages = { selectedTab = 2 },
            modifier = Modifier.zIndex(200f),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .zIndex(300f),
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
