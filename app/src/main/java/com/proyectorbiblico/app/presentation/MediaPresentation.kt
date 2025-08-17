package com.proyectorbiblico.app

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Display
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaPresentation(
    context: Context,
    display: Display,
    val archivo: ArchivoMultimedia
) : Presentation(context, display) {

    private var exoVideoPlayer: ExoPlayer? = null
    private var exoAudioPlayer: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var imageView: ImageView
    private lateinit var textoView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presentation_layout)

        playerView = findViewById(R.id.playerView)
        imageView = findViewById(R.id.imageView)
        //textoView = findViewById(R.id.versiculoTextView)
// Referencias (asegúrate de tener estos ids en tu XML)

        when (archivo.tipo) {
            TipoArchivo.IMAGEN -> mostrarImagen(archivo.uri)
            TipoArchivo.VIDEO -> reproducirVideo(archivo.uri)
            TipoArchivo.AUDIO -> reproducirAudio(archivo.uri)
            TipoArchivo.TEXTO -> mostrarVersiculo(archivo.texto ?: "")
            else -> mostrarVersiculo("Tipo no soportado")
        }
    }

    private fun mostrarVersiculo(texto: String) {
        val fondo = findViewById<ImageView>(R.id.fondoVersiculo)
        fondo.visibility = View.VISIBLE

        val contenedorGeneral = findViewById<LinearLayout>(R.id.contenedorGeneral)
        val titulo1 = findViewById<TextView>(R.id.tituloSeccion1)
        val texto1 = findViewById<TextView>(R.id.textoSeccion1)
        val titulo2 = findViewById<TextView>(R.id.tituloSeccion2)
        val texto2 = findViewById<TextView>(R.id.textoSeccion2)
        val separador = findViewById<View>(R.id.separadorLineal) // ponle id al <View> línea

        val secciones = archivo.secciones

        if (!secciones.isNullOrEmpty()) {
            // Muestra contenedor de secciones
            contenedorGeneral.visibility = View.VISIBLE

            // Sección 1 (siempre)
            val s1 = secciones[0]
            titulo1.text = s1.titulo
            texto1.text = s1.texto

            if (secciones.size >= 2) {
                // Sección 2 + separador visibles
                val s2 = secciones[1]
                titulo2.text = s2.titulo
                texto2.text = s2.texto
                titulo2.visibility = View.VISIBLE
                texto2.visibility = View.VISIBLE
                separador.visibility = View.VISIBLE
            } else {
                // Solo primera sección
                titulo2.visibility = View.GONE
                texto2.visibility = View.GONE
                separador.visibility = View.GONE
            }
            return
        }

        // Fallback: comportamiento anterior con "texto" plano
        if (!archivo.texto.isNullOrBlank()) {
            contenedorGeneral.visibility = View.VISIBLE
            titulo1.text = "" // o el título previo si lo separas con tu propio formato
            texto1.text = archivo.texto
            titulo2.visibility = View.GONE
            texto2.visibility = View.GONE
            separador.visibility = View.GONE
        }
    }

    private fun mostrarImagen(uri: Uri) {
        imageView.apply {
            Glide.with(context).load(uri).into(this)
            visibility = View.VISIBLE
        }
    }

    private fun reproducirVideo(uri: Uri) {
        exoVideoPlayer?.release()
        exoVideoPlayer = ExoPlayer.Builder(context).build().also { player ->
            playerView.player = player
            playerView.useController = false
            playerView.visibility = View.VISIBLE
            player.setMediaItem(MediaItem.fromUri(uri))
            player.repeatMode = ExoPlayer.REPEAT_MODE_OFF  // sin loop

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Ejecutar en hilo principal
                        CoroutineScope(Dispatchers.Main).launch {
                            val steps = 15
                            val delayMs = 100L
                            for (i in steps downTo 0) {
                                val vol = i / steps.toFloat()
                                player.volume = vol
                                delay(delayMs)
                            }

                            // Fade visual
                            playerView.animate()
                                .alpha(0f)
                                .setDuration(1000)
                                .withEndAction {
                                    try {
                                        dismiss()  // cierre seguro
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .start()
                        }
                    }
                }
            })

            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun reproducirAudio(uri: Uri) {
        exoAudioPlayer?.release()
        exoAudioPlayer = ExoPlayer.Builder(context).build().also { player ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun setPlayWhenReady(play: Boolean) {
        exoVideoPlayer?.playWhenReady = play
        exoAudioPlayer?.playWhenReady = play
    }

    fun setVolume(volume: Float) {
        exoVideoPlayer?.volume = volume
        exoAudioPlayer?.volume = volume
    }

    fun getVideoPlayer(): ExoPlayer? = exoVideoPlayer
    fun getAudioPlayer(): ExoPlayer? = exoAudioPlayer

    override fun onStop() {
        super.onStop()
        exoVideoPlayer?.release()
        exoAudioPlayer?.release()
        exoVideoPlayer = null
        exoAudioPlayer = null
    }

}
