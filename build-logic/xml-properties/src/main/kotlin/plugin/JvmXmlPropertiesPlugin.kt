/*
 * Copyright [2023-2024] [KnightWood]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

/**
 * 可以在kmp项目中使用的，类似于android buildConfig的插件
 */
class JvmXmlPropertiesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.run {
            val resourcesExtension = target.extensions.create(
                AppProperties::class.java,
                "xmlProps",
                AppPropertiesImpl::class.java,
            )

            // 将输出目录设置为源集
            // 抄自 compose multiplatform 插件
            val config = target.provider { resourcesExtension }
            plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
                println("apply kotlin multiplatform plugin,输出文件")
                val kotlinExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
                val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
                //将源码放到 "build/generateKtCode/" 目录并追加到源集
                appendSourceSet(kotlinExtension, commonMain, config, target)
            }

            plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
                println("apply kotlin jvm plugin,输出文件")
                val kotlinExtension =
                    target.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
                val main = SourceSet.MAIN_SOURCE_SET_NAME
                appendSourceSet(kotlinExtension, main, config, target)
            }

            val buildConfigTask = tasks.register("generateKtCodeFromXml", CodeGenTask::class.java) {
                val xml = target.property("xmlProps") as AppProperties
                val config = xml.buildType
//                println("flavor:${variant.flavorName} -- buildType:${variant.buildType}")

                //flavor、buildType、outputDir
                flavorName.set(config.flavor)
                buildTypeName.set(config.name)
                outputFolder.set(project.layout.buildDirectory.dir(xml.outDir).get())
                // 声明输出目录
                outputs.dir(outputFolder)

                //input xml files
                val listProperty = target.objects.listProperty(File::class.java)
                val xmls = xml.files(target.rootProject)
                listProperty.set(xmls)
                inputXmlFile.set(listProperty)
            }


            //只要触发 import操作，就让task执行
            // 抄自compose multiplatform 插件
            target.tasks.configureEach(object : Action<Task> {
                override fun execute(task: Task) {
                    if (task.name == IDEA_IMPORT_TASK_NAME) {
                        task.dependsOn(target.tasks.withType(CodeGenTask::class.java))
                    }
                }
            })
        }
    }
}

private fun appendSourceSet(
    kotlinExtension: KotlinProjectExtension,
    sourceSetName: String,
    config: Provider<AppProperties>,
    target: Project,
) {
    kotlinExtension.sourceSets.all(object : Action<KotlinSourceSet> {
        override fun execute(sourceSet: KotlinSourceSet) {
            if (sourceSet.name == sourceSetName) {
                sourceSet.kotlin.srcDir(config.map { appProperties ->
                    target.layout.buildDirectory.dir(
                        appProperties.outDir
                    ).get()
                })
            }
        }
    })
}

internal const val IDEA_IMPORT_TASK_NAME = "prepareKotlinIdeaImport"
internal const val KOTLIN_MPP_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
internal const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"
internal const val KOTLIN_ANDROID_PLUGIN_ID = "org.jetbrains.kotlin.android"
internal const val KOTLIN_JS_PLUGIN_ID = "org.jetbrains.kotlin.js"
internal const val COMPOSE_PLUGIN_ID = "org.jetbrains.compose"
internal const val BASE_DIR = "generateKtCode"
