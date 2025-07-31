package com.proyectorbiblico.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectorLibroDialog(
    libros: List<LibroBiblia>,
    onDismiss: () -> Unit,
    onLibroSeleccionado: (LibroBiblia) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtrados = libros.filter {
        it.nombres.any { nombre -> nombre.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Seleccionar libro") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar libro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filtrados.size) { index ->
                        val libro = filtrados[index]
                        Text(
                            text = libro.nombres.first(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLibroSeleccionado(libro)
                                    onDismiss()
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    )
}
