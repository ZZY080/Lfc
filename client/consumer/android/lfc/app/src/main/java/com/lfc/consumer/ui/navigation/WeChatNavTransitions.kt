package com.lfc.consumer.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val NAV_ANIM_DURATION_MS = 280
private const val BACKGROUND_PARALLAX_RATIO = 0.25f

/** 进入下一页：新页面从右侧滑入 */
fun weChatEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS),
        initialOffsetX = { fullWidth -> fullWidth },
    )

/** 进入下一页：当前页面向左轻微移出（微信 parallax 效果） */
fun weChatExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS),
        targetOffsetX = { fullWidth -> -(fullWidth * BACKGROUND_PARALLAX_RATIO).toInt() },
    )

/** 返回上一页：上一页从左轻微滑回 */
fun weChatPopEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS),
        initialOffsetX = { fullWidth -> -(fullWidth * BACKGROUND_PARALLAX_RATIO).toInt() },
    )

/** 返回上一页：当前页面向右滑出 */
fun weChatPopExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS),
        targetOffsetX = { fullWidth -> fullWidth },
    )
