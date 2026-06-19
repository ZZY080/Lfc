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

data class FeedChannelCatalog(
    val allChannels: List<String> = emptyList(),
    val defaultMyChannels: List<String> = emptyList(),
    val publishCategories: List<String> = emptyList(),
)

/** 发现页频道常量（名称以服务端 feed_channel 表为准） */
object FeedChannels {
    const val RECOMMEND = "推荐"
}

class FeedChannelStore(private val context: Context) {
    private val channelsKey = stringPreferencesKey("my_channels")
    private val allChannelsKey = stringPreferencesKey("all_channels")
    private val defaultMyKey = stringPreferencesKey("default_my_channels")
    private val publishCategoriesKey = stringPreferencesKey("publish_categories")

    val catalogFlow: Flow<FeedChannelCatalog> = context.feedChannelDataStore.data.map { prefs ->
        FeedChannelCatalog(
            allChannels = prefs[allChannelsKey]?.split(CHANNEL_DELIMITER)?.filter { it.isNotBlank() }
                ?: emptyList(),
            defaultMyChannels = prefs[defaultMyKey]?.split(CHANNEL_DELIMITER)?.filter { it.isNotBlank() }
                ?: emptyList(),
            publishCategories = prefs[publishCategoriesKey]?.split(CHANNEL_DELIMITER)?.filter { it.isNotBlank() }
                ?: emptyList(),
        )
    }

    val myChannelsFlow: Flow<List<String>> = context.feedChannelDataStore.data.map { prefs ->
        val catalog = FeedChannelCatalog(
            defaultMyChannels = prefs[defaultMyKey]?.split(CHANNEL_DELIMITER)?.filter { it.isNotBlank() }
                ?: emptyList(),
        )
        prefs[channelsKey]
            ?.split(CHANNEL_DELIMITER)
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: catalog.defaultMyChannels.ifEmpty { listOf(FeedChannels.RECOMMEND) }
    }

    suspend fun saveCatalog(catalog: FeedChannelCatalog) {
        context.feedChannelDataStore.edit { prefs ->
            prefs[allChannelsKey] = catalog.allChannels.joinToString(CHANNEL_DELIMITER)
            prefs[defaultMyKey] = catalog.defaultMyChannels.joinToString(CHANNEL_DELIMITER)
            prefs[publishCategoriesKey] = catalog.publishCategories.joinToString(CHANNEL_DELIMITER)
        }
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
        context.feedChannelDataStore.edit { prefs ->
            val defaults = prefs[defaultMyKey]?.split(CHANNEL_DELIMITER)?.filter { it.isNotBlank() }
                ?: emptyList()
            val normalized = if (defaults.isEmpty()) {
                listOf(FeedChannels.RECOMMEND)
            } else if (FeedChannels.RECOMMEND in defaults) {
                listOf(FeedChannels.RECOMMEND) + defaults.filter { it != FeedChannels.RECOMMEND }
            } else {
                listOf(FeedChannels.RECOMMEND) + defaults
            }
            prefs[channelsKey] = normalized.joinToString(CHANNEL_DELIMITER)
        }
    }

    companion object {
        private const val CHANNEL_DELIMITER = "\u0001"
    }
}
