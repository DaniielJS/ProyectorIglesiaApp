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

                    // Recupera la posición del player en pantalla externa
                    val (initialPosition, _) = MediaController.getVideoPlaybackInfo() ?: Pair(0L, false)

                    if (uri != Uri.EMPTY) {
                        val previewPlayer = remember(uri, initialPosition) {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(uri))
                                prepare()
                                seekTo(initialPosition)
                                playWhenReady = true // ✅ iniciar automáticamente
                            }
                        }

                        DisposableEffect(uri) {
                            onDispose {
                                previewPlayer.release()
                            }
                        }

                        AndroidView(
                            factory = {
                                PlayerView(it).apply {
                                    player = previewPlayer
                                    useController = false // ✅ sin controles
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    } else {
                        Text("Video no disponible", style = MaterialTheme.typography.bodyMedium)
                    }
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
