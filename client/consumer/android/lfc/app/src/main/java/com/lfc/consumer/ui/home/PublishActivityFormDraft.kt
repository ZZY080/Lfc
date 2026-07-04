package com.lfc.consumer.ui.home

import com.lfc.consumer.data.model.ActivityDto

/** 发布/编辑活动表单草稿（保存在 HomeScreen，跳转地址搜索时不丢失） */
data class PublishActivityFormDraft(
    val title: String = "",
    val description: String = "",
    val locationLabel: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startTime: String = "",
    val endTime: String = "",
    val maxParticipants: String = "0",
    val fee: String = "",
    val selectedImageUris: List<String> = emptyList(),
    val existingImageUrls: List<String> = emptyList(),
    val autoLocateConsumed: Boolean = false,
) {
    companion object {
        fun empty(): PublishActivityFormDraft = PublishActivityFormDraft()

        fun from(activity: ActivityDto): PublishActivityFormDraft = PublishActivityFormDraft(
            title = activity.title,
            description = activity.description.orEmpty(),
            locationLabel = activity.location,
            latitude = activity.latitude,
            longitude = activity.longitude,
            startTime = activity.startTime.take(16).replace(" ", "T"),
            endTime = activity.endTime.take(16).replace(" ", "T"),
            maxParticipants = activity.maxParticipants.toString(),
            fee = activity.fee?.toDoubleOrNull()?.let { if (it > 0) it.toString() else "" }.orEmpty(),
            existingImageUrls = activity.images.orEmpty(),
            autoLocateConsumed = activity.latitude != null && activity.longitude != null,
        )
    }
}
