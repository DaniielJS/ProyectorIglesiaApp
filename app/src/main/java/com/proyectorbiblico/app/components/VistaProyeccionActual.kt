package com.proyectorbiblico.app.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.proyectorbiblico.app.MediaController
import com.proyectorbiblico.app.model.TipoArchivo
import com.proyectorbiblico.app.model.ArchivoMultimedia

@Composable
fun VistaProyeccionActual(
    modifier: Modifier = Modifier
) {
    val activo = MediaController.ultimoProyectado

    Column(modifier = modifier.padding(8.dp)) {
        Text("🔎 Vista actual proyectada:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (activo != null) {
            when (activo.tipo) {
                TipoArchivo.IMAGEN -> {
                    AsyncImage(
                        model = activo.uri,
                        contentDescription = "Imagen proyectada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                TipoArchivo.TEXTO -> {
                    Text(
                        text = activo.texto ?: "(Sin texto)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                TipoArchivo.AUDIO -> {
                    Text("Nombre: ${activo.nombre}", style = MaterialTheme.typography.bodyLarge)
                    Text("Tipo: ${activo.tipo}", style = MaterialTheme.typography.bodyMedium)
                }

                TipoArchivo.VIDEO -> {
                    // dentro de VistaProyeccionActual, case TipoArchivo.VIDEO
                    val context = LocalContext.current
                    val uri = activo.uri

// 1) Un único player para la preview
                    val previewPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            volume = 0f // preview sin sonido
                        }
                    }

// 2) Registrar este player en el MediaController
                    DisposableEffect(previewPlayer) {
                        MediaController.previewPlayer = previewPlayer
                        onDispose {
                            if (MediaController.previewPlayer === previewPlayer) {
                                MediaController.previewPlayer = null
                            }
                            previewPlayer.release()
                        }
                    }

// 3) Cargar/actualizar el video cuando cambie el uri
                    LaunchedEffect(uri) {
                        previewPlayer.stop()
                        previewPlayer.setMediaItem(MediaItem.fromUri(uri))
                        previewPlayer.prepare()
                        previewPlayer.play()   // o .pause() si quieres que empiece pausado
                    }

// 4) Asegura que el PlayerView use este player
                    AndroidView(
                        factory = { ctx -> PlayerView(ctx).apply { player = previewPlayer; useController = false } },
                        update = { view -> if (view.player !== previewPlayer) view.player = previewPlayer },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }


                else -> {
                    Text("Proyectando: ${activo.nombre}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Text("No se está proyectando nada actualmente.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
