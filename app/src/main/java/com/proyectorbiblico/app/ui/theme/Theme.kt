import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Theme.kt
private val AppBlue = Color(0xFF1976D2)
private val DarkGray = Color(0xFF111111)

private val LightColors = lightColorScheme(
    primary = AppBlue,
    onPrimary = Color.White,
    secondary = AppBlue,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    // … el resto de tus colores
)

@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(0.dp)
        ),
        content = content
    )
}
