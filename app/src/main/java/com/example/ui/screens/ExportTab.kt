package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ModProject
import com.example.model.TargetEngine
import com.example.ui.ModStudioViewModel
import com.example.ui.components.PillToggle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudioCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun ExportTab(
    viewModel: ModStudioViewModel,
    project: ModProject
) {
    val context = LocalContext.current
    val validation by viewModel.validationReport.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val progressText by viewModel.exportProgress.collectAsStateWithLifecycle()

    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportZipToUri(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Engine & Mod Metadata
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Configuración y Compatibilidad",
                        subtitle = "Elige la versión de Psych Engine y personaliza los datos del pack.json"
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillToggle(
                            label = "Psych Engine 1.0.4",
                            selected = project.targetEngine == TargetEngine.PSYCH_1_0_4,
                            accent = StudioCyan,
                            onClick = { viewModel.updateModMetadata(targetEngine = TargetEngine.PSYCH_1_0_4) },
                            modifier = Modifier.weight(1f).testTag("export_engine_104")
                        )
                        PillToggle(
                            label = "Psych Engine 0.7.3",
                            selected = project.targetEngine == TargetEngine.PSYCH_0_7_3,
                            accent = StudioMint,
                            onClick = { viewModel.updateModMetadata(targetEngine = TargetEngine.PSYCH_0_7_3) },
                            modifier = Modifier.weight(1f).testTag("export_engine_073")
                        )
                    }

                    OutlinedTextField(
                        value = project.name,
                        onValueChange = { viewModel.updateModMetadata(name = it) },
                        label = { Text("Nombre del Mod") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = project.description,
                        onValueChange = { viewModel.updateModMetadata(description = it) },
                        label = { Text("Descripción") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = project.weekName,
                            onValueChange = { viewModel.updateModMetadata(weekName = it) },
                            label = { Text("Nombre Semana") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = project.difficulties,
                            onValueChange = { viewModel.updateModMetadata(difficulties = it) },
                            label = { Text("Dificultades") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Pre-Export Validation Checklist
        item {
            StudioCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Verificación Anti-Errores",
                        subtitle = "Comprobando que todos los JSON, OGG, XML y Lua estén completos"
                    )

                    validation?.checks?.forEach { check ->
                        val icon = when {
                            !check.passed -> Icons.Filled.Error
                            check.isWarning -> Icons.Filled.Warning
                            else -> Icons.Filled.CheckCircle
                        }
                        val tint = when {
                            !check.passed -> ErrorRed
                            check.isWarning -> WarningAmber
                            else -> SuccessGreen
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(check.title, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Text(check.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Export Actions
        item {
            val canExport = validation?.canExport != false
            StudioCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Descargar Mod en .ZIP",
                        subtitle = "Carpeta lista para extraer en PsychEngine/mods/"
                    )

                    if (isExporting) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = StudioCyan
                        )
                        Text(progressText, style = MaterialTheme.typography.labelMedium, color = StudioCyan)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                saveZipLauncher.launch("${project.slug}-${project.targetEngine.versionTag}.zip")
                            },
                            enabled = canExport && !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("download_zip_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar ZIP", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.shareModZip { shareIntent ->
                                    context.startActivity(shareIntent)
                                }
                            },
                            enabled = canExport && !isExporting,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StudioSurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("share_zip_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartir ZIP", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir")
                        }
                    }

                    Text(
                        text = "Cómo instalar: 1) Guarda el .zip • 2) Extrae la carpeta \"${project.slug}\" dentro de la carpeta mods/ de Psych Engine • 3) Abre el juego y entra a Story Mode o Freeplay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
