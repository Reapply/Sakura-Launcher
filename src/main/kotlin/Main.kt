package me.inspect

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import me.inspect.ui.LauncherApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sakura Launcher",
        undecorated = true,
        transparent = true,
        state = WindowState(size = DpSize(900.dp, 600.dp))
    ) {
        LauncherApp(
            onClose = ::exitApplication,
            onMinimize = { window.isMinimized = true }
        )
    }
}
