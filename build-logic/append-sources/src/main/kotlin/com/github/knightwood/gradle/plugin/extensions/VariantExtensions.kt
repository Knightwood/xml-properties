package com.github.knightwood.gradle.plugin.extensions

import com.android.build.gradle.internal.cxx.configure.buildTypeOf
import com.github.knightwood.gradle.plugin.model.CustomSourceSet
import org.gradle.api.Action
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

/**
 * Variant extensions
 *
 * 构建变体由 buildType 和 flavor 组成, 其中flavor是可选配置，
 * 每个buildType、flavor都可以设置一组DirectorySet添加到源集，
 * 以及使用ParamsBundle生成类似安卓中的BuildConfig类，
 * 并且在由flavor存在时，flavor中的配置将覆盖buildType中的配置。
 *
 * 当前激活的构建变体由 activeVariant 属性指定，命名规则为 "buildType-flavor"，
 * 使用 activeVariant.set("dev-free") 激活 "dev-free" 变体。
 *
 * 示例：
 * ```
 * projectVariant {
 *     activeVariant = "dev-free"
 *
 *     buildType {
 *
 *        release { //名为release的构建类型
 *            // configuration
 *        }
 *
 *        // other variants
 *        create("dev") {
 *            // configuration
 *        }
 *     }
 *
 *     flavor {
 *
 *        create("free"){
 *            // configuration
 *        }
 *
 *        create("paid"){
 *            // configuration
 *        }
 *     }
 * }
 *
 * ```
 *
 * 在module的build.gradle.kts中使用当前激活的构建变体信息
 * ```
 * val activeVariant = getActiveVariant()
 *
 * ```
 *
 *
 * @constructor
 * @property project
 * @param objectFactory the object factory
 */
abstract class VariantExtensions @Inject constructor(
    val objectFactory: ObjectFactory,
    val project: Project,
) : ExtensionAware {
    abstract val activeVariant: Property<String>
    internal abstract val buildTypeCollectionExtension: BuildTypeCollectionExtension
    internal abstract val flavorCollectionExtension: FlavorCollectionExtension

    fun buildType(action: Action<BuildTypeCollectionExtension>) {
        action.execute(buildTypeCollectionExtension)
    }

    fun flavor(action: Action<FlavorCollectionExtension>) {
        action.execute(flavorCollectionExtension)
    }

    //<editor-fold desc="内部api">

    /**
     * 根据[activeVariant]解析对应的构建类型和风味配置，返回合并后的配置
     */
    fun parseVariant(): MergedVariantExtension {
        val mergedConfiguration: MergedVariantExtension =
            objectFactory.newInstance<MergedVariantExtension>(MergedVariantExtension::class.java)
        val (buildTypeName, flavorName) = activeVariant.get().split("-")
        val builtType = with(buildTypeCollectionExtension) {
            parseBuilder(buildTypeName) ?: parseBuilderByIsDebug(true)
        }
        val flavor = flavorCollectionExtension.parseBuilder(flavorName)
        mergedConfiguration.apply {
            isDebug.set(builtType.isDebug)
            this.buildTypeName.set(builtType.name)
            this.flavorName.set(flavor?.name)
            distributionInfo.linux {
                this.appCategory = builtType.distributionInfoBuilder.linux.appCategory
            }

        }

        return mergedConfiguration
    }
    //</editor-fold>
}

abstract class MergedVariantExtension {
    @get:Inject
    internal abstract val objects: ObjectFactory

    abstract val isDebug: Property<Boolean>
    abstract val buildTypeName: Property<String>
    abstract val flavorName: Property<String?>
    abstract val distributionInfo: DistributionInfoBuilderExtension
    val customSourceSets = objects.domainObjectContainer<CustomSourceSet>(CustomSourceSet::class.java)
    val buildConfigFields = objects.domainObjectContainer(BuildConfigFieldBundleBuilder::class.java)
    abstract val buildConfigPackageName: Property<String>
    abstract val buildConfigClassName: Property<String>
}

private fun VariantExtensions.example() {
    activeVariant.set("dev-free")
    buildType {
        release {
            // configuration
        }
        create("dev") {
            distribution {
                linux {
                    // configuration
                }
                macOS {
                    // configuration
                }
            }
            buildConfigs {
                packageName = "com.github.knightwood"
                className = "BuildConfig"

                create("commonMain") {
                    isExpect = true
                    this + intField("AGE", 18)
                    this + booleanField("IS_DEBUG", false)
                    this + stringField("GENDER", null)
                    this + field<IntRange>("AGE", 2..10)
                }
                create("jvmMain") {
                    isExpect = false
                    this + stringField("NAME", "Knightwood")
                }
            }
        }
    }
    flavor {
        this.create("free") {
            // configuration
            sourceSets {
                create("commonMain") {
                    sourceDir = "path1/kt"
                }
                create("jvmMain") {
                    sourceDir = "path2/kt"
                    resourceDir = "path/res"
                }
            }
        }
    }
}
