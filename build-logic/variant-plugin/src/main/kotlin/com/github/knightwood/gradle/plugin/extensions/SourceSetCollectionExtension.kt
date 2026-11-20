package com.github.knightwood.gradle.plugin.extensions

import com.github.knightwood.gradle.plugin.model.CustomSourceSet
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
