package com.proyectorbiblico.app.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.presentation.getExternalDisplay

@Composable
fun ControlMultimedia(tipo: TipoArchivo) {
    var isPlaying by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(1f) }

    val context = LocalContext.current
    val display = (context as? Activity)?.getExternalDisplay()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play/Pause + Porcentaje
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = {
                        isPlaying = !isPlaying
                        MediaController.togglePlayPause(tipo, isPlaying)
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reanudar"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Volumen + Slider
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volumen")
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            MediaController.setVolume(tipo, it)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Botón Reproyectar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (display != null) {
                            MediaController.reproyectarActivo(context, display, tipo)
                        } else {
                            Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reproyectar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reproyectar sin reiniciar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
