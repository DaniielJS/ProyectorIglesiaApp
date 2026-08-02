package com.proyectorbiblico.app.components


import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.presentation.getExternalDisplay
import com.proyectorbiblico.app.R
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.Himno
import com.proyectorbiblico.app.model.HimnosResponse
import com.proyectorbiblico.app.model.SeccionVersiculo
import com.proyectorbiblico.app.model.TipoArchivo
import kotlinx.serialization.json.Json

@Composable
fun HimnosVista(
) {
    val context = LocalContext.current

    var himnoSeleccionado by remember {
        mutableStateOf<Himno?>(null)
    }

    val himnos = remember {
        cargarHimnos(context)
    }

    var buscarId by remember {
        mutableStateOf("")
    }

    var buscarTexto by remember {
        mutableStateOf("")
    }

    val listaFiltrada = remember(buscarId, buscarTexto) {

        himnos.filter {

            val coincideId =
                buscarId.isBlank() ||
                        it.idLyric.toString().contains(buscarId)

            val coincideTexto =
                buscarTexto.isBlank() ||
                        it.title.contains(buscarTexto, true) ||
                        it.content.any { linea ->
                            linea.contains(buscarTexto, true)
                        }

            coincideId && coincideTexto
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        OutlinedTextField(
            value = buscarId,
            onValueChange = { buscarId = it },
            label = { Text("Buscar por idLyric") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = buscarTexto,
            onValueChange = { buscarTexto = it },
            label = { Text("Buscar por título o contenido") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.height(250.dp)
        ) {

            items(listaFiltrada) { himno ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable {

                            himnoSeleccionado = himno

                        }
                ) {

                    Text(
                        "${himno.idLyric} - ${himno.title}",
                        modifier = Modifier.padding(12.dp)
                    )

                }

            }

        }
        Spacer(Modifier.height(20.dp))


            himnoSeleccionado?.let { himno ->
                Spacer(Modifier.height(20.dp))

                Text(
                    himno.title,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(10.dp))

                val parrafos = obtenerParrafos(himno.content)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(parrafos) { parrafo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(parrafo)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(onClick = {
                                        val display = (context as Activity).getExternalDisplay()
                                        if (display != null) {
                                            val archivo = ArchivoMultimedia(
                                                nombre = himno.title,
                                                uri = Uri.EMPTY,
                                                tipo = TipoArchivo.TEXTO,
                                                secciones = listOf(SeccionVersiculo(himno.title, parrafo)),
                                                fondoSeleccionado = R.drawable.himnos
                                            )
                                            MediaController.proyectar(context, display, archivo)
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

    }
}

fun cargarHimnos(context: Context): List<Himno> {

    return try {

        val texto = context.assets
            .open("hyms/hymn_db_es.json")
            .bufferedReader()
            .use { it.readText() }

        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.decodeFromString<HimnosResponse>(texto).data

    } catch (e: Exception) {

        e.printStackTrace()

        emptyList()
    }
}

fun obtenerParrafos(content: List<String>): List<String> {

    val resultado = mutableListOf<String>()

    val actual = StringBuilder()

    for (linea in content) {

        if (linea.matches(Regex("^\\d+$")) || linea == "CORO") {

            if (actual.isNotBlank()) {
                resultado.add(actual.toString().trim())
                actual.clear()
            }

            actual.append(linea).append("\n")

        } else {

            actual.append(linea).append("\n")
        }
    }

    if (actual.isNotBlank())
        resultado.add(actual.toString().trim())

    return resultado
}

