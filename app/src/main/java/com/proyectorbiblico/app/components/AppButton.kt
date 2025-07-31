package com.proyectorbiblico.app.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit
) {
  Button(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color(0xFF111111),
      contentColor = Color.White
    ),
    content = content
  )
}
