package com.humanoidai.communication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class CommunicationType {
    WHATSAPP,
    TELEGRAM,
    SMS,
    MISSED_CALL,
    EMAIL,
    GENERIC
}

@Parcelize
data class CommunicationItem(
    val id: String,
    val sourcePackage: String,
    val sourceApp: String,
    val sender: String,
    val title: String,
    val contentPreview: String,
    val timestamp: Long,
    val type: CommunicationType,
    val isActive: Boolean = true,
    val isPriority: Boolean = false,
    val priorityScore: Int = 0,
    val requiresAttention: Boolean = false
) : Parcelable

data class PriorityContact(
    val name: String,
    val phoneNumber: String?,
    val email: String?,
    val isPriority: Boolean = true
)
