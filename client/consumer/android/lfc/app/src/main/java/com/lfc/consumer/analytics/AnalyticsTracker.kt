package com.lfc.consumer.analytics

import com.lfc.consumer.data.ApiClient
import com.lfc.consumer.data.api.LfcApiService
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.data.model.AnalyticsEventInput
import com.lfc.consumer.data.model.IngestAnalyticsEventsRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AnalyticsTracker {
    private const val MAX_BATCH = 20
    private const val FLUSH_INTERVAL_MS = 30_000L
    private const val PLATFORM = "android"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val buffer = mutableListOf<AnalyticsEventInput>()
    private val sessionId = UUID.randomUUID().toString()

    private lateinit var api: LfcApiService
    private var flushJob: Job? = null
    private var initialized = false

    fun init(tokenManager: TokenManager) {
        if (initialized) return
        api = ApiClient.createApiService(tokenManager)
        flushJob?.cancel()
        flushJob = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
        initialized = true
    }

    fun track(event: String, properties: Map<String, Any?>? = null) {
        if (!initialized) return
        scope.launch {
            mutex.withLock {
                buffer.add(
                    AnalyticsEventInput(
                        event = event,
                        properties = properties,
                        platform = PLATFORM,
                        sessionId = sessionId,
                        occurredAt = nowIsoUtc(),
                    ),
                )
                if (buffer.size >= MAX_BATCH) {
                    flushLocked()
                }
            }
        }
    }

    suspend fun flush() {
        if (!initialized) return
        mutex.withLock { flushLocked() }
    }

    private suspend fun flushLocked() {
        if (buffer.isEmpty()) return
        val batch = buffer.toList()
        buffer.clear()
        runCatching {
            api.ingestAnalyticsEvents(IngestAnalyticsEventsRequest(batch))
        }
    }

    private fun nowIsoUtc(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
