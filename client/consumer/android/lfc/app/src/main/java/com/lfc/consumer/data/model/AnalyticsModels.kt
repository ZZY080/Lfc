package com.lfc.consumer.data.model

data class AnalyticsEventInput(
    val event: String,
    val properties: Map<String, Any?>? = null,
    val platform: String? = null,
    val sessionId: String? = null,
    val occurredAt: String? = null,
)

data class IngestAnalyticsEventsRequest(
    val events: List<AnalyticsEventInput>,
)

data class IngestAnalyticsEventsResponse(
    val accepted: Int,
)
