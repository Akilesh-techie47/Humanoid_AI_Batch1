package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// Data model
// -----------------------------------------------------------------
data class RecognizedPerson(
    val id: Int,
    val name: String,
    val label: String,           // e.g. "Family", "Colleague", "Unknown"
    val lastSeen: String,
    val confidence: Float,       // 0f–1f, filled in by ML in Phase 2
    val isKnown: Boolean
)

// -----------------------------------------------------------------
// RecognitionScreen
// -----------------------------------------------------------------
@Composable
fun RecognitionScreen(navController: NavController) {

    // Placeholder data — real faces injected by ML Kit in Phase 2
    val people = remember {
        listOf(
            RecognizedPerson(1, "Person A", "Family", "Just now", 0.97f, true),
            RecognizedPerson(2, "Person B", "Colleague", "5 min ago", 0.89f, true),
            RecognizedPerson(3, "Unknown #1", "Unknown", "12 min ago", 0.0f, false),
            RecognizedPerson(4, "Person C", "Neighbor", "1 hr ago", 0.93f, true),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recognition", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { /* TODO: Add person flow in Phase 2 */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Person", tint = AccentCyan)
            }
        }

        // Stats row
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip("Known", people.count { it.isKnown }.toString(), AccentCyan, Modifier.weight(1f))
            StatChip("Unknown", people.count { !it.isKnown }.toString(), Color(0xFFFF5C5C), Modifier.weight(1f))
            StatChip("Total", people.size.toString(), TextSecondary, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(people) { person ->
                PersonCard(person)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun PersonCard(person: RecognizedPerson) {
    val avatarColor = if (person.isKnown) AccentCyan else Color(0xFFFF5C5C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.15f))
                .border(1.5.dp, avatarColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = avatarColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(person.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelBadge(person.label)
                Text("Last seen: ${person.lastSeen}", fontSize = 11.sp, color = TextSecondary)
            }
        }

        if (person.isKnown) {
            Text(
                "${(person.confidence * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
        }
    }
}

@Composable
private fun LabelBadge(label: String) {
    val color = when (label) {
        "Family"    -> Color(0xFF66BB6A)
        "Colleague" -> Color(0xFF42A5F5)
        "Neighbor"  -> Color(0xFFFFA726)
        else        -> Color(0xFFFF5C5C)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
