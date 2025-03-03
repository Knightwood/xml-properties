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

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.register
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.work.Incremental
import plugin.parsed.xml.generagor.Xml2KotlinFileGenerator
import java.io.File

/**
 * 在build.gradle文件中配置内容
 *
 * ```
 * xmlProps {
 *     enabled = true
 *     excludeFile += "test1.xml"
 * }
 * ```
 */
class XmlPropertiesPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        target.extensions.create(
            AppProperties::class.java,
            "xmlProps",
            AppPropertiesImpl::class.java,
        )

        val isLibrary: Boolean = target.pluginManager.hasPlugin("com.android.library")
        if (!isLibrary) {
            // Registers a callback on the application of the Android Application plugin.
            // This allows the CustomPlugin to work whether it's applied before or after
            // the Android Application plugin.
            target.plugins.withType(AppPlugin::class.java) {

                // Queries for the extension set by the Android Application plugin.
                // This is the second of two entry points into the Android Gradle plugin
                val androidComponents =
                    target.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
                //对每一个变体都注册task，task名称不可重复，具体哪一个会被执行，gradle会自行决定
                // Registers a callback to be called, when a new variant is configured
                androidComponents.onVariants { variant ->
                    createTask(target, variant)
                }
            }
        } else {
            target.plugins.withType(LibraryPlugin::class.java) {
                val libraryComponents =
                    target.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
                libraryComponents.onVariants { variant ->
                    createTask(target, variant)
                }
            }
        }
    }

    /**
     * 创建核心task
     *
     * @param target
     * @param variant
     */
    private fun createTask(target: Project, variant: Variant) {
        val codeGenTaskTaskProvider =
            target.tasks.register<CodeGenTask>(variant.name.capitalized()) {
                val xmlProps = target.property("xmlProps") as AppProperties
//                println("flavor:${variant.flavorName} -- buildType:${variant.buildType}")

                //flavor、buildType、outputDir
                flavorName.set(variant.flavorName)
                buildTypeName.set(variant.buildType)
//                val targetDir = File(project.layout.buildDirectory.asFile.get(), "source/io")
                outputFolder.set(project.layout.buildDirectory.asFile.get())

                //input xml files
                val listProperty = target.objects.listProperty(File::class.java)
                val xmls = xmlProps.files(target.rootProject)
                listProperty.set(xmls)
                inputXmlFile.set(listProperty)
            }

        //通过getByName的方式创建一个sources文件夹，使用“/”可以创建多级目录
        variant.sources.java?.let {
            it.addGeneratedSourceDirectory(
                codeGenTaskTaskProvider,
                CodeGenTask::outputFolder
            )
        }
        //这个不生效，我不知道为什么
//        variant.sources .getByName("source/io").let {
//            //追加到源码目录
//            it.addGeneratedSourceDirectory(
//                codeGenTaskTaskProvider,
//                CodeGenTask::outputFolder
//            )
//        }
        //使用上面的"variant.sources.java"部分代码，这里不必要调用
        // 指定执行时机，在变体的preBuild时执行
//        variant.lifecycleTasks.registerPreBuild(codeGenTaskTaskProvider)
    }
}

/**
 * 生成文件task
 */
abstract class CodeGenTask : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val flavorName: Property<String?>

    @get:Input
    abstract val buildTypeName: Property<String>

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    /**
     * 输入的xml文件，InputFiles需要使用 ListProperty
     *
     * ```
     * val listProperty = target.objects.listProperty(File::class.java)
     * val xmls = xmlProps.files(target.rootProject)
     * listProperty.set(xmls)
     * inputXmlFile.set(listProperty)
     * ```
     *
     * 使用Incremental追踪文件变化
     */
    @get:InputFiles
    @get:Incremental
    abstract val inputXmlFile: ListProperty<File>

    @TaskAction
    fun doTaskAction() {
        val outputFile: File = outputFolder.asFile.get()
        outputFile.mkdirs()
        inputXmlFile.get().forEach { xmlFile ->
            //生成代码文件
            Xml2KotlinFileGenerator(
                buildType = buildTypeName.get(),
                flavor = flavorName.orNull,
                xmlFile = xmlFile,
                outPutDir = outputFile,
            ).generate()
        }
    }
}