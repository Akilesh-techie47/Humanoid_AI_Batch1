package com.humanoidai.communication

import android.content.Context
import android.util.Log
import com.humanoidai.ai.AIManager
import com.humanoidai.ai.AIRequest
import com.humanoidai.memory.LongTermMemory
import kotlinx.coroutines.flow.first

class CommunicationIntelligenceEngine(
    private val context: Context
) {
    private var aiManager: AIManager? = null

    fun setAIManager(manager: AIManager) {
        this.aiManager = manager
    }

    suspend fun getWhatDidIMissSummary(): String {
        val missedCalls = MissedCallDetector(context).getMissedCalls()
        val notifications = HumanoidNotificationListener.notifications.value
        
        val priorityContacts = LongTermMemory.getInstance(context).getCriticalContacts()
        val priorityNames = priorityContacts.map { it.name.lowercase() }.toSet()

        val allItems = (missedCalls + notifications).map { item ->
            val isPriority = priorityNames.contains(item.sender.lowercase())
            item.copy(
                isPriority = isPriority,
                priorityScore = calculatePriorityScore(item, isPriority)
            )
        }.sortedByDescending { it.priorityScore }

        if (allItems.isEmpty()) {
            return "Sir, you haven't missed anything important in the last 24 hours. Your world is currently quiet."
        }

        return generateGeminiSummary(allItems)
    }

    private fun calculatePriorityScore(item: CommunicationItem, isPriority: Boolean): Int {
        var score = 0
        if (isPriority) score += 50
        if (item.type == CommunicationType.MISSED_CALL) score += 40
        if (item.type == CommunicationType.WHATSAPP || item.type == CommunicationType.TELEGRAM) score += 30
        
        val content = item.contentPreview.lowercase()
        if (content.contains("urgent") || content.contains("emergency") || content.contains("asap")) score += 20
        
        // Decay score based on time (last 24 hours)
        val hoursOld = (System.currentTimeMillis() - item.timestamp) / (1000 * 60 * 60)
        score -= (hoursOld * 2).toInt()
        
        return score
    }

    private suspend fun generateGeminiSummary(items: List<CommunicationItem>): String {
        val topItems = items.take(10)
        val prompt = buildSummaryPrompt(topItems)
        val manager = aiManager ?: return generateLocalFallbackSummary(items)

        return try {
            val response = manager.ask(prompt, "Owner", "CommunicationIntelligence")
            response.text
        } catch (e: Exception) {
            Log.e("CommIntel", "Gemini summary failed: ${e.message}")
            generateLocalFallbackSummary(items)
        }
    }

    private fun buildSummaryPrompt(items: List<CommunicationItem>): String {
        val itemsDescription = items.joinToString("\n") { item ->
            "- Source: ${item.sourceApp}, Sender: ${item.sender}, Type: ${item.type}, Priority: ${item.isPriority}, Content: ${item.contentPreview}"
        }

        return """
            You are Humanoid AI, a sophisticated assistant. Analyze the following missed communications and provide a concise, natural, JARVIS-style briefing for the owner.
            
            MISSED COMMUNICATIONS:
            $itemsDescription
            
            INSTRUCTIONS:
            - Start with a high-level summary (e.g., "Sir, you have 5 things to catch up on.")
            - Highlight the most important/priority items first.
            - Group items if they are from the same person or app.
            - Keep it brief and professional.
            - If there's nothing urgent, say so.
        """.trimIndent()
    }

    private fun generateLocalFallbackSummary(items: List<CommunicationItem>): String {
        val count = items.size
        val priorityCount = items.count { it.isPriority }
        val calls = items.count { it.type == CommunicationType.MISSED_CALL }
        
        var summary = "Sir, you have $count missed items."
        if (priorityCount > 0) summary += " $priorityCount are from your priority contacts."
        if (calls > 0) summary += " You also have $calls missed calls."
        
        return summary
    }
}
