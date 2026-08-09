package com.humanoidai.distributed

import com.humanoidai.embodiment.Capability
import com.humanoidai.embodiment.EmbodimentType
import com.humanoidai.embodiment.HardwareStatus
import java.util.UUID

/**
 * Metadata for a remote AI instance.
 */
data class AIInstance(
    val id: String = UUID.randomUUID().toString(),
    val type: EmbodimentType,
    val capabilities: Set<Capability>,
    val status: HardwareStatus = HardwareStatus.HEALTHY,
    val batteryLevel: Int = 100,
    val workloadPercent: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Messages sent between distributed agents.
 */
sealed class CoordinationMessage {
    data class CapabilityUpdate(val instanceId: String, val capabilities: Set<Capability>) : CoordinationMessage()
    data class TaskAssignment(val taskId: String, val goalId: String, val executorId: String) : CoordinationMessage()
    data class TaskStatusUpdate(val taskId: String, val status: String) : CoordinationMessage()
    data class WorldStateDelta(val payload: Map<String, Any>) : CoordinationMessage()
    data class Heartbeat(val instanceId: String) : CoordinationMessage()
}

/**
 * Synchronization modes for the distributed cluster.
 */
enum class SyncPolicy {
    LOCAL_ONLY,
    TRUSTED_DEVICES,
    CLOUD_RELAY
}
