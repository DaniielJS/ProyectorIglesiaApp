package com.proyectorbiblico.app.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
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
                Text(
                    text = contenido,
                    fontSize = 16.sp
                )
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
