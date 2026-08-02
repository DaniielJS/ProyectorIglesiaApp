package com.proyectorbiblico.app.model

import kotlinx.serialization.Serializable

@Serializable
data class HimnosResponse(
    val data: List<Himno> = emptyList()
)

@Serializable
data class Himno(
    val id: Int = 0,
    val idLyric: Int = 0,
    val type: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val musicalNote: String = "",
    val classification: List<Int> = emptyList(),
    val isFavorite: Boolean = false,
    val haveSong: Boolean = false,
    val content: List<String> = emptyList()
)