package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.container

abstract class FlavorCollectionExtension @Inject constructor(
    objectFactory: ObjectFactory,
    val project: Project,
) {
    private val variantContainer: NamedDomainObjectContainer<FlavorBuilder> =
        project.container(FlavorBuilder::class)

    fun create(name: String, action: FlavorBuilder.() -> Unit) {
        if (variantContainer.findByName(name) == null) {
            variantContainer.create(name, action)
        } else {
            project.logger.warn("Flavor $name already exists")
        }
    }

    //<editor-fold desc="内部api">
    internal fun parseBuilder(flavor: String): FlavorBuilder? {
        return variantContainer.findByName(flavor)
    }
    //</editor-fold>

}

abstract class VariantBuilder {

    internal abstract val distributionInfoBuilder: DistributionInfoBuilderExtension
    fun distribution(action: Action<DistributionInfoBuilderExtension>) {
        action.execute(distributionInfoBuilder)
    }

    internal abstract val sourceSetBuilder: SourceSetCollectionExtension
    fun sourceSets(action: Action<SourceSetCollectionExtension>) {
        action.execute(sourceSetBuilder)
    }

    internal abstract val buildConfigBuilder: BuildConfigFieldsBundleCollectionExtension

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
