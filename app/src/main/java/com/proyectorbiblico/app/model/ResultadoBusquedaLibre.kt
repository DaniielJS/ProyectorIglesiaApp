package com.proyectorbiblico.app.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultadoBusquedaLibre(val data: List<VersiculoBusquedaLibre>)
@Serializable
data class VersiculoBusquedaLibre(
    val verse: String,
    val book: String? = null,
    val chapter: Int? = null,
    val number: Int,
    val id: Int,
    val study: String? = null
)
