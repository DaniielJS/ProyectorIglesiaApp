package com.proyectorbiblico.app.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.presentation.getExternalDisplay
import androidx.activity.ComponentActivity

@Composable
fun PanelVisuales(
    archivos: SnapshotStateList<ArchivoMultimedia>,
    onArchivoSeleccionado: (ArchivoMultimedia) -> Unit,
    onAbrirSelectorCarpeta: () -> Unit
) {
    val context = LocalContext.current
    var trigger by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        AppButton(onClick = {
            onAbrirSelectorCarpeta()
        }) {
            Text("Seleccionar Carpeta Multimedia")
        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 12.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )


        if (archivos.isNotEmpty()) {
            Text("Elementos multimedia :", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                ListaArchivosMultimedia(
                    archivos = archivos,
                    onArchivoSeleccionado = { archivo ->
                        onArchivoSeleccionado(archivo)
                        trigger++
                    }
                )
            }

            // DIVISOR SEGURO
            Divider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        Text("🎛 Elementos proyectándose :", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Columna izquierda: elementos activos
            Column(modifier = Modifier.weight(1f)) {
                key(trigger) {
                    TableElementosActivos(
                        audio = MediaController.getActivo(TipoArchivo.AUDIO),
                        video = MediaController.getActivo(TipoArchivo.VIDEO),
                        imagen = MediaController.getActivo(TipoArchivo.IMAGEN)
                    )
                }
            }

            // Separador vertical visible
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )

            // Columna derecha: vista modular de lo proyectado
            Column(modifier = Modifier.weight(1f)) {
                VistaProyeccionActual()
            }
        }
    }
}

