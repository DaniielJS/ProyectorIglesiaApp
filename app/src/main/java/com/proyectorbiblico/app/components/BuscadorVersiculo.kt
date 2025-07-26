package com.proyectorbiblico.app.components

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.presentation.getExternalDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

data class LibroBiblia(val nombres: List<String>, val abrev: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscadorVersiculo() {
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var libroInput by remember { mutableStateOf("") }
    var libroSeleccionado by remember { mutableStateOf<LibroBiblia?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var capitulo by remember { mutableStateOf("") }
    var versiculoInicio by remember { mutableStateOf("") }
    var versiculoFin by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var busquedaLibre by remember { mutableStateOf("") }

    val librosFiltrados = librosDisponibles.filter {
        it.nombres.any { n -> n.contains(libroInput, ignoreCase = true) }
    }

    suspend fun fetchBusquedaLibre(): String {
        return withContext(Dispatchers.IO) {
            try {
                val query = busquedaLibre.trim().replace(" ", "%20")
                val url = "https://bible-api.deno.dev/api/read/nvi/search?q=$query"
                URL(url).readText()
            } catch (e: Exception) {
                "Error en búsqueda libre"
            }
        }
    }

    suspend fun fetchVersiculoText(): String {
        return withContext(Dispatchers.IO) {
            try {
                val versiculoFinal = versiculoFin.takeIf { it.isNotBlank() }?.let { "-$it" } ?: ""
                val url = "https://bible-api.deno.dev/api/read/rv1960/${libroSeleccionado?.nombres?.first()}/${capitulo}/${versiculoInicio}$versiculoFinal"
                URL(url).readText()
            } catch (e: Exception) {
                "Error"
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Fila para libro y capítulo
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = libroInput,
                        onValueChange = {
                            libroInput = it
                            expanded = true
                        },
                        label = { Text("Libro") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = capitulo,
                onValueChange = { capitulo = it.filter { c -> c.isDigit() } },
                label = { Text("Cap") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.width(80.dp)
            )
        }

        // Fila para versículos
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = versiculoInicio,
                onValueChange = { versiculoInicio = it.filter { c -> c.isDigit() } },
                label = { Text("Vers. Inicio") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = versiculoFin,
                onValueChange = { versiculoFin = it.filter { c -> c.isDigit() } },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                label = { Text("Vers. Final (opcional)") },
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                if (libroSeleccionado != null && capitulo.isNotBlank() && versiculoInicio.isNotBlank()) {
                    loading = true
                    coroutineScope.launch {
                        resultado = fetchVersiculoText().trim()
                        loading = false
                    }
                } else {
                    Toast.makeText(context, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Buscando..." else "🔍 Buscar Versículo")
        }

        if (resultado.isNotBlank()) {
            Text(
                text = resultado,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Text("Búsqueda libre", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = busquedaLibre,
                onValueChange = { busquedaLibre = it },
                label = { Text("Texto completo") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (busquedaLibre.isNotBlank()) {
                        loading = true
                        coroutineScope.launch {
                            resultado = fetchBusquedaLibre().trim()
                            loading = false
                        }
                    }
                },
                modifier = Modifier.alignByBaseline()
            ) {
                Text(if (loading) "Buscando..." else "🔍 Buscar")
            }
        }
        // Búsqueda libre
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Button(
            onClick = {
                if (libroSeleccionado != null && capitulo.isNotBlank() && versiculoInicio.isNotBlank()) {
                    loading = true
                    val activity = context as Activity
                    val display = activity.getExternalDisplay()

                    if (display == null) {
                        Toast.makeText(context, "No hay pantalla externa", Toast.LENGTH_SHORT).show()
                        loading = false
                        return@Button
                    }

                    coroutineScope.launch {
                        val texto = fetchVersiculoText()
                        val archivo = ArchivoMultimedia(
                            nombre = "Versículo: ${libroSeleccionado?.nombres?.first()} $capitulo:$versiculoInicio" +
                                    (if (versiculoFin.isNotBlank()) "-$versiculoFin" else ""),
                            uri = Uri.EMPTY,
                            tipo = TipoArchivo.TEXTO,
                            texto = texto.trim()
                        )
                        MediaController.proyectar(activity, display, archivo)
                        resultado = texto.trim()
                        loading = false
                    }
                } else {
                    Toast.makeText(context, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📺 Proyectar Versículo")
        }
    }
}