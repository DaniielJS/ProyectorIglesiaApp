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
        ultimoProyectado = archivo
        when (archivo.tipo) {
            TipoArchivo.VIDEO -> {
                videoPresentation?.dismiss()
                videoPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            TipoArchivo.AUDIO -> {
                audioPresentation?.dismiss()
                audioPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            TipoArchivo.IMAGEN -> {
                imagenPresentation?.dismiss()
                imagenPresentation = MediaPresentation(context, display, archivo).also { it.show() }
            }
            TipoArchivo.TEXTO -> {
                val textoPresentation = MediaPresentation(context, display, archivo)
                textoPresentation.show()
            }
            else -> { /* Versículos u otros tipos */ }
        }
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
                previewPlayer?.playWhenReady = play }
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
