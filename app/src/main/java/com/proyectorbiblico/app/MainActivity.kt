package com.proyectorbiblico.app

import MyAppTheme
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.proyectorbiblico.app.components.*
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.ImagenBusqueda
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.presentation.getExternalDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadState(
    val id: String,
    val fileName: String,
    var progress: Float = 0f,
    var status: String = "Descargando...",
    var isCompleted: Boolean = false,
    var hasContentLength: Boolean = false
)

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

                Toast.makeText(
                    this,
                    "Carpeta seleccionada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                val archivos = listarArchivosEnCarpeta(uri)

                onArchivosLeidos?.invoke(archivos)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            MyAppTheme {

                val archivos = remember { mutableStateListOf<ArchivoMultimedia>() }
                val downloadStates = remember { mutableStateListOf<DownloadState>() }
                var trigger by remember { mutableStateOf(0) }
                var selectedTab by remember { mutableStateOf(TabSeccion.VISUALES) }

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        TopTabSelector(
                            selectedTab = selectedTab,
                            onTabSelected = {
                                selectedTab = it
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Panel de descargas
                        if (downloadStates.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Descargas activas", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    downloadStates.forEach { state ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(state.fileName, modifier = Modifier.weight(1f))
                                            if (!state.isCompleted) {
                                                if (state.hasContentLength) {
                                                    LinearProgressIndicator(
                                                        progress = state.progress,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 8.dp)
                                                    )
                                                } else {
                                                    LinearProgressIndicator(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 8.dp)
                                                    )
                                                }
                                            }
                                            Text(state.status, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }

                        when (selectedTab) {

                            TabSeccion.VERSICULOS -> {

                                BuscadorVersiculo()
                            }

                            TabSeccion.WEB -> {

                                PanelWeb(
                                    onImageClick = { imagen ->
                                        descargarImagen(imagen, downloadStates)
                                    }
                                )
                            }

                            TabSeccion.VISUALES -> {

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .verticalScroll(
                                            rememberScrollState()
                                        )
                                ) {

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    AppButton(
                                        onClick = {

                                            abrirSelectorDeCarpeta {

                                                archivos.clear()

                                                archivos.addAll(it)
                                            }
                                        }
                                    ) {

                                        Text(
                                            "Seleccionar Carpeta Multimedia"
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .padding(vertical = 12.dp)
                                            .background(
                                                MaterialTheme.colorScheme
                                                    .onSurface
                                                    .copy(alpha = 0.2f)
                                            )
                                    )

                                    // =====================================================
                                    // ELEMENTOS MULTIMEDIA
                                    //
                                    // audio | images
                                    // audio | image
                                    // audio | video
                                    // audio | video
                                    // =====================================================

                                    if (archivos.isNotEmpty()) {

                                        Text(
                                            "Elementos multimedia :",
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 500.dp)
                                        ) {

                                            // =====================================
// COLUMNA IZQUIERDA -> AUDIO
// =====================================
                                            Column(
                                                modifier = Modifier
                                                    .weight(0.5f)
                                                    .fillMaxHeight()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(end = 8.dp)
                                            ) {
                                                ListaArchivosMultimedia(
                                                    archivos = archivos.filter { it.tipo == TipoArchivo.AUDIO },
                                                    tipoVisible = TipoArchivo.AUDIO,
                                                    onArchivoSeleccionado = { archivo ->
                                                        proyectarArchivo(archivo)
                                                        trigger++
                                                    }
                                                )
                                            }

// =====================================
// SEPARADOR
// =====================================
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(1.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                    )
                                            )

// =====================================
// COLUMNA DERECHA -> IMÁGENES + VIDEOS
// =====================================
                                            Column(
                                                modifier = Modifier
                                                    .weight(1.5f)
                                                    .fillMaxHeight()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(start = 8.dp)
                                            ) {
                                                ListaArchivosMultimedia(
                                                    archivos = archivos.filter { it.tipo == TipoArchivo.IMAGEN },
                                                    tipoVisible = TipoArchivo.IMAGEN,
                                                    onArchivoSeleccionado = { archivo ->
                                                        proyectarArchivo(archivo)
                                                        trigger++
                                                    }
                                                )

                                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                                ListaArchivosMultimedia(
                                                    archivos = archivos.filter { it.tipo == TipoArchivo.VIDEO },
                                                    tipoVisible = TipoArchivo.VIDEO,
                                                    onArchivoSeleccionado = { archivo ->
                                                        proyectarArchivo(archivo)
                                                        trigger++
                                                    }
                                                )
                                            }
                                        }

                                        Divider(
                                            thickness = 1.dp,

                                            color = MaterialTheme
                                                .colorScheme
                                                .onSurface
                                                .copy(alpha = 0.2f),

                                            modifier = Modifier.padding(
                                                vertical = 12.dp
                                            )
                                        )
                                    }

                                    // =====================================================
                                    // ELEMENTOS PROYECTÁNDOSE
                                    // =====================================================

                                    Text(
                                        "🎛 Elementos proyectándose :",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                    ) {

                                        // COLUMNA IZQUIERDA

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {

                                            key(trigger) {

                                                TableElementosActivos(
                                                    audio = MediaController.getActivo(
                                                        TipoArchivo.AUDIO
                                                    ),

                                                    video = MediaController.getActivo(
                                                        TipoArchivo.VIDEO
                                                    ),

                                                    imagen = MediaController.getActivo(
                                                        TipoArchivo.IMAGEN
                                                    )
                                                )
                                            }
                                        }

                                        // SEPARADOR

                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(1.dp)
                                                .background(
                                                    MaterialTheme.colorScheme
                                                        .onSurface
                                                        .copy(alpha = 0.2f)
                                                )
                                        )

                                        // COLUMNA DERECHA

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {

                                            VistaProyeccionActual()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun abrirSelectorDeCarpeta(
        onArchivosLeidos: (List<ArchivoMultimedia>) -> Unit
    ) {

        this.onArchivosLeidos = onArchivosLeidos

        folderPickerLauncher.launch(null)
    }

    private fun listarArchivosEnCarpeta(
        uri: Uri
    ): List<ArchivoMultimedia> {

        val folder = DocumentFile.fromTreeUri(this, uri)

        return folder?.listFiles()?.mapNotNull {

            if (!it.isFile) return@mapNotNull null

            val tipo = when {

                it.name?.endsWith(".jpg", true) == true ||
                        it.name?.endsWith(".png", true) == true ||
                        it.name?.endsWith(".jpeg", true) == true ||
                        it.name?.endsWith(".gif", true) == true -> {
                    TipoArchivo.IMAGEN
                }

                it.name?.endsWith(".mp4", true) == true -> {
                    TipoArchivo.VIDEO
                }

                it.name?.endsWith(".mp3", true) == true -> {
                    TipoArchivo.AUDIO
                }

                else -> {
                    TipoArchivo.OTRO
                }
            }

            ArchivoMultimedia(
                nombre = it.name ?: "sin_nombre",
                uri = it.uri,
                tipo = tipo
            )

        } ?: emptyList()
    }

    private fun proyectarArchivo(
        archivo: ArchivoMultimedia
    ) {

        val display = getExternalDisplay()

        Toast.makeText(
            this,
            "Display ID: ${display?.displayId ?: "null"}",
            Toast.LENGTH_SHORT
        ).show()

        if (display != null) {

            MediaController.proyectar(
                this,
                display,
                archivo
            )

        } else {

            Toast.makeText(
                this,
                "No se detectó una pantalla externa",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun proyectarVersiculo(
        versiculo: String
    ) {

        val display = getExternalDisplay()

        if (display != null) {

            val archivo = ArchivoMultimedia(
                nombre = "Versículo",
                uri = Uri.EMPTY,
                tipo = TipoArchivo.TEXTO,
                texto = versiculo
            )

            MediaController.proyectar(
                this,
                display,
                archivo
            )

        } else {

            Toast.makeText(
                this,
                "No se detectó una pantalla externa",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun descargarImagen(
        imagen: ImagenBusqueda,
        downloadStates: MutableList<DownloadState>
    ) {

        val coroutineScope =
            kotlinx.coroutines.CoroutineScope(
                Dispatchers.Main
            )

        coroutineScope.launch {

            try {

                val fileName =
                    "${imagen.titulo.replace(Regex("[^a-zA-Z0-9]"), "_")}.jpg"

                val id = imagen.url // usar url como id único

                val state = DownloadState(
                    id = id,
                    fileName = fileName
                )

                downloadStates.add(state)

                val cultosDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM
                    ),
                    "cultos"
                )

                if (!cultosDir.exists()) {

                    cultosDir.mkdirs()
                }

                val file = File(
                    cultosDir,
                    fileName
                )

                val startTime = System.currentTimeMillis()

                withContext(Dispatchers.IO) {

                    val url = URL(imagen.url)

                    val connection =
                        url.openConnection() as HttpURLConnection

                    connection.doInput = true

                    connection.connect()

                    val contentLength = connection.contentLength

                    state.hasContentLength = contentLength > 0

                    val input = connection.inputStream

                    val output = FileOutputStream(file)

                    val buffer = ByteArray(8192)

                    var bytesRead: Int

                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {

                        output.write(buffer, 0, bytesRead)

                        totalBytesRead += bytesRead

                        val elapsed = System.currentTimeMillis() - startTime

                        if (elapsed > 0) {

                            val speedKBps = (totalBytesRead / 1024.0) / (elapsed / 1000.0)

                            state.status = "Descargando... ${speedKBps.toInt()} KB/s"

                        }

                        if (contentLength > 0) {

                            state.progress = totalBytesRead.toFloat() / contentLength.toFloat()

                        }
                    }

                    output.close()

                    input.close()
                }

                state.isCompleted = true

                state.status = "Completado"

                // Remover después de 5 segundos
                kotlinx.coroutines.delay(5000)
                downloadStates.remove(state)

                Toast.makeText(
                    this@MainActivity,
                    "Imagen descargada: $fileName",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Error al descargar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}