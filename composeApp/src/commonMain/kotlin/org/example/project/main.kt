package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.knightwood.config.BuildConfig

fun main() = application {
    BuildConfig
    Window(
        onCloseRequest = ::exitApplication,
        title = "KotlinProject",
    ) {

    }
}
