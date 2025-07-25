package com.proyectorbiblico.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.proyectorbiblico.app.components.ListaArchivosMultimedia
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.presentation.getExternalDisplay
import com.proyectorbiblico.app.ui.theme.AppTheme
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    private var selectedFolderUri: Uri? = null
    private var onArchivosLeidos: ((List<ArchivoMultimedia>) -> Unit)? = null

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedFolderUri = uri
                Toast.makeText(this, "Carpeta seleccionada correctamente", Toast.LENGTH_SHORT).show()

                val archivos = listarArchivosEnCarpeta(uri)
                onArchivosLeidos?.invoke(archivos)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val archivos = remember { mutableStateListOf<ArchivoMultimedia>() }
                var trigger by remember { mutableStateOf(0) }

                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        // Parte superior: botones
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Button(onClick = {
                                proyectarVersiculo("Porque de tal manera amó Dios al mundo...")
                                trigger++
                            }) {
                                Text("Proyectar Versículo")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = {
                                MediaController.cerrarTodo()
                                trigger++
                            }) {
                                Text("Cerrar Proyección")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(onClick = {
                                abrirSelectorDeCarpeta {
                                    archivos.clear()
                                    archivos.addAll(it)
                                }
                            }) {
                                Text("Seleccionar Carpeta Multimedia")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            selectedFolderUri?.let {
                                Text(
                                    "Carpeta seleccionada:\n${it.path}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Lista de archivos en un área desplazable
                        if (archivos.isNotEmpty()) {
                            Text("Archivos encontrados:", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Este bloque es scrollable y ocupa el espacio restante
                            ) {
                                ListaArchivosMultimedia(
                                    archivos = archivos,
                                    onArchivoSeleccionado = { archivo ->
                                        proyectarArchivo(archivo)
                                        trigger++
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Elementos activos al fondo
                        Text("🎛 Elementos activos:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        key(trigger) {
                            TableElementosActivos(
                                audio = MediaController.getActivo(TipoArchivo.AUDIO),
                                video = MediaController.getActivo(TipoArchivo.VIDEO),
                                imagen = MediaController.getActivo(TipoArchivo.IMAGEN)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun abrirSelectorDeCarpeta(onArchivosLeidos: (List<ArchivoMultimedia>) -> Unit) {
        this.onArchivosLeidos = onArchivosLeidos
        folderPickerLauncher.launch(null)
    }

    private fun listarArchivosEnCarpeta(uri: Uri): List<ArchivoMultimedia> {
        val folder = DocumentFile.fromTreeUri(this, uri)
        return folder?.listFiles()?.mapNotNull {
            if (!it.isFile) return@mapNotNull null
            val tipo = when {
                it.name?.endsWith(".jpg", true) == true || it.name?.endsWith(".png", true) == true -> TipoArchivo.IMAGEN
                it.name?.endsWith(".mp4", true) == true -> TipoArchivo.VIDEO
                it.name?.endsWith(".mp3", true) == true -> TipoArchivo.AUDIO
                else -> TipoArchivo.OTRO
            }
            ArchivoMultimedia(
                nombre = it.name ?: "sin_nombre",
                uri = it.uri,
                tipo = tipo
            )
        } ?: emptyList()
    }

    private fun proyectarArchivo(archivo: ArchivoMultimedia) {
        val display = getExternalDisplay()
        if (display != null) {
            MediaController.proyectar(this, display, archivo)
        } else {
            Toast.makeText(this, "No se detectó una pantalla externa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proyectarVersiculo(versiculo: String) {
        val display = getExternalDisplay()
        if (display != null) {
            val archivo = ArchivoMultimedia(
                nombre = "Versículo",
                uri = Uri.EMPTY, // No se usa para texto
                tipo = TipoArchivo.TEXTO,
                texto = versiculo
            )
            val mediaPresentation = MediaPresentation(this, display, archivo)
            mediaPresentation.show()
        } else {
            Toast.makeText(this, "No se detectó una pantalla externa", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun TableElementosActivos(
        audio: ArchivoMultimedia?,
        video: ArchivoMultimedia?,
        imagen: ArchivoMultimedia?
    ) {
        Column {
            Text("🎵 Audio: ${audio?.nombre ?: "-"}")
            if (audio != null) {
                ControlMultimedia(tipo = TipoArchivo.AUDIO)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("🎥 Video: ${video?.nombre ?: "-"}")
            if (video != null) {
                ControlMultimedia(tipo = TipoArchivo.VIDEO)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("🖼 Imagen: ${imagen?.nombre ?: "-"}")
        }
    }

    @Composable
    private fun ControlMultimedia(tipo: TipoArchivo) {
        var isPlaying by remember { mutableStateOf(true) }
        var volume by remember { mutableStateOf(1f) }

        val context = LocalContext.current
        val display = getExternalDisplay()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    isPlaying = !isPlaying
                    MediaController.togglePlayPause(tipo, isPlaying)
                }) {
                    Text(if (isPlaying) "⏸️ Pausar" else "▶️ Reanudar")
                }

                Text("🔊 Volumen")
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

            Button(
                onClick = {
                    if (display != null) {
                        MediaController.reproyectarActivo(context, display, tipo)
                    } else {
                        Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔁 Reproyectar sin reiniciar")
            }
        }
    }
}
