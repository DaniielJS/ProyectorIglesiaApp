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
                    val context = LocalContext.current
                    val uri = activo.uri

                    // 1) Un único player para la vista previa
                    val previewPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            volume = 0f          // 🔇 silenciar preview
                        }
                    }

                    // 2) Cargar el nuevo video cuando cambia el uri
                    //    (si quieres arrancar siempre desde 0, deja initialPosition = 0L)
                    val (initialPosition, _) = MediaController.getVideoPlaybackInfo() ?: (0L to false)
                    LaunchedEffect(uri) {
                        previewPlayer.stop() // detén lo anterior
                        previewPlayer.setMediaItem(MediaItem.fromUri(uri))
                        previewPlayer.prepare()
                        previewPlayer.seekTo(initialPosition)
                        previewPlayer.playWhenReady = true
                    }

                    // 3) Liberar recursos al salir
                    DisposableEffect(Unit) {
                        onDispose {
                            // Desacopla el player de la vista y libera
                            previewPlayer.release()
                        }
                    }

                    // 4) Asegurar que el PlayerView use SIEMPRE este player
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false  // pon false si no quieres controles en la preview
                                player = previewPlayer
                            }
                        },
                        update = { view ->
                            if (view.player !== previewPlayer) {
                                view.player = previewPlayer
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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
