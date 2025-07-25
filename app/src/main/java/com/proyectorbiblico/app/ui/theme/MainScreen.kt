package com.proyectorbiblico.app.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    onProyectarClick: () -> Unit,
    onCerrarClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("¡Proyecto funcionando correctamente!")
        Button(onClick = onProyectarClick) {
            Text("Proyectar Versículo")
        }
        Button(onClick = onCerrarClick) {
            Text("Cerrar Proyección")
        }
    }
}
