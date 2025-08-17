import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HistorialItem(
    val referencia: String,
    val contenido: String
) : Parcelable