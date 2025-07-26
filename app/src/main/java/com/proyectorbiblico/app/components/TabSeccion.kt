package com.proyectorbiblico.app.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class TabSeccion(val titulo: String) {
    VERSICULOS("Versículos"),
    VISUALES("Visuales")
}

@Composable
fun TopTabSelector(
    selectedTab: TabSeccion,
    onTabSelected: (TabSeccion) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier
    ) {
        TabSeccion.values().forEachIndexed { index, tab ->
            Tab(
                selected = selectedTab.ordinal == index,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.titulo) }
            )
        }
    }
}
