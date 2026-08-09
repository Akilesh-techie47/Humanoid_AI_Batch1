package com.humanoidai.ui.screens

sealed class HomeLayoutPreset(val id: String, val label: String) {
    object ClassicHud : HomeLayoutPreset("classic", "Classic HUD")
    object Minimal : HomeLayoutPreset("minimal", "Minimal")
    object RadialJarvis : HomeLayoutPreset("radial", "Radial JARVIS")
    object DashboardSplit : HomeLayoutPreset("split", "Dashboard Split")
    object FocusPerson : HomeLayoutPreset("focus", "Focus Person")
    object CardStack : HomeLayoutPreset("cards", "Card Stack")
    object SecurityNight : HomeLayoutPreset("security", "Security Night")
    object CompactWidget : HomeLayoutPreset("widget", "Compact Widget")
    object MultiZone : HomeLayoutPreset("zones", "Grid Multi-Zone")
    object ConversationFirst : HomeLayoutPreset("chat_first", "Conversation First")

    companion object {
        fun fromId(id: String): HomeLayoutPreset = when(id) {
            "minimal" -> Minimal
            "radial" -> RadialJarvis
            "split" -> DashboardSplit
            "focus" -> FocusPerson
            "cards" -> CardStack
            "security" -> SecurityNight
            "widget" -> CompactWidget
            "zones" -> MultiZone
            "chat_first" -> ConversationFirst
            else -> ClassicHud
        }
    }
}
