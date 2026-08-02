package com.proyectorbiblico.app.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object AsignadorFondos {

    // Fecha base de referencia: Un domingo conocido que iniciará con el Fondo 1
    // 4 de Enero de 2026 fue el primer domingo de este año
    @RequiresApi(Build.VERSION_CODES.O)
    private val FECHA_BASE: LocalDate = LocalDate.of(2026, 1, 4)

    /**
     * Obtiene el número de fondo (1..4) para un domingo específico.
     * Funciona para cualquier mes y año de forma continua.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun obtenerNumeroFondo(fecha: LocalDate): Int {
        if (fecha.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            throw IllegalArgumentException("La fecha proporcionada no es un domingo.")
        }

        // 1. Calcular cuántas semanas (domingos) han pasado desde la fecha base
        var semanasDiferencia = ChronoUnit.WEEKS.between(FECHA_BASE, fecha)

        // 2. Manejar valores negativos si la fecha es anterior a la fecha base
        if (semanasDiferencia < 0) {
            semanasDiferencia = (semanasDiferencia % 4) + 4
        }

        // 3. Aplicar el algoritmo de módulo para obtener el índice (0..3)
        val indiceFondo = (semanasDiferencia % 4).toInt()

        // 4. Retornar el número de fondo mapeado (1..4)
        return indiceFondo + 1
    }
}
