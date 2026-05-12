package com.proyectorbiblico.app.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo

// ─────────────────────────────────────────────────────────────────────────────
// LISTA PRINCIPAL (usada en MainActivity para audio/imagen/video)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ListaArchivosMultimedia(
    archivos: List<ArchivoMultimedia>,
    tipoVisible: TipoArchivo? = null,
    onArchivoSeleccionado: (ArchivoMultimedia) -> Unit
) {
    val archivosFiltrados =
        if (tipoVisible != null) archivos.filter { it.tipo == tipoVisible }
        else archivos

    when (tipoVisible) {

        // ── AUDIO: lista vertical compacta, sin fondo ──────────────────────
        TipoArchivo.AUDIO -> {
            Column {
                archivosFiltrados.forEach { archivo ->
                    ItemArchivoCompacto(
                        archivo = archivo,
                        onClick = { onArchivoSeleccionado(archivo) }
                    )
                }
            }
        }

        // ── IMAGEN / VIDEO: chips en wrap horizontal ───────────────────────
        TipoArchivo.IMAGEN,
        TipoArchivo.VIDEO -> {
            // FlowRow: se va a la línea siguiente cuando no cabe más
            // Requiere foundation >= 1.4 / material3 con FlowRow experimental
            // Si no tienes FlowRow, usa el bloque alternativo comentado abajo.
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                archivosFiltrados.forEach { archivo ->
                    ChipArchivo(
                        archivo = archivo,
                        onClick = { onArchivoSeleccionado(archivo) }
                    )
                }
            }
        }

        // ── FALLBACK: lista genérica ───────────────────────────────────────
        else -> {
            Column {
                archivosFiltrados.forEach { archivo ->
                    ItemArchivoCompacto(
                        archivo = archivo,
                        onClick = { onArchivoSeleccionado(archivo) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ITEM DE AUDIO: fila con icono + nombre, sin fondo llamativo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ItemArchivoCompacto(
    archivo: ArchivoMultimedia,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThumbnailForArchivo(
            archivo = archivo,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = archivo.nombre,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHIP DE IMAGEN/VIDEO: miniatura cuadrada + nombre corto
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChipArchivo(
    archivo: ArchivoMultimedia,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier.widthIn(max = 160.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThumbnailForArchivo(
                archivo = archivo,
                modifier = Modifier.size(64.dp)   // ← ajusta este valor a gusto
            )
            Text(
                text = archivo.nombre,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLUMNA CARD (no cambia, se deja igual)
// ─────────────────────────────────────────────────────────────────────────────

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    ThumbnailForArchivo(archivo = archivo, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = archivo.nombre.take(25) +
                                if (archivo.nombre.length > 25) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THUMBNAIL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThumbnailForArchivo(archivo: ArchivoMultimedia, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    when (archivo.tipo) {
        TipoArchivo.IMAGEN -> {
            AsyncImage(
                model = archivo.uri.toString(),
                contentDescription = archivo.nombre,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        TipoArchivo.VIDEO -> {
            val bitmap = remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(archivo.uri) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, archivo.uri)
                    bitmap.value = retriever.getFrameAtTime(1_000_000L)
                    retriever.release()
                } catch (_: Exception) { }
            }
            bitmap.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = archivo.nombre,
                    modifier = modifier,
                    contentScale = ContentScale.Crop
                )
            } ?: Icon(
                Icons.Default.Videocam,
                contentDescription = "Video",
                modifier = modifier
            )
        }
        TipoArchivo.AUDIO -> Icon(
            Icons.Default.MusicNote,
            contentDescription = "Audio",
            modifier = modifier
        )
        else -> Icon(
            Icons.Default.Description,
            contentDescription = "Archivo",
            modifier = modifier
        )
    }
}