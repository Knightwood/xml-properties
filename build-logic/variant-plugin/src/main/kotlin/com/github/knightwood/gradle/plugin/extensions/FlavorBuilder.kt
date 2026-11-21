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
    internal val distributionInfoBuilder: DistributionInfoBuilderExtension =
        objectFactory.newInstance(DistributionInfoBuilderExtension::class.java)

    fun distribution(action: Action<DistributionInfoBuilderExtension>) {
        action.execute(distributionInfoBuilder)
    }

    internal val sourceSetBuilder: SourceSetCollectionExtension =
        objectFactory.newInstance(SourceSetCollectionExtension::class.java)

    fun sourceSets(action: Action<SourceSetCollectionExtension>) {
        action.execute(sourceSetBuilder)
    }

    internal val buildConfigBuilder: BuildConfigFieldsBundleCollectionExtension =
        objectFactory.newInstance(BuildConfigFieldsBundleCollectionExtension::class.java)

    /**
     * 最终会根据配置生成 BuildConfig.kt 文件
     *
     * 如果使用kotlin jvm 插件，则不要指定expect，让它保持为null即可
     * ```
     * buildConfigs {
     *     packageName = "com.github.knightwood"
     *     className = "BuildConfig"
     *
     *     create("main") {
     *         expect = null
     *         this + intField("AGE", 18)
     *         this + booleanField("IS_DEBUG", false)
     *         this + stringField("GENDER", null)
     *     }
     *  }
     * ```
     *
     * 如果使用kotlin kmp 插件，则需在commonMain target块中指定expect为true，在jvmMain target块中指定expect为false，
     * 且在commonMain target块中定义的字段，在jvmMain target块中不能重复定义
     * 在生成BuildConfig代码文件时会将全部target中定义的字段合并到一起，所以你可以不在commonMain中定义任何字段
     * ```
     * buildConfigs {
     *     packageName = "com.github.knightwood"
     *     className = "BuildConfig"
     *
     *     reate("commonMain") {
     *         expect = true
     *     }
     *
     *     create("jvmMain") {
     *         expect = false
     *         this + stringField("NAME", "Knightwood")
     *         this + intField("AGE", 18)
     *         this + booleanField("IS_DEBUG", false)
     *         this + stringField("GENDER", null)
     *     }
     *  }
     * ```
     *
     * @param action
     */
    fun buildConfigs(action: Action<BuildConfigFieldsBundleCollectionExtension>) {
        action.execute(buildConfigBuilder)
    }
}

abstract class FlavorBuilder(val name: String) : VariantBuilder()
