package com.humanoidai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.humanoidai.ml.EnrolledPerson
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ui.theme.*

// -----------------------------------------------------------------
// RecognitionScreen — Phase 3 update
// Shows real enrolled persons from FaceEnrollmentManager.
// + button navigates to EnrollmentScreen.
// -----------------------------------------------------------------
@Composable
fun RecognitionScreen(
    navController: NavController,
    enrollmentManager: FaceEnrollmentManager,
    recognitionManager: FaceRecognitionManager
) {
    // Reload list whenever screen is shown
    var people by remember { mutableStateOf(enrollmentManager.getAllPersons()) }
    var showDeleteDialog by remember { mutableStateOf<EnrolledPerson?>(null) }

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
            IconButton(onClick = {
                navController.navigate("enrollment")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Person", tint = AccentCyan)
            }
        }

        // Stats row
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip("Enrolled", people.size.toString(), AccentCyan, Modifier.weight(1f))
            StatChip(
                "Family",
                people.count { it.label == "Family" }.toString(),
                Color(0xFF66BB6A),
                Modifier.weight(1f)
            )
            StatChip(
                "Others",
                people.count { it.label != "Family" }.toString(),
                Color(0xFF42A5F5),
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (people.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No persons enrolled yet",
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tap + to add a known person",
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { navController.navigate("enrollment") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enroll First Person", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(people) { person ->
                    EnrolledPersonCard(
                        person = person,
                        onDelete = { showDeleteDialog = person }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { person ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = SurfaceDark,
            title = {
                Text("Remove ${person.name}?", color = TextPrimary)
            },
            text = {
                Text(
                    "This person will no longer be recognized by the camera.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    enrollmentManager.removePerson(person.name)
                    recognitionManager.removeFace(person.name)
                    people = enrollmentManager.getAllPersons()
                    showDeleteDialog = null
                }) {
                    Text("Remove", color = Color(0xFFFF5C5C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// Enrolled person card
// -----------------------------------------------------------------
@Composable
private fun EnrolledPersonCard(
    person: EnrolledPerson,
    onDelete: () -> Unit
) {
    val labelColor = when (person.label) {
        "Family"    -> Color(0xFF66BB6A)
        "Colleague" -> Color(0xFF42A5F5)
        "Neighbor"  -> Color(0xFFFFA726)
        "Friend"    -> Color(0xFFAB47BC)
        else        -> TextSecondary
    }

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
                .background(AccentCyan.copy(alpha = 0.15f))
                .border(1.5.dp, AccentCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                person.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .background(labelColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    person.label,
                    fontSize = 10.sp,
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color(0xFFFF5C5C).copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -----------------------------------------------------------------
// Stat chip (unchanged from before)
// -----------------------------------------------------------------
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
