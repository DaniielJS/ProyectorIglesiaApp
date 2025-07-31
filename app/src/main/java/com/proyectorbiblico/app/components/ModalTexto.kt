package com.proyectorbiblico.app.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun ModalTexto(
    mostrar: Boolean,
    onCerrar: () -> Unit,
    titulo: String,
    contenido: String,
    onProyectar: () -> Unit
) {
    if (mostrar) {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 0.dp, max = 400.dp)      // límite de altura
                        .verticalScroll(rememberScrollState())    // habilita scroll
                        .padding(8.dp)
                ) {
                    Text(contenido)
                }
            },
            confirmButton = {
                TextButton(onClick = onProyectar) {
                    Text("Proyectar")
                }
            },
            dismissButton = {
                TextButton(onClick = onCerrar) {
                    Text("Cerrar")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}
