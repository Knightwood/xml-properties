package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import javax.inject.Inject

abstract class SourceSetCollectionExtension: ExtensionAware  {
    @get:Inject
    abstract val objects: ObjectFactory

    internal val container = objects.domainObjectContainer<CustomSourceSet>(CustomSourceSet::class.java)

    /**
     * Returns a [CustomSourceSet] builder for the given [target].
     *
     * @param target 构建目标，比如commonMain,androidMain,jvmMain等
     * @param action 配置 [CustomSourceSet] 的 lambda 表达式
     * @receiver [SourceSetCollectionExtension]
     */
    fun create(target: String, action: CustomSourceSet.() -> Unit) {
        container.create(target, action)
    }

}

/**
 * 定义某构建变体/风味的源码目录
 * ```
 * 目录结构：
 * module A
 *   |- build/
 *   |- src/
 *   |- build.gradle.kts
 *   |- builtSrc // 存放各个风味的自定义源码目录
 *        |- free/  // 自定义风味名称
 *            |- commonMain/
 *                 |- kotlin/  kotlin源码
 *                 |- resources/ 资源文件
 *            |- jvmMain/
 *                 |- kotlin/  kotlin源码
 *                 |- resources/ 资源文件
 *
 *  sourceSets {
 *      create("commonMain") {
 *          sourceDir = "buildSrc/free/commonMain/kotlin"
 *      }
 *      create("jvmMain") {
 *          sourceDir = "buildSrc/free/jvmMain/kotlin"
 *          resourceDir = "buildSrc/free/jvmMain/resources"
 *      }
 *  }
 *
 *```
 * @param name 某构建目标名称，比如commonMain， androidMain，jvmMain等
 *
 */
abstract class CustomSourceSet(val name: String) {

    /**
     * kotlin、java源码路径, 相对于当前module project的路径，例如 src/main/kotlin
     * The source directory. This is evaluated as per {@link org.gradle.api.Project#files(Object...)}
     */
    var sourceDir: String? = null

    /**
     * 资源文件路径, 相对于当前module project的路径，例如 src/main/res
     * The source directory. This is evaluated as per {@link org.gradle.api.Project#files(Object...)}
     */
    var resourceDir: String? = null

    /**
     * The source directory. This is evaluated as per {@link org.gradle.api.Project#files(Object...)}
     * 相对于当前module project的路径，例如 src/main/composeResource
     *
     * compose资源路径，无法直接添加到源集，需要手动获取当前Variant配置并依照compose multiplatform插件配置
     * ```
     * val variant = ...
     * compose.resources {
     *     customDirectory(
     *         sourceSetName = "jvmMain",
     *         directoryProvider = provider { layout.projectDirectory.dir(variant.composeResourcesDir) }
     *     )
     * }
     * ```
     */
    var composeResourceDir: String? = null
}
