package com.github.knightwood.gradle.plugin.buildconfig_generator

import com.github.knightwood.gradle.plugin.extensions.BuildConfigField
import com.github.knightwood.gradle.plugin.extensions.BuildConfigFieldBundleBuilder
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

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
     * 目前project配置的全部target的名称
     */
    @get:Input
    abstract val allTargetKotlinSourceSets: ListProperty<KotlinSourceSet>

    /**
     * 针对所有target的build config字段
     */
    @get:Input
    abstract val allTargetBuildConfigFields: ListProperty<BuildConfigFieldBundleBuilder>

    @TaskAction
    fun run() {
        val packageName = buildConfigPackageName.get()
        val className = buildConfigClassName.get()
        val kotlinSourceSets = allTargetKotlinSourceSets.get()
        val targetNames = kotlinSourceSets.map { it.name }.toSet()

        allTargetBuildConfigFields.get().forEach { bundleBuilder ->
            val sourceSet = kotlinSourceSets.firstOrNull { it.name == bundleBuilder.name }
            //仅在有对应源集的 bundle 才生成文件
            if (sourceSet != null) {
                // 拼接后的路径是相对于project/build目录的路径
                // 例如：
                // build/generated/buildConfig/commonMain/kotlin/
                // 最终包名、类名将传入kotlin poet，由kotlin poet生成包路径和kt文件
                val destDirPath =
                    outputFolder.get().dir("${bundleBuilder.name}${File.separator}kotlin${File.separator}")

                // 生成build config文件
                // 确定文件名，如果使用了kmp插件，有多个平台的源集，且isExpect不为null，
                // 则commonMain/main target 使用BuildConfig.kt;
                // jvmMain等非公共源集名称上追加target名称，例如 BuildConfig.jvmMain.kt
                // 注意：这里不带.kt后缀，由kotlin poet在写文件时补充
                val fileName = when (bundleBuilder.isExpect) {
                    false -> "$className.${bundleBuilder.name}"
                    else -> className
                }

                FileSpec.builder(packageName, fileName)
                    .addType(buildConfigType(className, bundleBuilder))
                    .build()
                    .writeTo(destDirPath.asFile)
                // 将生成的文件夹添加到源集目录
                sourceSet.kotlin.srcDir(destDirPath.asFile)
            }
        }
    }

    /**
     * 生成 object BuildConfig，字段按 [bundleBuilder] 中的配置填充
     */
    private fun buildConfigType(
        className: String,
        bundleBuilder: BuildConfigFieldBundleBuilder,
    ): TypeSpec {
        val isExpect = bundleBuilder.isExpect == true
        val typeBuilder = TypeSpec.objectBuilder(className)
        when (bundleBuilder.isExpect) {
            true -> typeBuilder.addModifiers(KModifier.EXPECT)
            false -> typeBuilder.addModifiers(KModifier.ACTUAL)
            null -> Unit
        }
        bundleBuilder.fields.forEach { field ->
            typeBuilder.addProperty(buildConfigProperty(field, isExpect))
        }
        return typeBuilder.build()
    }

    /**
     * expect 声明的属性不能带初始化器，因此 isExpect 时只声明类型，不填充值，
     * 同时也无法使用 const 修饰符（const 要求必须有初始值）。
     */
    private fun buildConfigProperty(field: BuildConfigField, isExpect: Boolean): PropertySpec {
        val type = field.cls.asTypeName().copy(nullable = field.value == null)
        val builder = PropertySpec.builder(field.name, type)
        if (!isExpect) {
            builder.initializer(field.initializer())
        }
        return builder.build()
    }

    /**
     * 将字段值转换为kotlin代码中的字面量
     */
    private fun BuildConfigField.initializer(): CodeBlock {
        return when (val v = value) {
            null -> CodeBlock.of("null")
            is String -> CodeBlock.of("%S", v)
            is Char -> CodeBlock.of("'%L'", v.escaped())
            is Float -> CodeBlock.of("%Lf", v)
            is Double -> CodeBlock.of("%L", v)
            is Long -> CodeBlock.of("%LL", v)
            is IntRange -> CodeBlock.of("%L..%L", v.first, v.last)
            is LongRange -> CodeBlock.of("%LL..%LL", v.first, v.last)
            is CharRange -> CodeBlock.of("'%L'..'%L'", v.first.escaped(), v.last.escaped())
            else -> CodeBlock.of("%L", v)
        }
    }

    private fun Char.escaped(): String = when (this) {
        '\\' -> "\\\\"
        '\'' -> "\\'"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> toString()
    }
}
