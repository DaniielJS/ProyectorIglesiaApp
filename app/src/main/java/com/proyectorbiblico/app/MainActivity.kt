package com.proyectorbiblico.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.proyectorbiblico.app.components.*
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.presentation.getExternalDisplay
import com.proyectorbiblico.app.ui.theme.AppTheme

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
                var selectedTab by remember { mutableStateOf(TabSeccion.VISUALES) }

                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)) {

                        TopTabSelector(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedTab) {
                            TabSeccion.VERSICULOS -> {
                                BuscadorVersiculo()
                            }

                            TabSeccion.VISUALES -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(onClick = {
                                        abrirSelectorDeCarpeta {
                                            archivos.clear()
                                            archivos.addAll(it)
                                        }
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
                                                    proyectarArchivo(archivo)
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
                                            .height(IntrinsicSize.Min) // <--- esto es clave
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
    fun Modifier.heightBetween(min: Dp, max: Dp): Modifier {
        return this.then(Modifier.heightIn(min = min, max = max))
    }

    private fun listarArchivosEnCarpeta(uri: Uri): List<ArchivoMultimedia> {
        val folder = DocumentFile.fromTreeUri(this, uri)
        return folder?.listFiles()?.mapNotNull {
            if (!it.isFile) return@mapNotNull null
            val tipo = when {
                it.name?.endsWith(".jpg", true) == true
                        || it.name?.endsWith(".png", true) == true
                        || it.name?.endsWith(".jpeg", true) == true
                        || it.name?.endsWith(".gif", true) == true -> TipoArchivo.IMAGEN
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
                uri = Uri.EMPTY,
                tipo = TipoArchivo.TEXTO,
                texto = versiculo
            )
            MediaController.proyectar(this, display, archivo) // ✅ Usa el controller, no MediaPresentation directamente
        } else {
            Toast.makeText(this, "No se detectó una pantalla externa", Toast.LENGTH_SHORT).show()
        }
    }

}
