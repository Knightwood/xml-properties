package com.github.knightwood.gradle.plugin

import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.LibraryVariant

internal const val IDEA_IMPORT_TASK_NAME = "prepareKotlinIdeaImport"
internal const val KOTLIN_MPP_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
internal const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"
internal const val KOTLIN_ANDROID_PLUGIN_ID = "org.jetbrains.kotlin.android"
internal const val KOTLIN_JS_PLUGIN_ID = "org.jetbrains.kotlin.js"
internal const val COMPOSE_PLUGIN_ID = "org.jetbrains.compose"
internal const val BASE_DIR = "builtSrc"
internal const val DefaultBuiltTypeFile = "builtType.json"

/**
 * 定义某构建变体的源码目录
 * ```
 * 目录结构：
 * module A
 *   |- build/
 *   |- src/
 *   |- build.gradle.kts
 *   |- builtSrc
 *        |- builtType.json
 *        |- prod/  构建类型的名称，对应dirName
 *            |- kt/  kotlin源码
 *            |- res/ 资源文件
 *```
 * @param dirName 存放某构建变体源码和资源文件的顶层文件夹名 例如 src_prod，匹配prod构建变体
 * 在编译过程中会根据当前构建变体匹配目标文件夹名称，将其源码文件夹和资源文件夹追加到源集
 */
@JvmInline
value class DirectorySet(
    val dirName: String,
) {
    fun ktDir(): String {
        return "$BASE_DIR/$dirName/kt"
    }

    fun resDir(): String {
        return "$BASE_DIR/$dirName/res"
    }
}

/**
 *
 * @param name 用于匹配[DirectorySet.dirName]
 * @param isDebug 当前是否是debug版本
 */
data class BuiltType(
    val isDebug: Boolean,
    val name: String,
) {
    companion object {
        val release = BuiltType(false, "release")
        val debug = BuiltType(true, "debug")
    }
}

interface IAppendSources {
    /**
     * 当前BuildType
     *
     * 除了直接使用[builtType]指定当前构建类型，也可以在文件中指定当前构建类型
     * 默认位于builtSrc/builtType.json
     *
     * 多个构建变体对应的源码、资源目录需要放到与src同级的builtSrc文件夹下
     * ```
     * module A
     *   |- src/
     *   |- builtSrc
     *        |- prod/
     *            |- kt/
     *            |- res/
     *        |- debug/
     *            |- kt/
     *            |- res/
     *```
     */
    var builtType: BuiltType

    /**
     * 将Android中的构建变体转换为BuildType
     * 转换之后，将BuildType对应的源码目录追加到Android当前构建变变体的源集
     *
     * 若不指定，则按当前[builtType]追加源集
     *
     * 在agp9或者哪个版本开始，Android app module不再支持与kmp代码放在一个module
     * 但此转换依旧有用
     */
    var androidAppBuildTypeConvertor: ((ApplicationVariant) -> BuiltType)?

    /**
     * 将Android中的构建变体转换为BuildType
     * 转换之后，将BuildType对应的源码目录追加到Android当前构建变变体的源集
     *
     * 若不指定，则按当前[builtType]追加源集
     */
    var androidLibBuildTypeConvertor: ((LibraryVariant) -> BuiltType)?
}


abstract class AppendSources : IAppendSources {
    override var androidAppBuildTypeConvertor: ((ApplicationVariant) -> BuiltType)? = null
    override var androidLibBuildTypeConvertor: ((LibraryVariant) -> BuiltType)? = null
    override var builtType: BuiltType = BuiltType.debug
}
