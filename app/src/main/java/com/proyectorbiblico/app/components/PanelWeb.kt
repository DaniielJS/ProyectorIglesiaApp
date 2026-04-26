package com.proyectorbiblico.app.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.proyectorbiblico.app.model.ImagenBusqueda
import com.proyectorbiblico.app.service.ImagenService
import kotlinx.coroutines.launch

@Composable
fun PanelWeb(onImageClick: (ImagenBusqueda) -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("") }
    var imagenes by remember { mutableStateOf<List<ImagenBusqueda>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Input y Botón de búsqueda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { 
                    urlInput = it
                    errorMensaje = ""
                },
                label = { Text("Buscar imágenes") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true
            )
            AppButton(
                onClick = {
                    if (urlInput.isNotBlank()) {
                        loading = true
                        errorMensaje = ""
                        coroutineScope.launch {
                            try {
                                imagenes = ImagenService.buscarImagenes(urlInput, 10)
                                if (imagenes.isEmpty()) {
                                    errorMensaje = "No se encontraron imágenes"
                                }
                            } catch (e: Exception) {
                                Log.e("PanelWeb", "Error en búsqueda", e)
                                errorMensaje = "Error al buscar: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "Ingresa un término de búsqueda", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text(if (loading) "Buscando..." else "🔍 Buscar")
            }
        }

        // Mensaje de error
        if (errorMensaje.isNotEmpty()) {
            Text(
                text = errorMensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Loading indicator
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Galería de imágenes
        if (imagenes.isNotEmpty()) {
            Text(
                text = "📸 ${imagenes.size} imágenes encontradas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((imagenes.size / 2 * 150 + 50).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imagenes) { imagen ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = MaterialTheme.shapes.medium,
                        onClick = { onImageClick(imagen) }
                    ) {
                        AsyncImage(
                            model = imagen.url,
                            contentDescription = imagen.titulo,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { error ->
                                Log.e("PanelWeb", "Error loading image: ${error.result.throwable}")
                            }
                        )
                    }
                }
            }
        }
    }
}
