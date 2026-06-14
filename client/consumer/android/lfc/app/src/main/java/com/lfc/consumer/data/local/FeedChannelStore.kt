package com.lfc.consumer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.feedChannelDataStore: DataStore<Preferences> by preferencesDataStore(name = "lfc_feed_channels")

/** 笔记频道（不含视频/直播/短剧） */
object FeedChannels {
    const val RECOMMEND = "推荐"

    val defaultMyChannels: List<String> = listOf(
        RECOMMEND,
        "旅行",
        "职场",
        "情感",
        "读书",
        "文化",
        "社科",
        "学习",
        "科学科普",
        "心理",
        "体育",
        "穿搭",
        "美食",
        "摄影",
        "户外",
        "校园生活",
        "护肤",
        "家居",
        "舞蹈",
        "手工",
    )

    val allChannels: List<String> = listOf(
        RECOMMEND,
        "旅行",
        "职场",
        "情感",
        "读书",
        "文化",
        "社科",
        "学习",
        "科学科普",
        "心理",
        "体育",
        "穿搭",
        "汽车",
        "美食",
        "摄影",
        "影视",
        "户外",
        "校园生活",
        "护肤",
        "家居",
        "舞蹈",
        "机车",
        "手工",
        "游戏",
        "科技数码",
        "壁纸",
        "婚礼",
        "竞技体育",
        "动漫",
        "艺术",
        "健身塑型",
        "露营",
        "好物",
        "活动",
        "生活",
    )

    fun recommendedFor(myChannels: List<String>): List<String> =
        allChannels.filter { channel -> channel !in myChannels }

    /** 发布笔记可选类型 */
    val publishCategories: List<String> =
        allChannels.filter { channel -> channel != RECOMMEND }
}

class FeedChannelStore(private val context: Context) {
    private val channelsKey = stringPreferencesKey("my_channels")

    val myChannelsFlow: Flow<List<String>> = context.feedChannelDataStore.data.map { prefs ->
        prefs[channelsKey]
            ?.split(CHANNEL_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: FeedChannels.defaultMyChannels
    }

    suspend fun saveMyChannels(channels: List<String>) {
        val normalized = channels
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .let { list ->
                if (FeedChannels.RECOMMEND in list) {
                    listOf(FeedChannels.RECOMMEND) + list.filter { it != FeedChannels.RECOMMEND }
                } else {
                    listOf(FeedChannels.RECOMMEND) + list
                }
            }
        context.feedChannelDataStore.edit { prefs ->
            prefs[channelsKey] = normalized.joinToString(CHANNEL_DELIMITER)
        }
    }

    suspend fun resetToDefault() {
        saveMyChannels(FeedChannels.defaultMyChannels)
    }

    companion object {
        private const val CHANNEL_DELIMITER = "\u0001"
    }
}
