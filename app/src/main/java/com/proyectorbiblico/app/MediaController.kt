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

    var previewPlayer: ExoPlayer? = null
    var ultimoProyectado by mutableStateOf<ArchivoMultimedia?>(null)
        private set
    fun getVideoPlaybackInfo(): Pair<Long, Boolean>? {
        val player = videoPresentation?.getVideoPlayer() ?: return null
        return Pair(player.currentPosition, player.isPlaying)
    }

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
        // Ensure any existing presentations are stopped and dismissed to avoid overlapping audio/video
        try {
            listOf(videoPresentation, audioPresentation, imagenPresentation, textoPresentation).forEach { pres ->
                try {
                    pres?.getVideoPlayer()?.let { p ->
                        try { p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                try {
                    pres?.getAudioPlayer()?.let { p ->
                        try { p.stop() } catch (_: Exception) {}
                        try { p.release() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                try { pres?.dismiss() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // Clear references
        videoPresentation = null
        audioPresentation = null
        imagenPresentation = null
        textoPresentation = null

        ultimoProyectado = archivo

        when (archivo.tipo) {
            TipoArchivo.VIDEO -> {
                videoPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            TipoArchivo.AUDIO -> {
                audioPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            TipoArchivo.IMAGEN -> {
                imagenPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            TipoArchivo.TEXTO -> {
                textoPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            else -> { /* Versículos u otros tipos */ }
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

}
