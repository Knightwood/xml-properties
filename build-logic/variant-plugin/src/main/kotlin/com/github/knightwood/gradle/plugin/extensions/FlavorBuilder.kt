package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.container

abstract class FlavorCollectionExtension : ExtensionAware {
    @get:Inject
    abstract val objectFactory: ObjectFactory

    @get:Inject
    abstract val logger: Logger

    private val variantContainer: NamedDomainObjectContainer<FlavorBuilder> =
        objectFactory.domainObjectContainer(FlavorBuilder::class.java)

    fun create(name: String, action: FlavorBuilder.() -> Unit) {
        if (variantContainer.findByName(name) == null) {
            variantContainer.create(name, action)
        } else {
            logger.warn("Flavor $name already exists")
        }
    }

    //<editor-fold desc="内部api">
    internal fun parseBuilder(flavor: String): FlavorBuilder? {
        return variantContainer.findByName(flavor)
    }
    //</editor-fold>

}

abstract class VariantBuilder {
    @get:Inject
    abstract val objectFactory: ObjectFactory
    val distributionInfoBuilder: DistributionInfoBuilderExtension =
        objectFactory.newInstance(DistributionInfoBuilderExtension::class.java)

    fun distribution(action: Action<DistributionInfoBuilderExtension>) {
        action.execute(distributionInfoBuilder)
    }

    val sourceSetBuilder: SourceSetCollectionExtension =
        objectFactory.newInstance(SourceSetCollectionExtension::class.java)

    fun sourceSets(action: Action<SourceSetCollectionExtension>) {
        action.execute(sourceSetBuilder)
    }

    val buildConfigBuilder: BuildConfigFieldsBundleCollectionExtension =
        objectFactory.newInstance(BuildConfigFieldsBundleCollectionExtension::class.java)

    /**
     * 最终会根据配置生成 BuildConfig.kt 文件
     *
     * @param action
     */
    fun buildConfigs(action: Action<BuildConfigFieldsBundleCollectionExtension>) {
        action.execute(buildConfigBuilder)
    }
}

abstract class FlavorBuilder(val name: String) : VariantBuilder()
