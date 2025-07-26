package com.proyectorbiblico.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo

@Composable
fun TableElementosActivos(
    audio: ArchivoMultimedia?,
    video: ArchivoMultimedia?,
    imagen: ArchivoMultimedia?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Audio
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote, contentDescription = "Audio")
            Spacer(modifier = Modifier.width(8.dp))
            Text(audio?.nombre ?: "-", style = MaterialTheme.typography.bodyLarge)
        }
        if (audio != null) {
            ControlMultimedia(tipo = TipoArchivo.AUDIO)
        }

        // Video
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Videocam, contentDescription = "Video")
            Spacer(modifier = Modifier.width(8.dp))
            Text(video?.nombre ?: "-", style = MaterialTheme.typography.bodyLarge)
        }
        if (video != null) {
            ControlMultimedia(tipo = TipoArchivo.VIDEO)
        }

        // Imagen
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Image, contentDescription = "Imagen")
            Spacer(modifier = Modifier.width(8.dp))
            Text(imagen?.nombre ?: "-", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
