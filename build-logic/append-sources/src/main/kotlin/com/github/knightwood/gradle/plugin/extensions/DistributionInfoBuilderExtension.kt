package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.jetbrains.compose.desktop.application.dsl.*
import java.io.File
import javax.inject.Inject

abstract class DistributionInfoBuilderExtension : AbstractDistributions() {
    @get:Inject
    internal abstract val objects: ObjectFactory

    val linux: LinuxPlatformSettings = objects.newInstance(LinuxPlatformSettings::class.java)
    open fun linux(fn: Action<LinuxPlatformSettings>) {
        fn.execute(linux)
    }

    val macOS: JvmMacOSPlatformSettings = objects.newInstance(JvmMacOSPlatformSettings::class.java)
    open fun macOS(fn: Action<JvmMacOSPlatformSettings>) {
        fn.execute(macOS)
    }

    val windows: WindowsPlatformSettings = objects.newInstance(WindowsPlatformSettings::class.java)
    fun windows(fn: Action<WindowsPlatformSettings>) {
        fn.execute(windows)
    }

    @JvmOverloads
    fun fileAssociation(
        mimeType: String, extension: String, description: String,
        linuxIconFile: File? = null, windowsIconFile: File? = null, macOSIconFile: File? = null,
    ) {
        linux.fileAssociation(mimeType, extension, description, linuxIconFile)
        windows.fileAssociation(mimeType, extension, description, windowsIconFile)
        macOS.fileAssociation(mimeType, extension, description, macOSIconFile)
    }
}
