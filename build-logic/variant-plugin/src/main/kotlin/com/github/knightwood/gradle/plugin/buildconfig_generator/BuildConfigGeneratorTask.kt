package com.github.knightwood.gradle.plugin.buildconfig_generator

import com.squareup.kotlinpoet.ClassName
//import com.github.knightwood.gradle.plugin.extensions.BuildConfigField
//import com.github.knightwood.gradle.plugin.extensions.BuildConfigFieldBundleBuilder
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
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
     * 当前 project 中实际存在的源集名称，仅用于过滤需要生成文件的 bundle。
     * 只保存字符串，避免把 KGP 的活模型对象写进任务状态（配置缓存无法序列化）。
     */
    @get:Input
    abstract val targetSourceSetNames: SetProperty<String>

    /**
     * 针对所有 target 的 build config 字段，使用可序列化的最小数据模型
     */
    @get:Input
    abstract val allTargetBuildConfigFields: ListProperty<BuildConfigFieldBundleBuilder>

    @TaskAction
    fun run() {
        val packageName = buildConfigPackageName.get()
        val className = buildConfigClassName.get()
        val sourceSetNames = targetSourceSetNames.get()

        allTargetBuildConfigFields.get().forEach { bundle ->
            //仅在有对应源集的 bundle 才生成文件
            if (bundle.name !in sourceSetNames) return@forEach
            // 拼接后的路径是相对于project/build目录的路径
            // 例如：build/generated/source/buildConfig/commonMain/kotlin/
            val destDirPath =
                outputFolder.get().dir("${bundle.name}${File.separator}kotlin${File.separator}")

            // 生成build config文件
            // 确定文件名，如果使用了kmp插件，有多个平台的源集，且isExpect不为null，
            // 则commonMain/main target 使用BuildConfig.kt;
            // jvmMain等非公共源集名称上追加target名称，例如 BuildConfig.jvmMain.kt
            val fileName = when (bundle.isExpect) {
                false -> "$className.${bundle.name}"
                else -> className
            }

            FileSpec.builder(packageName, fileName)
                .addType(buildConfigType(className, bundle))
                .build()
                .writeTo(destDirPath.asFile)
        }
    }

    /**
     * 生成 object BuildConfig，字段按 [bundle] 中的配置填充
     */
    private fun buildConfigType(
        className: String,
        bundle: BuildConfigFieldBundleBuilder,
    ): TypeSpec {
        val isExpect = bundle.isExpect
        val typeBuilder = TypeSpec.objectBuilder(className)
        when (bundle.isExpect) {
            true -> typeBuilder.addModifiers(KModifier.EXPECT)
            false -> typeBuilder.addModifiers(KModifier.ACTUAL)
            null -> Unit
        }
        bundle.fields.forEach { field ->
            typeBuilder.addProperty(buildConfigProperty(field, isExpect))
        }
        return typeBuilder.build()
    }

    /**
     * expect 声明的属性不能带初始化器，因此 isExpect 时只声明类型，不填充值，
     * 同时也无法使用 const 修饰符（const 要求必须有初始值）。
     * @param isExpect 是否为 expect 声明，null 表示不指定
     */
    private fun buildConfigProperty(field: BuildConfigField, isExpect: Boolean?): PropertySpec {
        val type = ClassName.bestGuess(field.typeName).copy(nullable = field.value == null)
//        val type = field.cls.copy(nullable = field.value == null)
        val builder = PropertySpec.builder(field.name, type)
        if (isExpect == null) {
            builder.initializer(field.initializer())
        } else {
            if (!isExpect) { //actual字段
                builder.addModifiers(KModifier.ACTUAL)
                builder.initializer(field.initializer())
            } else {
                builder.addModifiers(KModifier.EXPECT)
            }
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
//            is IntRange -> CodeBlock.of("%L..%L", v.first, v.last)
//            is LongRange -> CodeBlock.of("%LL..%LL", v.first, v.last)
//            is CharRange -> CodeBlock.of("'%L'..'%L'", v.first.escaped(), v.last.escaped())
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

/**
 * 写入任务状态的 bundle 快照，只包含可序列化的基础数据
 */
data class BuildConfigBundleInput(
    val name: String,
    val isExpect: Boolean?,
    val fields: List<BuildConfigFieldInput>,
) : java.io.Serializable

data class BuildConfigFieldInput(
    val name: String,
    /** 完整类型名，例如 kotlin.Int、kotlin.String、kotlin.ranges.IntRange */
    val typeName: String,
    val value: java.io.Serializable?,
) : java.io.Serializable
typealias BuildConfigFieldBundleBuilder = BuildConfigBundleInput
typealias BuildConfigField = BuildConfigFieldInput
