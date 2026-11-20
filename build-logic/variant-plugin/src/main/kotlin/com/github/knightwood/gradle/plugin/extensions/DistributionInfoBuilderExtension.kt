package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.compose.desktop.application.dsl.*
import java.io.File
import javax.inject.Inject

abstract class DistributionInfoBuilderExtension : AbstractDistributions() {
    @get:Inject
    abstract val objects: ObjectFactory

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

//<editor-fold desc="合并配置">

/**
 * 将自身已配置的项写入 [target]，未配置的项不会写入，因此 [target] 中同名的旧值会被保留。
 *
 * 合并 buildType 与 flavor 的配置时，先对 buildType 调用本方法，再对 flavor 调用本方法，
 * 后写入的一方会覆盖先写入的一方，从而实现 flavor > buildType 的优先级。
 *
 * 判定“已配置”的规则：
 * - 可空属性（如 String?）：非 null 视为已配置
 * - Boolean 属性：与 compose 插件中的默认值不同视为已配置，因此无法用 false 覆盖 buildType 中的 true
 * - [Property] 属性：以 [Property.isPresent] 为准
 * - targetFormats：非空视为已配置
 *
 * 注意：compose 插件中的 fileAssociations、infoPlist 是 internal 属性，无法在此合并。
 */
internal fun DistributionInfoBuilderExtension.mergeTo(target: DistributionInfoBuilderExtension) {
    packageName?.let { target.packageName = it }
    packageVersion?.let { target.packageVersion = it }
    copyright?.let { target.copyright = it }
    description?.let { target.description = it }
    vendor?.let { target.vendor = it }
    appResourcesRootDir.mergeTo(target.appResourcesRootDir)
    licenseFile.mergeTo(target.licenseFile)
    if (targetFormats.isNotEmpty()) target.targetFormats = targetFormats

    linux.mergeTo(target.linux)
    macOS.mergeTo(target.macOS)
    windows.mergeTo(target.windows)
}

internal fun DistributionInfoBuilderExtension.mergeTo(target: JvmApplicationDistributions) {
    packageName?.let { target.packageName = it }
    packageVersion?.let { target.packageVersion = it }
    copyright?.let { target.copyright = it }
    description?.let { target.description = it }
    vendor?.let { target.vendor = it }
    appResourcesRootDir.mergeTo(target.appResourcesRootDir)
    licenseFile.mergeTo(target.licenseFile)
    if (targetFormats.isNotEmpty()) target.targetFormats = targetFormats

    linux.mergeTo(target.linux)
    macOS.mergeTo(target.macOS)
    windows.mergeTo(target.windows)
}

/**
 * 自身已赋值时写入 [target]
 */
private fun <T> Property<T>.mergeTo(target: Property<T>) {
    if (isPresent) target.set(this)
}

/**
 * boolean 无法表达“未配置”状态，只有与 [default] 不同的值才视为已配置
 */
private fun Boolean.mergeTo(target: Boolean, default: Boolean): Boolean {
    return if (this == default) target else this
}

private fun AbstractPlatformSettings.mergePlatformSettingsTo(target: AbstractPlatformSettings) {
    iconFile.mergeTo(target.iconFile)
    packageVersion?.let { target.packageVersion = it }
    installationPath?.let { target.installationPath = it }
}

internal fun LinuxPlatformSettings.mergeTo(target: LinuxPlatformSettings) {
    mergePlatformSettingsTo(target)
    target.shortcut = shortcut.mergeTo(target.shortcut, default = false)
    packageName?.let { target.packageName = it }
    appRelease?.let { target.appRelease = it }
    appCategory?.let { target.appCategory = it }
    debMaintainer?.let { target.debMaintainer = it }
    menuGroup?.let { target.menuGroup = it }
    rpmLicenseType?.let { target.rpmLicenseType = it }
    debPackageVersion?.let { target.debPackageVersion = it }
    rpmPackageVersion?.let { target.rpmPackageVersion = it }
}

internal fun WindowsPlatformSettings.mergeTo(target: WindowsPlatformSettings) {
    mergePlatformSettingsTo(target)
    target.console = console.mergeTo(target.console, default = false)
    target.dirChooser = dirChooser.mergeTo(target.dirChooser, default = true)
    target.perUserInstall = perUserInstall.mergeTo(target.perUserInstall, default = false)
    target.shortcut = shortcut.mergeTo(target.shortcut, default = false)
    target.menu = menu.mergeTo(target.menu, default = false)
    menuGroup?.let { target.menuGroup = it }
    upgradeUuid?.let { target.upgradeUuid = it }
    msiPackageVersion?.let { target.msiPackageVersion = it }
    exePackageVersion?.let { target.exePackageVersion = it }
}

private fun AbstractMacOSPlatformSettings.mergeMacOSSettingsTo(target: AbstractMacOSPlatformSettings) {
    mergePlatformSettingsTo(target)
    packageName?.let { target.packageName = it }
    packageBuildVersion?.let { target.packageBuildVersion = it }
    dmgPackageVersion?.let { target.dmgPackageVersion = it }
    dmgPackageBuildVersion?.let { target.dmgPackageBuildVersion = it }
    appCategory?.let { target.appCategory = it }
    minimumSystemVersion?.let { target.minimumSystemVersion = it }
    bundleID?.let { target.bundleID = it }
    signing.mergeTo(target.signing)
    notarization.mergeTo(target.notarization)
}

internal fun JvmMacOSPlatformSettings.mergeTo(target: JvmMacOSPlatformSettings) {
    mergeMacOSSettingsTo(target)
    dockName?.let { target.dockName = it }
    target.setDockNameSameAsPackageName =
        setDockNameSameAsPackageName.mergeTo(target.setDockNameSameAsPackageName, default = true)
    target.appStore = appStore.mergeTo(target.appStore, default = false)
    entitlementsFile.mergeTo(target.entitlementsFile)
    runtimeEntitlementsFile.mergeTo(target.runtimeEntitlementsFile)
    pkgPackageVersion?.let { target.pkgPackageVersion = it }
    pkgPackageBuildVersion?.let { target.pkgPackageBuildVersion = it }
    provisioningProfile.mergeTo(target.provisioningProfile)
    runtimeProvisioningProfile.mergeTo(target.runtimeProvisioningProfile)
}

private fun MacOSSigningSettings.mergeTo(target: MacOSSigningSettings) {
    // sign 总是有值（默认 false 或来自系统属性），因此只有 true 才视为已配置
    if (sign.get()) target.sign.set(true)
    identity.mergeTo(target.identity)
    keychain.mergeTo(target.keychain)
    prefix.mergeTo(target.prefix)
}

private fun MacOSNotarizationSettings.mergeTo(target: MacOSNotarizationSettings) {
    appleID.mergeTo(target.appleID)
    password.mergeTo(target.password)
    teamID.mergeTo(target.teamID)
}

//</editor-fold>
