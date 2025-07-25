package com.proyectorbiblico.app.presentation

import android.app.Activity
import android.content.Context.DISPLAY_SERVICE
import android.hardware.display.DisplayManager
import android.view.Display

fun Activity.getExternalDisplay(): Display? {
    val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
    val displays = displayManager.displays

    // Filtrar pantallas externas (no la principal)
    return displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
}

