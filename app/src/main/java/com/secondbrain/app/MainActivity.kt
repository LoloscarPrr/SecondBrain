package com.secondbrain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.secondbrain.app.core.model.DayPart
import com.secondbrain.app.core.model.Memory
import com.secondbrain.app.core.model.TemporalContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val app = application as SecondBrainApplication
        MainViewModel.Factory(
            memoryRepository = app.memoryRepository,
            saveTextCapture = app.saveTextCapture
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecondBrainApp(viewModel)
        }
    }
}

@Composable
private fun SecondBrainApp(viewModel: MainViewModel) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val captureText by viewModel.captureText.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SecondBrain",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Captura ahora. Organiza después.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Nueva memoria",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = captureText,
                                onValueChange = viewModel::updateCaptureText,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 7,
                                placeholder = {
                                    Text("Escribe una idea, decisión, pendiente o cualquier cosa que quieras recordar…")
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = viewModel::saveCapture,
                                    enabled = captureText.isNotBlank() && !isSaving
                                ) {
                                    Text(if (isSaving) "Guardando…" else "Guardar")
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Memorias",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (memories.isEmpty()) {
                                "Todavía no has guardado ninguna memoria."
                            } else {
                                "${memories.size} ${if (memories.size == 1) "memoria guardada" else "memorias guardadas"}"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(
                    items = memories,
                    key = { memory -> memory.id }
                ) { memory ->
                    MemoryCard(memory)
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(memory: Memory) {
    val createdFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale("es", "CL"))
        .withZone(ZoneId.systemDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = memory.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = createdFormatter.format(memory.createdAt),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            memory.temporalContext?.let { temporal ->
                Text(
                    text = temporal.displayLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun TemporalContext.displayLabel(): String {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "CL"))
    val dateText = if (endDate != null && endDate != startDate) {
        "${dateFormatter.format(startDate)} → ${dateFormatter.format(endDate)}"
    } else {
        dateFormatter.format(startDate)
    }
    val partText = when (dayPart) {
        DayPart.MORNING -> " · mañana"
        DayPart.AFTERNOON -> " · tarde"
        DayPart.EVENING -> " · tarde-noche"
        DayPart.NIGHT -> " · noche"
        null -> ""
    }
    return "Programado: $dateText$partText"
}
