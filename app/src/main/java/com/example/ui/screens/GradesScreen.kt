package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CourseGrade
import com.example.model.KisiiGradingScale
import com.example.model.UiState
import com.example.ui.theme.Gold500
import com.example.ui.theme.Sapphire800
import com.example.ui.theme.Sapphire950
import com.example.ui.viewmodel.AcademicSummary
import java.util.Locale

@Composable
fun GradesScreen(
    gradesUi: UiState<List<CourseGrade>>,
    summary: AcademicSummary,
    onSaveGrade: (
        existingId: String?,
        courseCode: String,
        courseTitle: String,
        semester: String,
        credits: Long,
        catScore: Double,
        assignmentScore: Double,
        examScore: Double,
    ) -> Unit,
    onDeleteGrade: (String) -> Unit,
) {
    var editingGrade by remember { mutableStateOf<CourseGrade?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingGrade = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_grade_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add course grade")
            }
        }
    ) { innerPadding ->
        when (gradesUi) {
            UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = gradesUi.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        GpaSummaryCard(summary = summary)
                    }

                    if (gradesUi.data.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Grade,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "No grades recorded yet",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Track your CATs, assignments, and exam scores to compute your Kisii University GPA.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(onClick = { showAddDialog = true }) {
                                        Text("Add Unit Grade")
                                    }
                                }
                            }
                        }
                    } else {
                        items(gradesUi.data, key = { it.id }) { grade ->
                            GradeCard(
                                grade = grade,
                                onClick = {
                                    editingGrade = grade
                                    showAddDialog = true
                                },
                                onDelete = { onDeleteGrade(grade.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        GradeEditorDialog(
            existing = editingGrade,
            onDismiss = { showAddDialog = false },
            onConfirm = { code, title, semester, credits, cat, assign, exam ->
                onSaveGrade(
                    editingGrade?.id,
                    code,
                    title,
                    semester,
                    credits,
                    cat,
                    assign,
                    exam,
                )
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun GpaSummaryCard(summary: AcademicSummary) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(colors = listOf(Sapphire950, Sapphire800))
                )
                .padding(20.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "CUMULATIVE GPA",
                            style = MaterialTheme.typography.labelMedium,
                            color = Gold500,
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f / 4.00", summary.cumulativeGpa),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", summary.averagePercentage)}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                            )
                            Text(
                                text = "${summary.totalCredits} credits • ${summary.unitCount} units",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Kisii Scale: A (70+) • B (60-69) • C (50-59) • D (40-49) • E (<40)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun GradeCard(
    grade: CourseGrade,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val gradeColor = when (grade.gradeLetter) {
        "A" -> MaterialTheme.colorScheme.tertiary
        "B" -> MaterialTheme.colorScheme.primary
        "C" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${grade.courseCode} • ${grade.credits} Credits",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = grade.courseTitle,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = grade.semester,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = gradeColor.copy(alpha = 0.14f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = grade.gradeLetter,
                                style = MaterialTheme.typography.headlineSmall,
                                color = gradeColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", grade.totalScore)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = gradeColor,
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete grade",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (grade.totalScore / 100.0).toFloat().coerceIn(0f, 1f) },
                color = gradeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "CAT: ${String.format(Locale.US, "%.1f", grade.catScore)}/30",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "Assign: ${String.format(Locale.US, "%.1f", grade.assignmentScore)}/20",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "Exam: ${String.format(Locale.US, "%.1f", grade.examScore)}/70",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun GradeEditorDialog(
    existing: CourseGrade?,
    onDismiss: () -> Unit,
    onConfirm: (
        code: String,
        title: String,
        semester: String,
        credits: Long,
        cat: Double,
        assign: Double,
        exam: Double,
    ) -> Unit,
) {
    var code by remember { mutableStateOf(existing?.courseCode.orEmpty()) }
    var title by remember { mutableStateOf(existing?.courseTitle.orEmpty()) }
    var semester by remember { mutableStateOf(existing?.semester ?: "Sem 1 2026/2027") }
    var creditsText by remember { mutableStateOf((existing?.credits ?: 3L).toString()) }
    var catText by remember { mutableStateOf((existing?.catScore ?: 0.0).toString()) }
    var assignText by remember { mutableStateOf((existing?.assignmentScore ?: 0.0).toString()) }
    var examText by remember { mutableStateOf((existing?.examScore ?: 0.0).toString()) }

    val cat = (catText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 30.0)
    val assign = (assignText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 20.0)
    val exam = (examText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 70.0)
    val previewTotal = (cat + assign + exam).coerceIn(0.0, 100.0)
    val previewLetter = KisiiGradingScale.letterForScore(previewTotal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Record Course Grade" else "Update Course Grade") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. COMP 314)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("Semester") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        value = creditsText,
                        onValueChange = { creditsText = it },
                        label = { Text("Credits") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = catText,
                        onValueChange = { catText = it },
                        label = { Text("CAT /30") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = assignText,
                        onValueChange = { assignText = it },
                        label = { Text("Assign /20") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = examText,
                        onValueChange = { examText = it },
                        label = { Text("Exam /70") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Calculated Total: ${String.format(Locale.US, "%.1f", previewTotal)}% • Grade $previewLetter",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val credits = (creditsText.toLongOrNull() ?: 3L).coerceIn(1L, 12L)
                    onConfirm(code, title, semester, credits, cat, assign, exam)
                },
                enabled = code.trim().length >= 2 && title.trim().length >= 2,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
