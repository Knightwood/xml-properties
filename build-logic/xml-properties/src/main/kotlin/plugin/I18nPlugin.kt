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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties
import javax.inject.Inject

class I18nPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create(
            I18nConfig::class.java,
            "i18n",
            I18nConfigImpl::class.java,
        )
        target.run {
            tasks.register("i18n-task", I18nGenerateTask::class.java) {
                val config = target.property("i18n") as I18nConfig
                packageName.set(config.targetPackageName)
                fileName.set(config.targetFileName)
                outputFolder.set(file(config.outDir))
                inputFile.set(file("${config.inDir}/string.properties"))
                //测试
//                outputFolder.set(file("build/generated"))
//                inputFile.set(file("src/main/resources/i18n/string.properties"))
            }
        }
    }
}

abstract class I18nGenerateTask @Inject constructor(
    project: Project,
) : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val fileName: Property<String>

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    @get:InputFile
    abstract val inputFile: Property<File>


    @TaskAction
    fun run() {
        val properties = Properties()
        inputFile.get().inputStream().use { inputStream ->
            properties.load(inputStream)
        }
        val outputFile: File = outputFolder.asFile.get()
        outputFile.mkdirs()
        generateClass(packageName.get(), fileName.get(), outputFile, properties)
    }

    fun generateClass(
        packageName: String, fileName: String, outPutDir: File,
        properties: Properties
    ) {
        //生成kotlin文件
        val fileBlock = FileSpec.builder(packageName, fileName)
            .apply {
                addType(
                    TypeSpec.objectBuilder(fileName).apply {
                        addModifiers(KModifier.PUBLIC)
                        for (name1 in properties.propertyNames()) {
                            addProperty(buildPropertySpec(name1.toString(), "\"${name1}\""))
                        }
                    }.build()
                )
            }
            .build()
        fileBlock.writeTo(outPutDir)
    }

    private fun buildPropertySpec(name: String, value: String): PropertySpec {
        val field = PropertySpec.builder(name, String::class, KModifier.PUBLIC)
            .apply {
                mutable(false)
                initializer(value)
            }
            .build()
        return field
    }

}