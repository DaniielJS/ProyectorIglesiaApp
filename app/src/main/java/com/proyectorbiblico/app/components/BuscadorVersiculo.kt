package com.proyectorbiblico.app.components

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.HistorialItem
import com.proyectorbiblico.app.model.ResultadoBusquedaLibre
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.model.VersiculoBusquedaLibre
import com.proyectorbiblico.app.presentation.getExternalDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscadorVersiculo() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val libroFocus = remember { FocusRequester() }
    val capituloFocus = remember { FocusRequester() }
    val versiculoInicioFocus = remember { FocusRequester() }
    val versiculoFinFocus = remember { FocusRequester() }

    val historialBusqueda = remember { mutableStateListOf<HistorialItem>() }

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
    LaunchedEffect(Unit) {
        libroFocus.requestFocus()
    }

    val librosDisponibles = remember {
        listOf(
            LibroBiblia(listOf("Genesis"), "GN"),
            LibroBiblia(listOf("Exodo", "Exodus"), "EX"),
            LibroBiblia(listOf("Levitico", "Leviticus"), "LV"),
            LibroBiblia(listOf("Numeros", "Numbers"), "NM"),
            LibroBiblia(listOf("Deuteronomio", "Deuteronomy"), "DT"),
            LibroBiblia(listOf("Josue", "Joshua"), "JOS"),
            LibroBiblia(listOf("Jueces", "Judges"), "JUE"),
            LibroBiblia(listOf("Rut", "Ruth"), "RT"),
            LibroBiblia(listOf("1-Samuel"), "1S"),
            LibroBiblia(listOf("2-Samuel"), "2S"),
            LibroBiblia(listOf("1-Reyes", "1-Kings"), "1R"),
            LibroBiblia(listOf("2-Reyes", "2-Kings"), "2R"),
            LibroBiblia(listOf("1-Cronicas", "1-Chronicles"), "1CR"),
            LibroBiblia(listOf("2-Cronicas", "2-Chronicles"), "2CR"),
            LibroBiblia(listOf("Esdras", "Ezra"), "ESD"),
            LibroBiblia(listOf("Nehemias", "Nehemiah"), "NEH"),
            LibroBiblia(listOf("Ester", "Esther"), "EST"),
            LibroBiblia(listOf("Job"), "JOB"),
            LibroBiblia(listOf("Salmos", "Psalms"), "SAL"),
            LibroBiblia(listOf("Proverbios", "Proverbs"), "PR"),
            LibroBiblia(listOf("Eclesiastes", "Ecclesiastes"), "EC"),
            LibroBiblia(listOf("Cantares", "Song of Solomon"), "CNT"),
            LibroBiblia(listOf("Isaias", "Isaiah"), "IS"),
            LibroBiblia(listOf("Jeremias", "Jeremiah"), "JER"),
            LibroBiblia(listOf("Lamentaciones", "Lamentations"), "LM"),
            LibroBiblia(listOf("Ezequiel", "Ezekiel"), "EZ"),
            LibroBiblia(listOf("Daniel"), "DN"),
            LibroBiblia(listOf("Oseas", "Hosea"), "OS"),
            LibroBiblia(listOf("Joel"), "JL"),
            LibroBiblia(listOf("Amos"), "AM"),
            LibroBiblia(listOf("Abdias", "Obadiah"), "ABD"),
            LibroBiblia(listOf("Jonas", "Jonah"), "JON"),
            LibroBiblia(listOf("Miqueas", "Micah"), "MI"),
            LibroBiblia(listOf("Nahum"), "NAH"),
            LibroBiblia(listOf("Habacuc", "Habakkuk"), "HAB"),
            LibroBiblia(listOf("Sofonias", "Zephaniah"), "SOF"),
            LibroBiblia(listOf("Hageo", "Haggai"), "HAG"),
            LibroBiblia(listOf("Zacarias", "Zechariah"), "ZAC"),
            LibroBiblia(listOf("Malaquias", "Malachi"), "MAL"),
            LibroBiblia(listOf("Mateo", "Matthew"), "MT"),
            LibroBiblia(listOf("Marcos", "Mark"), "MR"),
            LibroBiblia(listOf("Lucas", "Luke"), "LC"),
            LibroBiblia(listOf("Juan", "John"), "JN"),
            LibroBiblia(listOf("Hechos", "Acts"), "HCH"),
            LibroBiblia(listOf("Romanos", "Romans"), "RO"),
            LibroBiblia(listOf("1-Corintios", "Corinthians"), "1CO"),
            LibroBiblia(listOf("2-Corintios", "2-Corinthians"), "2CO"),
            LibroBiblia(listOf("Galatas", "Galatians"), "GA"),
            LibroBiblia(listOf("Efesios", "Ephesians"), "EF"),
            LibroBiblia(listOf("Filipenses", "Philippians"), "FIL"),
            LibroBiblia(listOf("Colosenses", "Colossians"), "COL"),
            LibroBiblia(listOf("1-Tesalonicenses", "1-Thessalonians"), "1TS"),
            LibroBiblia(listOf("2-Tesalonicenses", "2-Thessalonians"), "2TS"),
            LibroBiblia(listOf("1-Timoteo", "Timothy"), "1TI"),
            LibroBiblia(listOf("2-Timoteo", "2-Timothy"), "2TI"),
            LibroBiblia(listOf("Tito", "Titus"), "TIT"),
            LibroBiblia(listOf("Filemon", "Philemon"), "FLM"),
            LibroBiblia(listOf("Hebreos", "Hebrews"), "HE"),
            LibroBiblia(listOf("Santiago", "James"), "STG"),
            LibroBiblia(listOf("1-Pedro", "1-Peter"), "1P"),
            LibroBiblia(listOf("2-Pedro", "2-Peter"), "2P"),
            LibroBiblia(listOf("1-Juan", "1-John"), "1JN"),
            LibroBiblia(listOf("2-Juan", "2-John"), "2JN"),
            LibroBiblia(listOf("3-Juan", "3-John"), "3JN"),
            LibroBiblia(listOf("Judas", "Jude"), "JUD"),
            LibroBiblia(listOf("Apocalipsis", "Revelation"), "AP")
        )
    }

    val librosFiltrados = librosDisponibles.filter {
        it.nombres.any { n -> n.contains(libroInput, ignoreCase = true) }
    }

    suspend fun fetchVersiculoText(): List<VersiculoBusquedaLibre> {
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
                            .clickable { expandirVersiculo = !expandirVersiculo; expandirBusquedaLibre = !expandirBusquedaLibre }
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
                        Row( modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            Button(onClick = {
                                if (libroSeleccionado != null && capitulo.isNotBlank() && versiculoInicio.isNotBlank()) {
                                    loading = true
                                    coroutineScope.launch {
                                        resultado = fetchVersiculoText()
                                        loading = false

                                        val clave = resultado.firstOrNull()?.let { it.book to it.chapter }
                                        val textoCompleto =
                                            resultado.joinToString("\n") { "${it.number}. ${it.verse}" }
                                        tituloModal = "${clave?.first} ${clave?.second}"
                                        textoModal = textoCompleto

                                        val resumen = "${clave?.first} ${clave?.second}:${versiculoInicio}" +
                                                if (versiculoFin.isNotBlank()) "-$versiculoFin" else ""

                                        val contenido = textoCompleto

                                        val item = HistorialItem(referencia = resumen, contenido = contenido)
                                        if (historialBusqueda.none { it.referencia == item.referencia }) {
                                            historialBusqueda.add(0, item)
                                        }

                                        mostrarModal = true
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
                            Button(onClick = {
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
                            }, modifier = Modifier.fillMaxWidth()) {
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
                            .clickable { expandirBusquedaLibre = !expandirBusquedaLibre; expandirVersiculo = !expandirVersiculo }
                    )

                    if (expandirBusquedaLibre) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = busquedaLibre,
                            onValueChange = { busquedaLibre = it },
                            label = { Text("Búsqueda libre") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (busquedaLibre.isNotBlank()) {
                                loading = true
                                coroutineScope.launch {
                                    resultado = fetchBusquedaLibre()
                                    loading = false

                                    val clave = resultado.firstOrNull()?.let { it.book to it.chapter }
                                    val textoCompleto =
                                        resultado.joinToString("\n") { "${it.number}. ${it.verse}" }
                                    tituloModal = "${clave?.first} ${clave?.second}"
                                    textoModal = textoCompleto

                                    mostrarModal = true
                                }
                            }
                        }) {
                            Text(if (loading) "Buscando..." else "🔍 Buscar libre")
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
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = item.referencia,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.contenido.lines().firstOrNull()?.take(80) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                TextButton(onClick = {
                                    val display = (context as Activity).getExternalDisplay()
                                    if (display != null) {
                                        val archivo = ArchivoMultimedia(
                                            nombre = item.referencia,
                                            uri = Uri.EMPTY,
                                            tipo = TipoArchivo.TEXTO,
                                            texto = item.contenido
                                        )
                                        MediaController.proyectar(context, display, archivo)
                                    } else {
                                        Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("Proyectar")
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            ModalTexto(
                mostrar = mostrarModal,
                onCerrar = { mostrarModal = false },
                titulo = tituloModal,
                contenido = textoModal,
                onProyectar = {
                    val display = (context as Activity).getExternalDisplay()
                    if (display != null) {
                        val archivo = ArchivoMultimedia(
                            nombre = tituloModal,
                            uri = Uri.EMPTY,
                            tipo = TipoArchivo.TEXTO,
                            texto = textoModal
                        )
                        MediaController.proyectar(context, display, archivo)
                    } else {
                        Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
                    }
                    mostrarModal = false
                }
            )
        }
                //Spacer(modifier = Modifier.height(16.dp))


}
}
data class LibroBiblia(val nombres: List<String>, val abrev: String)
