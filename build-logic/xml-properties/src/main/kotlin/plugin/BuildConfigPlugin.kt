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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.registering
import java.io.File

/**
 * 可以在kmp项目中使用的，类似于android buildConfig的插件
 */
class BuildConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        target.extensions.create(
            BuildConfig::class.java,
            "KmpBuildConfig",
            BuildConfigImpl::class.java,
        )

        target.extensions.create(
            AppProperties::class.java,
            "xmlProps",
            AppPropertiesImpl::class.java,
        )
        target.run {
            val buildConfigTask = tasks.register("kmp-buildConfig-task", CodeGenTask::class.java,){
                val config = target.property("KmpBuildConfig") as BuildConfig
                val xml = target.property("xmlProps") as AppProperties
                //                println("flavor:${variant.flavorName} -- buildType:${variant.buildType}")

                //flavor、buildType、outputDir
                flavorName.set(config.buildType.flavor)
                buildTypeName.set(config.buildType.name)
                if (xml.outDir!=null){
                    outputFolder.set(File(xml.outDir!!))
                }else{
                    outputFolder.set(project.layout.buildDirectory.asFile.get())
                }

                //input xml files
                val listProperty = target.objects.listProperty(File::class.java)
                val xmls = xml.files(target.rootProject)
                listProperty.set(xmls)
                inputXmlFile.set(listProperty)
            }
        }
    }

}