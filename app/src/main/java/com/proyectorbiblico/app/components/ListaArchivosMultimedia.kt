package com.proyectorbiblico.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo

@Composable
fun ListaArchivosMultimedia(
    archivos: List<ArchivoMultimedia>,
    onArchivoSeleccionado: (ArchivoMultimedia) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(archivos) { archivo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArchivoSeleccionado(archivo) }
                    .padding(12.dp)
            ) {
                Text(
                    text = when (archivo.tipo) {
                        TipoArchivo.IMAGEN -> "🖼️"
                        TipoArchivo.VIDEO -> "🎥"
                        TipoArchivo.AUDIO -> "🎵"
                        else -> "📄"
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(archivo.nombre)
            }
            Divider()
        }
    }
}
