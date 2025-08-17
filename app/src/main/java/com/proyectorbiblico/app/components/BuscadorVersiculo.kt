package com.proyectorbiblico.app.components

import HistorialItem
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.ResultadoBusquedaLibre
import com.proyectorbiblico.app.model.SeccionVersiculo
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.model.VersiculoBusquedaLibre
import com.proyectorbiblico.app.presentation.getExternalDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscadorVersiculo(buscadorVM: BuscadorViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val libroFocus = remember { FocusRequester() }
    val capituloFocus = remember { FocusRequester() }
    val versiculoInicioFocus = remember { FocusRequester() }
    val versiculoFinFocus = remember { FocusRequester() }
    val librosEnMemoria = mutableMapOf<String, Map<String, Map<String, String>>>()
    val historialBusqueda = buscadorVM.historial

    var libroInput by remember { mutableStateOf("") }
    var libroSeleccionado by remember { mutableStateOf<LibroBiblia?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var capitulo by remember { mutableStateOf("") }
    var versiculoInicio by remember { mutableStateOf("") }
    var versiculoFin by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var busquedaLibre by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<List<VersiculoBusquedaLibre>>(emptyList()) }

    var mostrarModal by remember { mutableStateOf(false) }
    var textoModal by remember { mutableStateOf("") }
    var tituloModal by remember { mutableStateOf("") }
    var expandirVersiculo by remember { mutableStateOf(true) }
    var expandirBusquedaLibre by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf("") }

    var seccion1 by rememberSaveable { mutableStateOf<HistorialItem?>(null) }
    var seccion2 by rememberSaveable { mutableStateOf<HistorialItem?>(null) }
    var segundaSeccionActiva by rememberSaveable { mutableStateOf(false) }

// === HELPERS como lambdas (dentro del Composable) ===
    val proyectarSolo: (HistorialItem) -> Unit = { item ->
        seccion1 = item
        seccion2 = null
        segundaSeccionActiva = false

        val display = (context as Activity).getExternalDisplay()
        if (display != null) {
            val archivo = ArchivoMultimedia(
                nombre = item.referencia,
                uri = Uri.EMPTY,
                tipo = TipoArchivo.TEXTO,
                secciones = listOf(
                    SeccionVersiculo(
                        titulo = item.referencia,
                        texto = item.contenido
                    )
                )
            )
            MediaController.proyectar(context, display, archivo)
        } else {
            Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
        }
    }

    val proyectarAmbas: () -> Unit = {
        val s1 = seccion1
        val s2 = seccion2
        if (s1 != null && s2 != null) {
            val display = (context as Activity).getExternalDisplay()
            if (display != null) {
                val archivo = ArchivoMultimedia(
                    nombre = "${s1.referencia} + ${s2.referencia}",
                    uri = Uri.EMPTY,
                    tipo = TipoArchivo.TEXTO,
                    secciones = listOf(
                        SeccionVersiculo(s1.referencia, s1.contenido),
                        SeccionVersiculo(s2.referencia, s2.contenido)
                    )
                )
                MediaController.proyectar(context, display, archivo)
            } else {
                Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Falta completar ambas secciones", Toast.LENGTH_SHORT).show()
        }
    }

    val añadirComoSegunda: (HistorialItem) -> Unit = { item ->
        if (!segundaSeccionActiva) {
            if (seccion1 == null) {
                // Si aún no hay primera, esta card pasa a ser la primera
                seccion1 = item
                proyectarSolo(item)
            } else {
                // Ya hay primera: esta es la segunda
                seccion2 = item
                segundaSeccionActiva = true
                proyectarAmbas()
            }
        }
    }

    LaunchedEffect(Unit) {
        libroFocus.requestFocus()
    }

    val librosDisponibles = remember {
        val archivos = context.assets.list("bible")?.filter { it.endsWith(".json") } ?: emptyList()

        archivos.map { nombreArchivo ->
            val base = nombreArchivo.removeSuffix(".json") // Ej: "1juan"
            val display = formatNombreLibro(base)
            LibroBiblia(nombres = listOf(display), abrev = base)
        }.sortedBy { it.nombres.first() }
    }

    val librosFiltrados = librosDisponibles.filter {
        it.nombres.any { n -> n.contains(libroInput, ignoreCase = true) }
    }

    /*suspend fun fetchVersiculoText(): List<VersiculoBusquedaLibre> {
        return withContext(Dispatchers.IO) {
            try {
                val versiculoFinal = versiculoFin.takeIf { it.isNotBlank() }?.let { "-$it" } ?: ""
                val url = "https://bible-api.deno.dev/api/read/rv1960/${libroSeleccionado?.nombres?.first()}/${capitulo}/${versiculoInicio}$versiculoFinal"
                val json = URL(url).readText()
                Log.d("BUSQUEDA", "JSON recibido: $json")
                val parsed = if (json.trim().startsWith("[")) {
                    Json.decodeFromString<List<VersiculoBusquedaLibre>>(json)
                } else {
                    listOf(Json.decodeFromString<VersiculoBusquedaLibre>(json))
                }
                parsed.map {
                    it.copy(book = libroSeleccionado?.nombres?.first() ?: "", chapter = capitulo.toIntOrNull() ?: 0)
                }
            } catch (e: Exception) {
                Log.e("BUSQUEDA", "Error al parsear versículo", e)
                emptyList()
            }
        }
    }*/

    suspend fun buscarVersiculosLocal(
        context: Context,
        libro: String,
        capitulo: String,
        versiculoInicio: String,
        versiculoFin: String?
    ): List<VersiculoBusquedaLibre> = withContext(Dispatchers.IO) {
        try {
            val nombreArchivo = libro.lowercase().replace(" ", "")  // ej. "1 - Juan" → "1juan"

            // Si no está en memoria, cargar desde assets
            val libroData = librosEnMemoria.getOrPut(nombreArchivo) {
                val json = context.assets.open("bible/$nombreArchivo.json").bufferedReader().use { it.readText() }
                Json.decodeFromString<Map<String, Map<String, String>>>(json)
            }

            val capitulos = libroData[capitulo] ?: return@withContext emptyList()

            // Rango de versículos
            val ini = versiculoInicio.toIntOrNull() ?: return@withContext emptyList()
            val fin = versiculoFin?.toIntOrNull() ?: ini

            (ini..fin).mapNotNull { num ->
                val texto = capitulos[num.toString()] ?: return@mapNotNull null
                VersiculoBusquedaLibre(
                    book = libro,
                    chapter = capitulo.toInt(),
                    number = num,
                    verse = texto,
                    id = 0,
                    study = ""
                )
            }
        } catch (e: Exception) {
            Log.e("BUSQUEDA", "Error al buscar versículo local", e)
            emptyList()
        }
    }

    suspend fun fetchBusquedaLibre(): List<VersiculoBusquedaLibre> {
        return withContext(Dispatchers.IO) {
            try {
                val query = busquedaLibre.trim().replace(" ", "%20")
                val url = "https://bible-api.deno.dev/api/read/nvi/search?q=$query"
                val json = URL(url).readText()
                Log.d("BUSQUEDA", "JSON recibido: $json")

                val jsonParser = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

                val parsed = jsonParser.decodeFromString<ResultadoBusquedaLibre>(json)
                parsed.data
            } catch (e: Exception) {
                Log.e("BUSQUEDA", "Error en búsqueda libre", e)
                emptyList()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandirVersiculo = !expandirVersiculo; expandirBusquedaLibre =
                                !expandirBusquedaLibre
                            }
                    ) {
                        Text(
                            text = "📖 Buscar por versículo",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(12.dp)
                        )
                    }
                    if (expandirVersiculo) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.weight(2f)
                            ) {
                                OutlinedTextField(
                                    value = libroInput,
                                    onValueChange = {
                                        libroInput = it
                                        expanded = true
                                    },
                                    label = { Text("Libro") },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .focusRequester(libroFocus)
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    librosFiltrados.forEach { libro ->
                                        DropdownMenuItem(
                                            text = { Text(libro.nombres.first()) },
                                            onClick = {
                                                libroSeleccionado = libro
                                                libroInput = libro.nombres.first()
                                                expanded = false
                                                focusManager.moveFocus(FocusDirection.Next)
                                                coroutineScope.launch {
                                                    capituloFocus.requestFocus()
                                                }
                                            }
                                        )
                                    }
                                }

                            }
                            OutlinedTextField(
                                value = capitulo,
                                onValueChange = {
                                    capitulo = it.filter { c -> c.isDigit() }
                                    if (it.isNotEmpty() && it.length >= 2) versiculoInicioFocus.requestFocus()
                                },
                                label = { Text("Capit.") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(capituloFocus)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            OutlinedTextField(
                                value = versiculoInicio,
                                onValueChange = {
                                    versiculoInicio = it.filter { c -> c.isDigit() }
                                    if (it.isNotEmpty() && it.length >= 2) versiculoFinFocus.requestFocus()
                                },
                                label = { Text("Vers. Inicio") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(versiculoInicioFocus)
                            )

                            OutlinedTextField(
                                value = versiculoFin,
                                onValueChange = {
                                    versiculoFin = it.filter { c -> c.isDigit() }
                                },
                                label = { Text("Vers. Final") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(versiculoFinFocus)
                            )
                        }
                        if (apiError.isNotEmpty()) {
                            Text(
                                text = apiError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            AppButton(onClick = {
                                if (libroSeleccionado != null && capitulo.isNotBlank() && versiculoInicio.isNotBlank()) {
                                    loading = true
                                    focusManager.clearFocus()
                                    apiError = ""
                                    coroutineScope.launch {
                                        try {
                                            resultado = buscarVersiculosLocal(
                                                context = context,
                                                libro = libroSeleccionado?.abrev ?: return@launch,
                                                capitulo = capitulo,
                                                versiculoInicio = versiculoInicio,
                                                versiculoFin = versiculoFin
                                            )
                                            if (resultado.isEmpty()) {
                                                apiError =
                                                    "No se encontró el versículo ${libroInput} $capitulo:$versiculoInicio"
                                            } else {
                                                val clave = resultado.firstOrNull()
                                                    ?.let { it.book to it.chapter }
                                                val textoCompleto =
                                                    resultado.joinToString("\n") { "${it.number}. ${it.verse}" }

                                                val resumen =
                                                    "${clave?.first?.let { formatNombreLibro(it) }} ${clave?.second}:${versiculoInicio}" +
                                                            if (versiculoFin.isNotBlank()) "-$versiculoFin" else ""

                                                val contenido = textoCompleto

                                                val item = HistorialItem(
                                                    referencia = resumen,
                                                    contenido = contenido
                                                )
                                                if (historialBusqueda.none { it.referencia == item.referencia }) {
                                                    buscadorVM.añadir(item)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            apiError =
                                                e.message ?: "Error al consultar el versículo"
                                        } finally {
                                            loading = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Completa todos los campos correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }) {
                                Text(if (loading) "Buscando..." else "🔍 Buscar")
                            }
                            AppButton(onClick = {
                                libroInput = ""
                                libroSeleccionado = null
                                capitulo = ""
                                versiculoInicio = ""
                                versiculoFin = ""
                                busquedaLibre = ""
                                resultado = emptyList()
                                coroutineScope.launch {
                                    libroFocus.requestFocus()
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text("Limpiar")
                            }
                        }
                    }
                }
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔍 Búsqueda libre",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandirBusquedaLibre = !expandirBusquedaLibre; expandirVersiculo =
                                !expandirVersiculo
                            }
                    )

                    if (expandirBusquedaLibre) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = busquedaLibre,
                            onValueChange = { busquedaLibre = it },
                            label = { Text("Búsqueda libre") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (apiError.isNotEmpty()) {
                            Text(
                                text = apiError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AppButton(onClick = {
                            if (busquedaLibre.isNotBlank()) {
                                loading = true
                                focusManager.clearFocus()
                                coroutineScope.launch {
                                    try {
                                        resultado = fetchBusquedaLibre()
                                        if (resultado.isEmpty()) {
                                            apiError =
                                                "No se encontraron resultados para \"$busquedaLibre\""
                                        } else {
                                            tituloModal = "Resultados de “$busquedaLibre”"
                                            textoModal = resultado.joinToString("\n\n") {
                                                "${it.book} ${it.chapter}:${it.number} — ${it.verse}"
                                            }
                                            mostrarModal = true
                                        }
                                    } catch (e: Exception) {
                                        apiError = e.message ?: "Error inesperado"
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                        }) {
                            Text(if (loading) "Buscando..." else "🔍 Buscar")
                        }
                    }
                }
            }
        }
// --- SECCIÓN: Búsqueda libre ---
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "🕘 Últimos versículos buscados",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                historialBusqueda.take(5).forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF1976D2), // antes: 0xFF0D47A1
                                            Color(0xFF64B5F6)  // antes: 0xFF1976D2
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            // Título: usa referencia o fallback si está en blanco
                            Text(
                                text = item.referencia.takeIf { it.isNotBlank() }
                                    ?: "${libroInput} ${capitulo}:${versiculoInicio}" +
                                    if (versiculoFin.isNotBlank()) "-${versiculoFin}" else "",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Snippet del versículo en texto pequeño con ellipsis
                            Text(
                                text = item.contenido,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Botón degradado oscuro
                                Surface(
                                    color = Color(0xFF222222),
                                    modifier = Modifier
                                        .weight(1f)          // <-- clave
                                        .height(48.dp)
                                        .clickable {proyectarSolo(item)},
                                    shape = RoundedCornerShape(12.dp),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "Proyectar",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                if (!segundaSeccionActiva) {
                                    // Botón degradado oscuro
                                    Surface(
                                        color = Color(0xFF222222),
                                        modifier = Modifier
                                            .weight(1f)          // <-- clave
                                            .height(48.dp)
                                            .clickable { añadirComoSegunda(item) },
                                        shape = RoundedCornerShape(12.dp),
                                        shadowElevation = 4.dp
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = "Añadir a segunda sección",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge
                                            )
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
}


fun formatNombreLibro(base: String): String {
    val nombreFormateado = base.replaceFirstChar { it.uppercaseChar() }

    return if (nombreFormateado.first().isDigit()) {
        val index = nombreFormateado.indexOfFirst { it.isLetter() }
        val numero = nombreFormateado.substring(0, index)
        val texto = nombreFormateado.substring(index).replaceFirstChar { it.uppercaseChar() }
        "$numero - $texto"
    } else {
        nombreFormateado
    }
}

data class LibroBiblia(val nombres: List<String>, val abrev: String)
class BuscadorViewModel : ViewModel() {
    // MutableStateList que sobrevive a recomposiciones y cambios de configuración
    private val _historial = mutableStateListOf<HistorialItem>()
    val historial: SnapshotStateList<HistorialItem> = _historial

    fun añadir(hist: HistorialItem) {
        if (_historial.none { it.referencia == hist.referencia }) {
            _historial.add(0, hist)
        }
    }
}
