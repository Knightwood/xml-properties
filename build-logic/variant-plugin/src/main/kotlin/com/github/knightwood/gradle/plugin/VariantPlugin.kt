package com.github.knightwood.gradle.plugin

import com.github.knightwood.gradle.plugin.PluginConsts.IDEA_IMPORT_TASK_NAME
import com.github.knightwood.gradle.plugin.buildconfig_generator.BuildConfigGeneratorTask
import com.github.knightwood.gradle.plugin.extensions.MergedVariantExtension
import com.github.knightwood.gradle.plugin.extensions.VariantExtensions
import com.github.knightwood.gradle.plugin.extensions.toInput
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Variant plugin
 *
 * builtType和flavor中都能配置distribution、buildConfigs、CustomSourceSet，且flavor的配置会覆盖buildType的配置
 *
 * 用法
 * ``` build.gradle.kts
 * variant {
 *     activeVariant.set("dev-free")
 *     buildType {
 *         release {
 *             // configuration
 *         }
 *         create("dev") {
 *             distribution {
 *                 linux {
 *                     // configuration
 *                 }
 *                 macOS {
 *                     // configuration
 *                 }
 *             }
 *             buildConfigs {
 *                 packageName = "com.github.knightwood"
 *                 className = "BuildConfig"
 *
 *                 create("commonMain") {
 *                     isExpect = true
 *                     this + intField("AGE", 18)
 *                     this + booleanField("IS_DEBUG", false)
 *                     this + stringField("GENDER", null)
 *                     this + field<IntRange>("AGE", 2..10)
 *                 }
 *                 create("jvmMain") {
 *                     isExpect = false
 *                     this + stringField("NAME", "Knightwood")
 *                 }
 *             }
 *         }
 *     }
 *     flavor {
 *         this.create("free") {
 *             // configuration
 *             sourceSets {
 *                 create("commonMain") {
 *                     sourceDir = "buildSrc/free/commonMain/kotlin"
 *                 }
 *                 create("jvmMain") {
 *                     sourceDir = "buildSrc/free/jvmMain/kotlin"
 *                     resourceDir = "buildSrc/free/jvmMain/resources"
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * // 此插件收集到的distribution配置没法在此插件中直接设置给compose插件，只能手动在build.gradle.kts中配置
 * compose.desktop {
 *    application {
 *        val info = variant.parseActiveVariant().distribution
 *        nativeDistributions {
 *        packageVersion = info.parsePackageVersion()
 *        packageName = info.parsePackageName()
 *        vendor = info.parseVendor()
 *        windows {
 *           shortcut = true
 *           menuGroup = info.parseMenuGroupName()
 *           menu = true
 *        }
 *    }
 * }
 *
 * ```
 *
 * @constructor Creates a new VariantPlugin
 */
class VariantPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<VariantExtensions>("variant", VariantExtensions::class.java)
        target.afterEvaluate {
            val configProvider = target.provider { extension }.map { it.parseVariant() }
            plugins.withId(PluginConsts.KOTLIN_MPP_PLUGIN_ID) {
                val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
                println("apply kotlin multiplatform plugin")
                val kotlinExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
                val config = configProvider.get()
                applyCustomSourceSetToCurrentProject(config, kotlinExtension.sourceSets)
                generateBuildConfigForAllTargets(config, kotlinExtension.sourceSets)
            }

            plugins.withId(PluginConsts.KOTLIN_JVM_PLUGIN_ID) {
                val main = SourceSet.MAIN_SOURCE_SET_NAME
                println("apply kotlin jvm plugin")
                val kotlinExtension =
                    target.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
                val config = configProvider.get()
                applyCustomSourceSetToCurrentProject(config, kotlinExtension.sourceSets)
                generateBuildConfigForAllTargets(config, kotlinExtension.sourceSets)
            }
        }
    }

    /**
     * Creates a new build config for all targets
     *
     * 为所有目标创建BuildConfig.kt文件并追加到源集
     *
     * @param config
     * @param targets 所有目标KotlinSourceSet集合
     */
    fun Project.generateBuildConfigForAllTargets(
        config: MergedVariantExtension,
        targets: NamedDomainObjectCollection<KotlinSourceSet>,
    ) {
        val outDirPath = layout.buildDirectory.dir("generated/source/buildConfig")
        val generateTask = tasks.register<BuildConfigGeneratorTask>("GenerateBuildConfigTask") {
            buildConfigPackageName.set(config.buildConfigPackageName)
            buildConfigClassName.set(config.buildConfigClassName)
            allTargetBuildConfigFields.set(config.buildConfigFields.map { it.toInput() })
//            allTargetBuildConfigFields.set(config.buildConfigFields)
            outputFolder.set(outDirPath)
            targetSourceSetNames.set(targets.names)
        }
        // 配置阶段把生成目录注册为源集目录，避免在执行阶段修改构建模型；
        targets.configureEach {
            println("add generated buildconfig dir to sourceSet: ${this.name}")
            val sourceSet: KotlinSourceSet = this
            val sourceSetName = sourceSet.name
            /*
            我尝试通过 generateTask 这个 TaskProvider 让编译任务自动依赖生成本任务，
            通过获取generateTask.outputFolder追加到源集，失败了，报错说BuildConfigGeneratorTask任务还没执行完就预先获取了outputFolder
            sourceSet.kotlin.srcDir(
                   generateTask.flatMap { it.outputFolder }.map { it.dir("$sourceSetName/kotlin") }
            )
            */
            // 不再通过 generateTask 输出目录追加到源集，直接使用outDirPath避免报错
            sourceSet.kotlin.srcDir(
                outDirPath.map { it.dir("$sourceSetName/kotlin") }
            )
        }
        //只要触发 import操作，就让task执行
        // 抄自compose multiplatform 插件
        tasks.configureEach(object : Action<Task> {
            override fun execute(task: Task) {
                if (task.name == IDEA_IMPORT_TASK_NAME) {
                    task.dependsOn(tasks.withType(BuildConfigGeneratorTask::class.java))
                }
            }
        })
    }


    /**
     *  将所有自定义的sourceSet追加到当前项目kotlinSourceSet列表中。
     *
     *  @param config 用户自定义构建变体配置，包含面向不同构建平台的自定义sourceSet
     *  @param kotlinSourceSets 项目的kotlinSourceSet配置列表;
     *      kotlin jvm平台时可能只有main SourceSet
     *      kotlin multiplatform平台时可能有commonMain和jvmMain 等SourceSet
     */
    fun applyCustomSourceSetToCurrentProject(
        config: MergedVariantExtension,
        kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    ) {
        kotlinSourceSets.forEach {
            config.customSourceSets.findByName(it.name)?.let { customSourceSet ->
                customSourceSet.resourceDir?.let { it1 -> it.resources.srcDir(it1) }
                customSourceSet.sourceDir?.let { it1 -> it.kotlin.srcDir(it1) }
            }
        }
    }
}
