package com.proyectorbiblico.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo

@Composable
fun ListaArchivosMultimedia(
    archivos: List<ArchivoMultimedia>,
    onArchivoSeleccionado: (ArchivoMultimedia) -> Unit
) {
    val imagenes = archivos.filter { it.tipo == TipoArchivo.IMAGEN }
    val audios = archivos.filter { it.tipo == TipoArchivo.AUDIO }
    val videos = archivos.filter { it.tipo == TipoArchivo.VIDEO }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TipoColumnaCard(Icons.Default.Image to "Imágenes", imagenes, onArchivoSeleccionado)
        TipoColumnaCard(Icons.Default.MusicNote to "Audios", audios, onArchivoSeleccionado)
        TipoColumnaCard(Icons.Default.Videocam to "Videos", videos, onArchivoSeleccionado)
    }
}

@Composable
fun RowScope.TipoColumnaCard(
    info: Pair<ImageVector, String>,
    archivos: List<ArchivoMultimedia>,
    onClick: (ArchivoMultimedia) -> Unit
) {
    val (icono, titulo) = info

    Card(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp)
            .heightIn(max = 280.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(icono, contentDescription = titulo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(titulo, style = MaterialTheme.typography.titleSmall)
            }

            archivos.forEach { archivo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onClick(archivo) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (archivo.tipo) {
                        TipoArchivo.IMAGEN -> Icon(Icons.Default.Image, contentDescription = "Imagen", modifier = Modifier.size(24.dp))
                        TipoArchivo.VIDEO -> Icon(Icons.Default.Videocam, contentDescription = "Video", modifier = Modifier.size(24.dp))
                        TipoArchivo.AUDIO -> Icon(Icons.Default.MusicNote, contentDescription = "Audio", modifier = Modifier.size(24.dp))
                        else -> Icon(Icons.Default.Description, contentDescription = "Archivo", modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = archivo.nombre.take(25) + if (archivo.nombre.length > 25) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
