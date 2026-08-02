package com.proyectorbiblico.app

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Display
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
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
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import com.proyectorbiblico.app.presentation.AsignadorFondos
import androidx.core.content.ContextCompat

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
    private lateinit var selectorFondosVersiculo: LinearLayout
    private var fondoSeleccionadoResId: Int? = null

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presentation_layout)

        playerView = findViewById(R.id.playerView)
        imageView = findViewById(R.id.imageView)
        selectorFondosVersiculo = findViewById(R.id.selectorFondosVersiculo)

        // Mantener la raíz invisible hasta que el contenido esté listo para evitar flashes
        val rootView = findViewById<View>(R.id.presentation_root)
        rootView.visibility = View.INVISIBLE

        when (archivo.tipo) {
            TipoArchivo.IMAGEN -> mostrarImagen(archivo.uri)
            TipoArchivo.VIDEO -> reproducirVideo(archivo.uri)
            TipoArchivo.AUDIO -> reproducirAudio(archivo.uri)
            TipoArchivo.TEXTO -> mostrarVersiculo(archivo.texto ?: "")
            else -> mostrarVersiculo("Tipo no soportado")
        }
    }

    private fun cambiarFondo(resId: Int) {
        val fondo = findViewById<ImageView>(R.id.fondoVersiculo)
        // Cancel any running animation and clear previous drawable to avoid transient artifacts

        // Resolve drawable via ContextCompat (safer inside Presentation)
        val drawable = ContextCompat.getDrawable(context, resId)
        if (drawable != null) {
            fondo.setImageDrawable(drawable)
        } else {
            fondo.setImageResource(resId)
        }
        fondo.visibility = View.VISIBLE
        fondo.invalidate()
        fondo.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
        fondoSeleccionadoResId = resId
    }

    private val fondosVersiculos = listOf(
        R.drawable.versiculo1,
        R.drawable.versiculo2,
        R.drawable.versiculo3,
        R.drawable.versiculo4
    )




    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarVersiculo(texto: String) {
        playerView.visibility = View.GONE
        imageView.visibility = View.GONE

        val root = findViewById<View>(R.id.presentation_root)

        val fondoAUsar = archivo.fondoSeleccionado ?: fondoSeleccionadoResId ?: run {
            try {
                // Elegir el domingo actual (o anterior si hoy no es domingo)
                val hoy = LocalDate.now()
                val domingo = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val numero = AsignadorFondos.obtenerNumeroFondo(domingo) // 1..4
                fondosVersiculos.getOrNull(numero - 1) ?: fondosVersiculos.first()
            } catch (e: Exception) {
                fondosVersiculos.first()
            }
        }

        // Si el ImageView ya está mostrando el mismo fondo (y es visible), no reasignar para evitar parpadeos
        val fondoView = findViewById<ImageView>(R.id.fondoVersiculo)
        if (!(fondoView.visibility == View.VISIBLE && fondoSeleccionadoResId != null && fondoSeleccionadoResId == fondoAUsar)) {
            cambiarFondo(fondoAUsar)
        }

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
            // Mostrar la presentación antes de salir
            root.visibility = View.VISIBLE
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

        // Finalmente mostrar la presentación ahora que todo está preparado
        findViewById<View>(R.id.presentation_root).visibility = View.VISIBLE
    }

    private fun mostrarImagen(uri: Uri) {
        findViewById<ImageView>(R.id.fondoVersiculo).visibility = View.GONE
        findViewById<LinearLayout>(R.id.contenedorGeneral).visibility = View.GONE

        val imageSource = uri.toString()
        val root = findViewById<View>(R.id.presentation_root)
        imageView.apply {
            alpha = 0f
            Glide.with(context).load(imageSource).into(this)
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(400)
                .start()
        }
        // Mostrar toda la presentación ahora que la imagen se colocó (evita flash)
        root.visibility = View.VISIBLE
    }

    private fun reproducirVideo(uri: Uri) {
        findViewById<ImageView>(R.id.fondoVersiculo).visibility = View.GONE
        findViewById<LinearLayout>(R.id.contenedorGeneral).visibility = View.GONE

        exoVideoPlayer?.release()
        exoVideoPlayer = ExoPlayer.Builder(context).build().also { player ->
            playerView.apply {
                alpha = 0f
                this.player = player
                useController = false
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start()
            }
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
            // Mostrar la presentación cuando el reproductor esté listo (reduce parpadeo)
            val root = findViewById<View>(R.id.presentation_root)
            root.visibility = View.VISIBLE
            player.playWhenReady = true
        }
    }

    private fun reproducirAudio(uri: Uri) {
        findViewById<ImageView>(R.id.fondoVersiculo).visibility = View.GONE
        findViewById<LinearLayout>(R.id.contenedorGeneral).visibility = View.GONE

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
    
    fun cambiarFondoExternamente(resId: Int) {
        cambiarFondo(resId)
    }

    override fun onStop() {
        super.onStop()
        exoVideoPlayer?.release()
        exoAudioPlayer?.release()
        exoVideoPlayer = null
        exoAudioPlayer = null
    }

}
