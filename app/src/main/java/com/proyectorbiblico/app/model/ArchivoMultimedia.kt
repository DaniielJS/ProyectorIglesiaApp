package com.proyectorbiblico.app.model

import android.net.Uri

data class ArchivoMultimedia(
    val nombre: String,
    val uri: Uri,
    val tipo: TipoArchivo,
    val texto: String? = null,
    val secciones: List<SeccionVersiculo>? = null
)

enum class TipoArchivo {
    IMAGEN, VIDEO, AUDIO, TEXTO, OTRO
}