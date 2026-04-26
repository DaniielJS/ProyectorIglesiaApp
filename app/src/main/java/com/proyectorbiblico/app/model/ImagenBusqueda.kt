package com.proyectorbiblico.app.model

import kotlinx.serialization.Serializable

@Serializable
data class ImagenBusqueda(
    val url: String,
    val titulo: String = "",
    val descripcion: String = ""
)

