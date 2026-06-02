package com.proyectorbiblico.app.service

import android.util.Log
import com.google.gson.JsonParser
import com.proyectorbiblico.app.model.ImagenBusqueda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Random

object ImagenService {

    private const val SERPAPI_KEY = "--"

    private fun getRandomImages(count: Int): List<ImagenBusqueda> {
        val random = Random()
        return (1..count).map { i ->
            val randomId = random.nextInt(1000)
            ImagenBusqueda(
                url = "https://picsum.photos/200/300?random=$randomId",
                thumbnail = "https://picsum.photos/50/50?random=$randomId",
                titulo = "Imagen aleatoria $i",
                descripcion = "Imagen de respaldo aleatoria"
            )
        }
    }

    suspend fun buscarImagenes(query: String, limite: Int = 20): List<ImagenBusqueda> {
        return withContext(Dispatchers.IO) {
            try {
                if (SERPAPI_KEY.isEmpty()) {
                    Log.e("ImagenService", "API key de SerpAPI no configurada")
                    return@withContext getRandomImages(limite)
                }

                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "https://serpapi.com/search?engine=google_images&q=$encoded&api_key=$SERPAPI_KEY&num=$limite&imgsz=xga"

                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                if (connection.responseCode != 200) {
                    Log.e("ImagenService", "Error HTTP: ${connection.responseCode}")
                    return@withContext getRandomImages(limite)
                }

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val jsonObject = JsonParser().parse(json).asJsonObject
                val imagesResults = jsonObject.getAsJsonArray("images_results")
                    ?: return@withContext getRandomImages(limite)

                val imagenes = mutableListOf<ImagenBusqueda>()
                val maxResults = if (imagesResults.size() < limite) imagesResults.size() else limite

                for (i in 0 until maxResults) {
                    try {
                        val result = imagesResults.get(i).asJsonObject

                        val title = result.get("title")?.asString ?: query
                        val link = result.get("link")?.asString ?: ""

                        val original = result.get("original")?.asString
                        val thumbnail = result.get("thumbnail")?.asString

                        if (!original.isNullOrEmpty() && !thumbnail.isNullOrEmpty()) {
                            imagenes.add(
                                ImagenBusqueda(
                                    url = original,          // HD
                                    thumbnail = thumbnail,   // liviano
                                    titulo = title,
                                    descripcion = link
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ImagenService", "Error al procesar imagen: ${e.message}")
                    }
                }

                if (imagenes.isEmpty()) getRandomImages(limite) else imagenes

            } catch (e: Exception) {
                Log.e("ImagenService", "Error en búsqueda: ${e.message}")
                getRandomImages(limite)
            }
        }
    }
}