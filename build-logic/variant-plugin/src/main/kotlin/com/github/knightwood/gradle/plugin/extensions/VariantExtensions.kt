package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.Action
import javax.inject.Inject
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions

/**
 * buildType 与 flavor 在构建变体名称中的分隔符
 */
private const val VARIANT_SEPARATOR = "-"

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
    internal val buildTypeCollectionExtension: BuildTypeCollectionExtension =
        objectFactory.newInstance(BuildTypeCollectionExtension::class.java)
    internal val flavorCollectionExtension: FlavorCollectionExtension =
        objectFactory.newInstance(FlavorCollectionExtension::class.java)

    fun buildType(action: Action<BuildTypeCollectionExtension>) {
        action.execute(buildTypeCollectionExtension)
    }

    fun flavor(action: Action<FlavorCollectionExtension>) {
        action.execute(flavorCollectionExtension)
    }

    //<editor-fold desc="内部api">

    /**
     * 根据[activeVariant]解析对应的[BuiltTypeBuilder]和[FlavorBuilder]，
     * 返回合并[BuiltTypeBuilder]和[FlavorBuilder]后的最终配置
     *
     * 合并规则（优先级 flavor > buildType）：
     * - distribution：逐项合并，flavor 中已配置的项覆盖 buildType 中的同名项
     * - sourceSets：按源集名称合并，flavor 中非 null 的字段覆盖 buildType 中同名字段
     * - buildConfigs：包名、类名以 flavor 优，按 bundle 名称合并字段，同名字段以 flavor 优
     */
    fun parseVariant(): MergedVariantExtension {
        val mergedConfiguration: MergedVariantExtension =
            objectFactory.newInstance<MergedVariantExtension>(MergedVariantExtension::class.java)
        val variantName = activeVariant.orNull
            ?: throw InvalidUserDataException(
                "activeVariant is not configured, set it like: activeVariant.set(\"debug\") or activeVariant.set(\"debug-free\")"
            )
        // 构建变体命名规则为 "buildType-flavor"，flavor 可以省略
        val buildTypeName = variantName.substringBefore(VARIANT_SEPARATOR)
        val flavorName = variantName.substringAfter(VARIANT_SEPARATOR, "").ifEmpty { null }

        val builtType = with(buildTypeCollectionExtension) {
            parseBuilder(buildTypeName) ?: run {
                logger.warn("buildType $buildTypeName not found, fallback to debug buildType")
                parseBuilderByIsDebug(true)
            }
        }
        val flavor = flavorName?.let { name ->
            flavorCollectionExtension.parseBuilder(name) ?: run {
                project.logger.warn("flavor $name not found, only buildType configuration is applied")
                null
            }
        }

        //合并配置，优先级：flavor > buildType
        mergedConfiguration.apply {
            debug.set(builtType.debug)
            this.buildTypeName.set(builtType.name)
            this.flavorName.set(flavor?.name)

            //先写入buildType，再写入flavor，后写入的已配置项覆盖先写入的
            builtType.distributionInfoBuilder.mergeTo(distributionInfo)
            flavor?.distributionInfoBuilder?.mergeTo(distributionInfo)

            builtType.sourceSetBuilder.mergeTo(customSourceSets)
            flavor?.sourceSetBuilder?.mergeTo(customSourceSets)

            builtType.buildConfigBuilder.mergeTo(buildConfigFields)
            flavor?.buildConfigBuilder?.mergeTo(buildConfigFields)
            organizeBuildConfigFieldsBundleContainers(buildConfigFields)
            buildConfigPackageName.set(
                flavor?.buildConfigBuilder?.packageName ?: builtType.buildConfigBuilder.packageName
            )
            buildConfigClassName.set(
                flavor?.buildConfigBuilder?.className ?: builtType.buildConfigBuilder.className
            )
        }
        return mergedConfiguration
    }
    //</editor-fold>
}

/**
 * 将此插件生成的mergedVariantExtension配置同步到compose插件的JvmApplicationDistributions
 *
 * @receiver the mergedVariantExtension
 * @param distribution the compose plugin JvmApplicationDistributions
 */
fun MergedVariantExtension.syncTo(distribution: JvmApplicationDistributions) {
    this.distributionInfo.mergeTo(distribution)
}

abstract class MergedVariantExtension : ExtensionAware {
    @get:Inject
    abstract val objects: ObjectFactory

    abstract val debug: Property<Boolean>
    abstract val buildTypeName: Property<String>
    abstract val flavorName: Property<String?>
    val distributionInfo: DistributionInfoBuilderExtension = objects.newInstance(DistributionInfoBuilderExtension::class.java)
    val customSourceSets = objects.domainObjectContainer<CustomSourceSet>(CustomSourceSet::class.java)
    val buildConfigFields = objects.domainObjectContainer(BuildConfigFieldBundleBuilder::class.java)
    abstract val buildConfigPackageName: Property<String>
    abstract val buildConfigClassName: Property<String>
}

//<editor-fold desc="合并配置">

/**
 * 将当前源集配置写入 [target]，同名源集中已配置的字段会覆盖 [target] 中的旧值。
 *
 * 先合并 buildType 的源集，再合并 flavor 的源集，即可得到 flavor > buildType 的结果。
 */
private fun SourceSetCollectionExtension.mergeTo(target: NamedDomainObjectContainer<CustomSourceSet>) {
    container.forEach { sourceSet ->
        val merged = target.findByName(sourceSet.name)
        if (merged == null) {
            target.create(sourceSet.name) { copyFrom(sourceSet) }
        } else {
            merged.copyFrom(sourceSet)
        }
    }
}

private fun CustomSourceSet.copyFrom(source: CustomSourceSet) {
    source.sourceDir?.let { sourceDir = it }
    source.resourceDir?.let { resourceDir = it }
    source.composeResourceDir?.let { composeResourceDir = it }
}

/**
 * 将用户配置的每个target的BuildConfig字段组 写入 [targetContainer]
 * 多次调用此方法时，同名字段以当前配置（targetContainer）优先，不同名的字段则追加。
 *
 * @receiver 每个平台的BuildConfig字段组容器，将写入targetContainer
 * @param targetContainer 每个平台的BuildConfig字段组容器
 */
private fun BuildConfigFieldsBundleCollectionExtension.mergeTo(
    targetContainer: NamedDomainObjectContainer<BuildConfigFieldBundleBuilder>,
) {
    container.forEach { bundleOnTarget ->
        val merged = targetContainer.findByName(bundleOnTarget.name) ?: targetContainer.create(bundleOnTarget.name) {}
        bundleOnTarget.expect?.let { merged.expect = it }
        val overriddenNames = bundleOnTarget.fields.map { it.name }.toSet()
        merged.fields.removeIf { it.name in overriddenNames }
        merged.fields.addAll(bundleOnTarget.fields)
    }
}

/**
 * Organize build config fields bundle containers
 * 在多次调用mergeTo方法将BuildType、Flavor中每个平台的BuildConfig字段合并且添加到 [MergedVariantExtension.buildConfigFields]之后，
 * 我们需要再次处理合并后的容器，将所有标记了expect==false的BuildConfig字段Bundle中所有字段添加到 expect==true的BuildConfig字段Bundle中，
 *
 * @param container
 */
private fun organizeBuildConfigFieldsBundleContainers(container: NamedDomainObjectContainer<BuildConfigFieldBundleBuilder>) {
    val expectFalseBundles = container.filter { it.expect == false }
    expectFalseBundles.forEach { expectFalseBundle ->
        container.filter { it.expect == true }.forEach { expectTrueBundle ->
            expectFalseBundle.fields.forEach { field ->
                if (!expectTrueBundle.fields.contains(field)) {
                    expectTrueBundle.fields.add(field)
                }
            }
        }
    }
}

//</editor-fold>

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
                    expect = true
                    this + intField("AGE", 18)
                    this + booleanField("IS_DEBUG", false)
                    this + stringField("GENDER", null)
//                    this + field<IntRange>("AGE", 2..10)
                }
                create("jvmMain") {
                    expect = false
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
