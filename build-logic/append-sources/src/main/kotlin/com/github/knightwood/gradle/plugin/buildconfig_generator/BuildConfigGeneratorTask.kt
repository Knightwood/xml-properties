package com.github.knightwood.gradle.plugin.buildconfig_generator

import com.github.knightwood.gradle.plugin.extensions.BuildConfigFieldBundleBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.file.File

abstract class BuildConfigGeneratorTask : DefaultTask() {
    @get:Input
    abstract val buildConfigPackageName: Property<String>

    @get:Input
    abstract val buildConfigClassName: Property<String>

    /**
     * 文件输出基础路径, 相对于当前project
     * 例如 outputFolder 为 build/generated/buildConfig
     *
     * 最终生成文件将输出到基础路径某target为名的文件夹里
     * outputFolder
     *      |
     *      |--- commonMain
     *          |--- kotlin
     *              |--- com/github/knightwood/buildconfig
     *                  |--- BuildConfig.kt
     *      |--- jvmMain
     *          |--- kotlin
     *              |--- com/github/knightwood/buildconfig
     *                  |--- BuildConfig.jvm.kt
     */
    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    /**
     * Target names
     */
    @get:Input
    abstract val targetNames: ListProperty<String>

    /**
     * 针对所有target的build config字段
     */
    @get:Input
    abstract val allTargetBuildConfigFields: ListProperty<BuildConfigFieldBundleBuilder>

    @TaskAction
    fun run() {
        val result = targetNames.map { targets ->
            targets.map { target: String ->
                // 拼接后的路径是相对于project/build目录的路径
                // 例如：
                // build/generated/buildConfig/commonMain/kotlin/
                // 最终包名、类名将传入kotlin poet，由kotlin poet生成包路径和kt文件
                val destPath = outputFolder.get().dir("$target${File.separator}kotlin${File.separator}")

                destPath
            }
        }
    }
}
