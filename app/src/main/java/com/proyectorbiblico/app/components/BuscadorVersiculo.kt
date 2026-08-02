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
import com.proyectorbiblico.app.R
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.SeccionVersiculo
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.model.VersiculoBusquedaLibre
import com.proyectorbiblico.app.presentation.getExternalDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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
    var expandirVersiculo by remember { mutableStateOf(true) }
    var expandirBusquedaLibre by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf("") }

    var seccion1 by rememberSaveable { mutableStateOf<HistorialItem?>(null) }
    var seccion2 by rememberSaveable { mutableStateOf<HistorialItem?>(null) }
    var segundaSeccionActiva by rememberSaveable { mutableStateOf(false) }
    var fondoSeleccionado by remember { mutableStateOf<Int?>(null) }

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
                ),
                fondoSeleccionado = fondoSeleccionado
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
                    ),
                    fondoSeleccionado = fondoSeleccionado
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

    val totalVersiculosCapitulo by produceState<Int?>(
        initialValue = null,
        key1 = libroSeleccionado?.abrev,
        key2 = capitulo
    ) {
        val abrev = libroSeleccionado?.abrev
        if (abrev.isNullOrBlank() || capitulo.isBlank()) {
            value = null
            return@produceState
        }

        // Cargar si falta
        val libroData = librosEnMemoria.getOrPut(abrev) {
            val json = context.assets.open("bible/$abrev.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, String>>>(json)
        }

        val capKey = capitulo.trimStart('0').ifEmpty { capitulo } // por si escriben "01"
        value = libroData[capKey]?.size
    }

    suspend fun buscarLibreLocal(query: String): List<HistorialItem> {
        return withContext(Dispatchers.IO) {
            val words = query.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            val results = mutableListOf<HistorialItem>()
            val lowerQuery = query.lowercase()

            // For complete phrase: find 2 verses
            var phraseCount = 0
            val allBooks = librosDisponibles.map { it.abrev }
            for (book in allBooks) {
                if (phraseCount >= 2) break
                val libroData = librosEnMemoria.getOrPut(book) {
                    val json = context.assets.open("bible/$book.json").bufferedReader().use { it.readText() }
                    Json.decodeFromString<Map<String, Map<String, String>>>(json)
                }
                for ((chapter, verses) in libroData) {
                    if (phraseCount >= 2) break
                    for ((verseNum, text) in verses) {
                        if (text.lowercase().contains(lowerQuery) && phraseCount < 2) {
                            val ref = "${formatNombreLibro(book)} $chapter:$verseNum"
                            results.add(HistorialItem(ref, text, isFromSearch = true))
                            phraseCount++
                        }
                    }
                }
            }

            // For each word: find 1 verse per word
            for (word in words) {
                var found = false
                val lowerWord = word.lowercase()
                for (book in allBooks) {
                    if (found) break
                    val libroData = librosEnMemoria.getOrPut(book) {
                        val json = context.assets.open("bible/$book.json").bufferedReader().use { it.readText() }
                        Json.decodeFromString<Map<String, Map<String, String>>>(json)
                    }
                    for ((chapter, verses) in libroData) {
                        if (found) break
                        for ((verseNum, text) in verses) {
                            if (text.lowercase().contains(lowerWord) && !found) {
                                val ref = "${formatNombreLibro(book)} $chapter:$verseNum"
                                results.add(HistorialItem(ref, text, isFromSearch = true))
                                found = true
                                break
                            }
                        }
                    }
                }
            }
            results
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
                            // Label con el total de versículos
                            if (totalVersiculosCapitulo != null) {
                                Text(
                                    text = "Total: $totalVersiculosCapitulo",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
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
                                            val versiculos = buscarVersiculosLocal(
                                                context = context,
                                                libro = libroSeleccionado?.abrev ?: return@launch,
                                                capitulo = capitulo,
                                                versiculoInicio = versiculoInicio,
                                                versiculoFin = versiculoFin
                                            )
                                            if (versiculos.isEmpty()) {
                                                apiError =
                                                    "No se encontró el versículo ${libroInput} $capitulo:$versiculoInicio"
                                            } else {
                                                val bloques = versiculos.chunked(3)

                                                bloques.forEach { grupo ->
                                                    val clave = grupo.firstOrNull()
                                                        ?.let { it.book to it.chapter }

                                                    val resumen = buildString {
                                                        append("${clave?.first?.let { formatNombreLibro(it) }} ${clave?.second}:${grupo.first().number}")
                                                        if (grupo.size > 1) {
                                                            append("-${grupo.last().number}")
                                                        }
                                                    }

                                                    val contenido = grupo.joinToString("\n") { "${it.number}. ${it.verse}" }

                                                    val item = HistorialItem(
                                                        referencia = resumen,
                                                        contenido = contenido
                                                    )

                                                    if (historialBusqueda.none { it.referencia == item.referencia }) {
                                                        buscadorVM.añadir(item)
                                                    }
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
                            }, modifier = Modifier.weight(1f)){
                                Text(if (loading) "Buscando..." else "🔍 Buscar")
                            }
                            AppButton(onClick = {
                                libroInput = ""
                                libroSeleccionado = null
                                capitulo = ""
                                versiculoInicio = ""
                                versiculoFin = ""
                                busquedaLibre = ""
                                coroutineScope.launch {
                                    libroFocus.requestFocus()
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text("Limpiar")
                            }
                        }
                        if (loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
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
                                        val resultados = buscarLibreLocal(busquedaLibre)
                                        resultados.forEach { buscadorVM.añadir(it) }
                                        if (resultados.isEmpty()) {
                                            apiError = "No se encontraron resultados para \"$busquedaLibre\""
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
                        if (loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
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
                historialBusqueda.take(50).forEach { item ->
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
                                        if (item.isFromSearch) {
                                            listOf(Color(0xFF388E3C), Color(0xFF81C784))
                                        } else {
                                            listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
                                        }
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                AppButton(onClick = {
                                    coroutineScope.launch {
                                        val next = getNextReference(context, item.referencia)
                                        if (next != null) {
                                            buscadorVM.updateItem(item, next)
                                        }
                                    }
                                }) {
                                    Text("+")
                                }
                                AppButton(onClick = {
                                    coroutineScope.launch {
                                        val prev = getPreviousReference(context, item.referencia)
                                        if (prev != null) {
                                            buscadorVM.updateItem(item, prev)
                                        }
                                    }
                                }) {
                                    Text("-")
                                }
                                AppButton(onClick = {
                                    buscadorVM.historial.remove(item)
                                }, modifier = Modifier.width(48.dp)) {
                                    Text("✕")
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

fun parseReferencia(ref: String): Triple<String, Int, Pair<Int, Int?>>? {
    val parts = ref.split(" ")
    if (parts.size < 2) return null
    val book = parts.dropLast(1).joinToString(" ")
    val last = parts.last()
    val chapVerse = last.split(":")
    if (chapVerse.size != 2) return null
    val chapter = chapVerse[0].toIntOrNull() ?: return null
    val verses = chapVerse[1].split("-")
    val start = verses[0].toIntOrNull() ?: return null
    val end = if (verses.size > 1) verses[1].toIntOrNull() else null
    return Triple(book, chapter, start to end)
}

suspend fun getNextReference(context: Context, referencia: String): HistorialItem? = withContext(Dispatchers.IO) {
    try {
        val parsed = parseReferencia(referencia) ?: return@withContext null
        val (book, chapter, verseRange) = parsed
        val (start, end) = verseRange

        val newStart: Int
        var newEnd: Int?

        if (end == null) {
            // Single verse, next is start + 1
            newStart = start + 1
            newEnd = null
        } else {
            // Range, add the next verse to the range
            newStart = start
            newEnd = end + 1
        }

        // Load the book data
        val nombreArchivo = book.lowercase().replace(" ", "").replace("-", "")
        val libroData = context.assets.open("bible/$nombreArchivo.json").bufferedReader().use { it.readText() }
        val data = Json.decodeFromString<Map<String, Map<String, String>>>(libroData)
        val chapterData = data[chapter.toString()] ?: return@withContext null

        // Check if the new verses exist
        val versiculos = mutableListOf<VersiculoBusquedaLibre>()
        val ini = newStart
        val fin = newEnd ?: newStart
        for (num in ini..fin) {
            val texto = chapterData[num.toString()] ?: return@withContext null
            versiculos.add(VersiculoBusquedaLibre(
                book = book,
                chapter = chapter,
                number = num,
                verse = texto,
                id = 0,
                study = ""
            ))
        }

        // Create new reference and content
        val newReference = if (newEnd == null) {
            "$book $chapter:$newStart"
        } else {
            "$book $chapter:$newStart-$newEnd"
        }

        val newContent = versiculos.joinToString("\n") { "${it.number}. ${it.verse}" }

        HistorialItem(newReference, newContent, isFromSearch = false)
    } catch (e: Exception) {
        Log.e("Buscador", "Error al obtener la siguiente referencia", e)
        null
    }
}

suspend fun getPreviousReference(context: Context, referencia: String): HistorialItem? = withContext(Dispatchers.IO) {
    try {
        val parsed = parseReferencia(referencia) ?: return@withContext null
        val (book, chapter, verseRange) = parsed
        val (start, end) = verseRange

        val newStart: Int
        var newEnd: Int?

        if (end == null) {
            // Single verse, add the previous verse to make a range
            newStart = start - 1
            newEnd = start
            if (newStart < 1) return@withContext null // No previous verse
        } else {
            // Range, remove the last verse
            newStart = start
            newEnd = end - 1
            if (newEnd < newStart) {
                // If it becomes a single verse, set end to null
                newEnd = null
            }
        }

        // Load the book data
        val nombreArchivo = book.lowercase().replace(" ", "").replace("-", "")
        val libroData = context.assets.open("bible/$nombreArchivo.json").bufferedReader().use { it.readText() }
        val data = Json.decodeFromString<Map<String, Map<String, String>>>(libroData)
        val chapterData = data[chapter.toString()] ?: return@withContext null

        // Check if the new verses exist
        val versiculos = mutableListOf<VersiculoBusquedaLibre>()
        val ini = newStart
        val fin = newEnd ?: newStart
        for (num in ini..fin) {
            val texto = chapterData[num.toString()] ?: return@withContext null
            versiculos.add(VersiculoBusquedaLibre(
                book = book,
                chapter = chapter,
                number = num,
                verse = texto,
                id = 0,
                study = ""
            ))
        }

        // Create new reference and content
        val newReference = if (newEnd == null) {
            "$book $chapter:$newStart"
        } else {
            "$book $chapter:$newStart-$newEnd"
        }

        val newContent = versiculos.joinToString("\n") { "${it.number}. ${it.verse}" }

        HistorialItem(newReference, newContent, isFromSearch = false)
    } catch (e: Exception) {
        Log.e("Buscador", "Error al obtener la referencia anterior", e)
        null
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

    fun updateItem(old: HistorialItem, new: HistorialItem) {
        val index = _historial.indexOf(old)
        if (index >= 0) {
            _historial[index] = new
        }
    }
}
