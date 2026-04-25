package com.proyectorbiblico.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun VolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    segments: Int = 20
) {

    Row(
        modifier = modifier
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newVolume = (offset.x / size.width).coerceIn(0f, 1f)
                    onVolumeChange(newVolume)
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(Icons.Default.VolumeUp, contentDescription = "Volumen")

        Spacer(Modifier.width(8.dp))

        Row(modifier = Modifier.weight(1f)) {

            val activeSegments = (volume * segments).toInt()

            for (i in 0 until segments) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .padding(horizontal = 1.dp)
                        .background(
                            if (i < activeSegments)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}