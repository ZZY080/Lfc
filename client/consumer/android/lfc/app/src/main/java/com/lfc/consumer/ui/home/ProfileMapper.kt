package com.lfc.consumer.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lfc.consumer.data.model.UserProfileDto
import com.lfc.consumer.data.model.displayName

sealed class ProfileScanTarget {
    data class ById(val userId: Int) : ProfileScanTarget()
    data class ByLfcNo(val lfcNo: String) : ProfileScanTarget()
}

object ProfileShareHelper {
    private val legacyProfileUrlPattern = Regex(
        pattern = """(?:https?://)?lfc\.campus/user/(\d+)""",
        option = RegexOption.IGNORE_CASE,
    )
    private val lfcProfileUrlPattern = Regex(
        pattern = """(?:https?://)?lfc\.campus/lfc/([1-9]\d{9})""",
        option = RegexOption.IGNORE_CASE,
    )
    private val lfcNoPattern = Regex("""^[1-9]\d{9}$""")

    fun profileLink(lfcNo: String): String = "https://lfc.campus/lfc/$lfcNo"

    fun parseProfileScanTarget(content: String): ProfileScanTarget? {
        val trimmed = content.trim()
        lfcProfileUrlPattern.find(trimmed)?.groupValues?.getOrNull(1)?.let { lfcNo ->
            return ProfileScanTarget.ByLfcNo(lfcNo)
        }
        legacyProfileUrlPattern.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { userId ->
            return ProfileScanTarget.ById(userId)
        }
        if (trimmed.matches(lfcNoPattern)) {
            return ProfileScanTarget.ByLfcNo(trimmed)
        }
        return trimmed.toIntOrNull()?.takeIf { it in 1..999_999_999 }?.let { ProfileScanTarget.ById(it) }
    }

    fun shareProfile(context: Context, profile: UserProfileDto) {
        val text = buildString {
            append(profile.displayName())
            append(" 的莲峰校园主页\n")
            append(profileLink(profile.lfcNo))
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享主页"))
    }

    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE,
                )
            }
        }
        return bitmap
    }
}

fun UserProfileDto.toXhsProfileData(): XhsProfileData = XhsProfileData(
    userId = id,
    lfcNo = lfcNo,
    displayName = displayName(),
    bio = bio ?: "莲峰校园 · 记录校园生活",
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
    postCount = postCount,
    followingCount = followingCount,
    followerCount = followerCount,
    likeAndFavoriteCount = likeAndFavoriteCount,
    posts = posts.orEmpty(),
    activities = activities.orEmpty(),
    participationCount = participationCount,
    isFollowing = isFollowing,
    showCommentsPublic = showCommentsPublic,
    showFavoritesPublic = showFavoritesPublic,
    showLikesPublic = showLikesPublic,
    alipayBound = alipayBound,
    alipayLoginIdMasked = alipayLoginIdMasked,
)
