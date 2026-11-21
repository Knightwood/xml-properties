package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.NamedDomainObjectContainer
import javax.inject.Inject
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware

abstract class BuildTypeCollectionExtension: ExtensionAware  {
    @get:Inject
    abstract val objectFactory: ObjectFactory
    @get:Inject
    abstract val logger: Logger

    private val variantContainer: NamedDomainObjectContainer<BuiltTypeBuilder> =
        objectFactory.domainObjectContainer(BuiltTypeBuilder::class.java)

    fun create(name: String, action: BuiltTypeBuilder.() -> Unit) {
        if (variantContainer.findByName(name) == null) {
            variantContainer.create(name, action)
        } else {
            logger.warn("BuiltType $name already exists")
        }
    }

    fun release(action: BuiltTypeBuilder.() -> Unit) {
        create("release", action)
    }

    fun debug(action: BuiltTypeBuilder.() -> Unit) {
        create("debug", action)
    }

    //<editor-fold desc="内部api">

    internal fun parseBuilder(buildType: String): BuiltTypeBuilder? {
        return variantContainer.findByName(buildType)
    }

    internal fun parseBuilderByIsDebug(isDebug: Boolean): BuiltTypeBuilder {
        addDefaultBuilder()
        return variantContainer.findByName(if (isDebug) "debug" else "release")!!
    }

    internal fun addDefaultBuilder() {
        if (variantContainer.findByName("release") == null) {
            variantContainer.create("release") {
                debug = false
            }
        }
        if (variantContainer.findByName("debug") == null) {
            variantContainer.create("debug") {
                debug = true
            }
        }
    }

    //</editor-fold>
}

/**
 * Built type builder
 *
 * 存储用户配置的BuiltType信息，用于后续转换为BuiltType
 *
 * @constructor Creates a new BuiltTypeBuilder
 * @property name
 */
abstract class BuiltTypeBuilder constructor(val name: String) : VariantBuilder() {
    var debug: Boolean = false
}
