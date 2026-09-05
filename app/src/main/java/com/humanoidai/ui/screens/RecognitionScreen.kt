package com.humanoidai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humanoidai.ml.EnrolledPerson
import com.humanoidai.ml.FaceEnrollmentManager
import com.humanoidai.ml.FaceRecognitionManager
import com.humanoidai.ui.components.SidePanelDrawer
import com.humanoidai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    navController: NavController,
    enrollmentManager: FaceEnrollmentManager,
    recognitionManager: FaceRecognitionManager
) {
    var people by remember { mutableStateOf(enrollmentManager.getAllPersons()) }
    var showDeleteDialog by remember { mutableStateOf<EnrolledPerson?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    SidePanelDrawer(
        navController = navController,
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            topBar = {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { drawerState.open() } 
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.4f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Menu, "Open Menu", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text("RECOGNITION", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentCyan, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                
                Spacer(Modifier.weight(1f))

                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("enrollment") 
                }) {
                    Icon(Icons.Default.PersonAdd, "Add Person", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
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
                                onDelete = { showDeleteDialog = person },
                                onTogglePriority = {
                                    enrollmentManager.toggleCriticalStatus(person.name)
                                    people = enrollmentManager.getAllPersons()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { person ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = SurfaceDark,
            title = { Text("Remove ${person.name}?", color = TextPrimary) },
            text = { Text("This person will no longer be recognized by the camera.", color = TextSecondary) },
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

@Composable
private fun EnrolledPersonCard(person: EnrolledPerson, onDelete: () -> Unit, onTogglePriority: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val labelColor = when (person.label) {
        "Family"    -> SuccessGreen
        "Colleague" -> AccentCyan
        "Neighbor"  -> WarningOrange
        "Friend"    -> AccentPurple
        else        -> TextSecondary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = SurfaceDark.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.1f))
                    .border(1.dp, AccentCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(person.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = labelColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, labelColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        person.label.uppercase(), 
                        fontSize = 9.sp, 
                        color = labelColor, 
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTogglePriority()
            }) {
                Icon(
                    if (person.isCritical) Icons.Default.Star else Icons.Default.StarBorder,
                    "Priority",
                    tint = if (person.isCritical) WarningOrange else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            }) {
                Icon(Icons.Default.Delete, "Remove", tint = ErrorRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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