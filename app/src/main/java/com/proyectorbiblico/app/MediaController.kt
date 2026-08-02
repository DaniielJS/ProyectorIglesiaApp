package com.proyectorbiblico.app

import android.content.Context
import android.view.Display
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.proyectorbiblico.app.model.ArchivoMultimedia
import com.proyectorbiblico.app.model.TipoArchivo

object MediaController {

    private var videoPresentation: MediaPresentation? = null
    private var audioPresentation: MediaPresentation? = null
    private var imagenPresentation: MediaPresentation? = null
    private var textoPresentation: MediaPresentation? = null

    // Presentations pending dismissal once the new one reports ready
    private val pendingOldPresentations: MutableList<MediaPresentation> = mutableListOf()

    var previewPlayer: ExoPlayer? = null
    var ultimoProyectado by mutableStateOf<ArchivoMultimedia?>(null)
        private set


    fun isPlaying(tipo: TipoArchivo): Boolean {
        return when (tipo) {
            TipoArchivo.VIDEO -> videoPresentation?.getVideoPlayer()?.isPlaying ?: false
            TipoArchivo.AUDIO -> audioPresentation?.getAudioPlayer()?.isPlaying ?: false
            else -> false
        }
    }

    fun getVolume(tipo: TipoArchivo): Float {
        return when (tipo) {
            TipoArchivo.VIDEO -> videoPresentation?.getVideoPlayer()?.volume ?: 1f
            TipoArchivo.AUDIO -> audioPresentation?.getAudioPlayer()?.volume ?: 1f
            else -> 1f
        }
    }

    fun proyectar(context: Context, display: Display, archivo: ArchivoMultimedia) {
        // 1. Stop playback on existing presentations to cut audio immediately
        try {
            listOf(videoPresentation, audioPresentation, imagenPresentation, textoPresentation).forEach { pres ->
                try { pres?.getVideoPlayer()?.let { p -> try { p.stop() } catch (_: Exception) {} } } catch (_: Exception) {}
                try { pres?.getAudioPlayer()?.let { p -> try { p.stop() } catch (_: Exception) {} } } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // Keep track of current presentations so we can dismiss them once the new one is ready
        pendingOldPresentations.clear()
        listOf(videoPresentation, audioPresentation, imagenPresentation, textoPresentation).forEach { pres ->
            if (pres != null) pendingOldPresentations.add(pres)
        }

        // 2. Create the new presentation instance
        ultimoProyectado = archivo
        val nueva: MediaPresentation? = try {
            when (archivo.tipo) {
                TipoArchivo.VIDEO -> MediaPresentation(context, display, archivo)
                TipoArchivo.AUDIO -> MediaPresentation(context, display, archivo)
                TipoArchivo.IMAGEN -> MediaPresentation(context, display, archivo)
                TipoArchivo.TEXTO -> MediaPresentation(context, display, archivo)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        // Assign the new presentation to the appropriate slot immediately so it's the "current"
        when (archivo.tipo) {
            TipoArchivo.VIDEO -> videoPresentation = nueva
            TipoArchivo.AUDIO -> audioPresentation = nueva
            TipoArchivo.IMAGEN -> imagenPresentation = nueva
            TipoArchivo.TEXTO -> textoPresentation = nueva
            else -> {}
        }

        // 3. Show the new presentation (its root is initially invisible until content is ready)
        try { nueva?.show() } catch (_: Exception) {}
    }

    fun reproyectarActivo(context: Context, display: Display, tipo: TipoArchivo) {
        when (tipo) {
            TipoArchivo.VIDEO -> {
                val archivo = videoPresentation?.archivo ?: return
                val lastPosition = videoPresentation?.getVideoPlayer()?.currentPosition ?: 0L

                videoPresentation?.dismiss()
                videoPresentation = MediaPresentation(context, display, archivo).also {
                    it.show()
                    it.getVideoPlayer()?.seekTo(lastPosition)
                }
            }
            TipoArchivo.AUDIO -> {
                val archivo = audioPresentation?.archivo ?: return
                val lastPosition = audioPresentation?.getAudioPlayer()?.currentPosition ?: 0L

                audioPresentation?.dismiss()
                audioPresentation = MediaPresentation(context, display, archivo).also {
                    it.show()
                    it.getAudioPlayer()?.seekTo(lastPosition)
                }
            }
            TipoArchivo.IMAGEN -> {
                val archivo = imagenPresentation?.archivo ?: return
                imagenPresentation?.dismiss()
                imagenPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            else -> {}
        }
    }


    fun getActivo(tipo: TipoArchivo): ArchivoMultimedia? {
        return when (tipo) {
            TipoArchivo.VIDEO -> videoPresentation?.archivo
            TipoArchivo.AUDIO -> audioPresentation?.archivo
            TipoArchivo.IMAGEN -> imagenPresentation?.archivo
            else -> null
        }
    }

    fun togglePlayPause(tipo: TipoArchivo, play: Boolean) {
        when (tipo) {
            TipoArchivo.VIDEO -> {
                videoPresentation?.setPlayWhenReady(play)
                if (play) previewPlayer?.play() else previewPlayer?.pause()
            }
            TipoArchivo.AUDIO -> audioPresentation?.setPlayWhenReady(play)
            else -> {}
        }
    }

    fun setVolume(tipo: TipoArchivo, volume: Float) {
        when (tipo) {
            TipoArchivo.VIDEO -> videoPresentation?.setVolume(volume)
            TipoArchivo.AUDIO -> audioPresentation?.setVolume(volume)
            else -> {}
        }
    }

    /**
     * Called by a MediaPresentation when it has its content ready to be shown.
     * Dismisses old presentations that were pending and releases their players.
     */
    fun notifyPresentationReady(presentation: MediaPresentation) {
        try {
            // Dismiss any pending old presentations (except the one that just became ready)
            val toDismiss = pendingOldPresentations.toList()
            for (old in toDismiss) {
                if (old === presentation) continue
                try { old.dismiss() } catch (_: Exception) {}
                try { old.getVideoPlayer()?.release() } catch (_: Exception) {}
                try { old.getAudioPlayer()?.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        pendingOldPresentations.clear()
    }

}
