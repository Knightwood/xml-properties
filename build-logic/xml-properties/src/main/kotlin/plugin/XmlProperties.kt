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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.scope.ProjectInfo.Companion.getBaseName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import plugin.parsed.xml.generagor.Xml2KotlinFileGenerator
import java.io.File
import java.io.Serializable
import java.util.regex.Pattern

fun main() {
    val suffix = listOf("Debug", "Release")
        .joinToString("|") { Pattern.quote(it) }
    val prefix = ":arch:ui:theme"
//    val prefix = ":app"
//    val prefix = ":arch:ui:picture-selector"
    val s =
        "[DefaultTaskExecutionRequest{args=[:app:assembleMasterDebug, :app:assembleMasterDebugUnitTest, :app:assembleMasterDebugAndroidTest, :arch:ui:theme:assembleDebug, :arch:ui:theme:assembleDebugUnitTest, :arch:ui:theme:assembleDebugAndroidTest, :arch:ui:picture-selector:assembleDebug, :arch:ui:picture-selector:assembleDebugUnitTest, :arch:ui:picture-selector:assembleDebugAndroidTest],projectPath='null',rootDir='null'}]"
    val pattern = Pattern.compile("$prefix:assemble(\\w+)?($suffix)", Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(s)
    println(matcher.find())  // 应该输出 true
    println(matcher.group())  // 输出匹配的部分
    println(matcher.group(1))  // 输出匹配的部分
    println(matcher.group(2))  // 输出匹配的部分
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalFeature

/**
 * xml解析插件配置
 */
interface AppProperties : Serializable {
    /**
     * 是否启用插件
     */
    var enabled: Boolean

    /**
     * 哪些xml文件需要排除，填写名称即可，比如: example.xml
     */
    var excludeFile: ArrayList<String>

    /**
     * 要解析哪个文件夹下的xml文件
     *
     * 这是个相对与项目根目录的路径。
     *
     * 若指定为resources，即project/resources/
     *
     * 若指定为apps/abc，即project/apps/abc/
     */
    var dir: String

    /**
     * 在安卓中使用时可以为null，会默认输出到generate目录。
     *
     * 在kmp项目中使用时，需要自行指定输出位置并将其追加到页码目录。
     * 比如
     * ```
     *
     * //指定源码的生成路径
     * val myCodeGenOutDir = "build/generated/mycode"
     * xmlProps {
     *     dir = "apps/composeApp2/resources/buildconfig" // 配置文件所在目录
     *     outDir = myCodeGenOutDir
     * }
     *
     *  // kmp项目
     * kotlin {
     *   sourceSets {
     *      commonMain.configure {
     *          kotlin.srcDir(myCodeGenOutDir) // 添加生成的代码为源码
     *      }
     *   }
     * }
     *
     * // 普通kotlin项目
     * sourceSets {
     *     main {
     *         kotlin {
     *             srcDirs(myCodeGenOutDir)
     *         }
     *     }
     * }
     * ```
     */
    var outDir: String?

    /**
     * 如果无法解析当前任务，则使用此BuildType和Flavor。
     */
    var defaultBV: VariantBean

    /**
     * 获取所有可被解析的xml文件
     *
     * @param rootProject
     * @return
     */
    fun files(rootProject: Project): List<File>

    /**
     * 构建生成代码的放置路径
     *
     * @return
     */
    @Deprecated(message = "这个会生成在BuildConfig下的目录，请使用targetDir(rootProject: Project,parentDir: String, pair: BV)")
    fun targetDir(project: Project, pair: VariantBean): String
    fun targetDir(parentDir: String, pair: VariantBean): File
}

/**
 * xml解析插件配置接口实现类
 *
 * @property dir
 * @property enabled
 * @property excludeFile
 * @property defaultBV
 */
open class AppPropertiesImpl(
    override var dir: String = "resources",
    override var enabled: Boolean = true,
    override var excludeFile: ArrayList<String> = ArrayList(),
    override var defaultBV: VariantBean = VariantBean(null, null),
) : AppProperties, Serializable {

    override var outDir: String? = null

    /**
     * 列出所有xml合法的文件
     *
     * @param rootProject 注意，是rootProject，不是project
     * @return
     */
    override fun files(rootProject: Project): List<File> {
        if (!enabled) {
            println("empty")
            return emptyList()
        } else {
            val xmlDir = rootProject.file(dir)
            //对resources目录下的所有xml文件解析
            return if (xmlDir.exists() && xmlDir.isDirectory) {
                xmlDir.listFiles()?.filter {
                    //可以被解析的文件需要以xml结尾，且不在排除列表中
                    it.name.endsWith(".xml") && !excludeFile.contains(it.name)
                } ?: emptyList()
            } else {
                println("not dir")
                emptyList()
            }
        }
    }

    @Deprecated("xmlProperties用不到这些方法了")
    override fun targetDir(parentDir: String, pair: VariantBean): File {
        val path = buildString {
            //把生成的文件放入kotlin下
            append(parentDir)
            if (pair.flavor != null) {
                append(File.separator)
                append(pair.flavor)
            }

            append(File.separator)
            append(pair.buildType)
        }
        return File(path).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    @Deprecated("这个会生成在BuildConfig下的目录，请使用targetDir(rootProject: Project,parentDir: String, pair: BV)")
    override fun targetDir(project: Project, pair: VariantBean): String {
        //得到app构建后的generated目录
        val sourcesDir = project.layout.buildDirectory.dir("generated")
        val path = buildString {
            //把生成的文件放入buildConfig下
            append(sourcesDir.get().asFile.absolutePath)
            append(File.separator)
            append("source")
            append(File.separator)
            append("buildConfig")

            if (pair.flavor != null) {
                append(File.separator)
                append(pair.flavor)
            }

            append(File.separator)
            append(pair.buildType)
        }
        File(path).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        return path
    }

    override fun toString(): String {
        return "dir: $dir, enabled: $enabled, excludeFile: ${excludeFile.joinToString()}"
    }
}

/**
 * BUILD_TYPE 和 FLAVOR
 *
 * @property buildType
 * @property flavor
 */
open class VariantBean(
    val buildType: String = "debug",//默认debug
    val flavor: String? = null,
) : Serializable {

    companion object {
        /**
         * 直接构建生成BV实例
         *
         * @param buildType
         * @param flavor
         * @return
         */
        operator fun invoke(
            buildType: String?,//默认debug
            flavor: String?,
        ): VariantBean {
            return VariantBean(buildType ?: "debug", flavor)
        }

        /**
         * 从Gradle任务请求中解析出当前的构建类型和风味名称，生成BV实例
         *
         * @param projectDisplayName 当前project显示名称，用于日志和输出
         * @param gradle Gradle实例，用于访问构建配置和参数
         * @param buildTypeArray 在当前project的gradle中配置的BuildType列表，用于匹配和解析当前构建类型
         * @return 返回一个包含构建类型和风味名称的实例，如果无法解析则返回null
         */
        internal fun parseCurrentBuildTypeAndFlavor(
            projectDisplayName: String,
            gradle: Gradle,
            buildTypeArray: List<String>
        ): VariantBean? {
            //通过cutDisplayName获取到当前project的名称，比如得到 ":app" 或是 ":arch:ui:theme"
            //projectDisplayName 都是 project ':arch:ui:theme' 格式，需要将‘’中的内容截取出来
            val suffix = cutDisplayName(projectDisplayName) ?: ""
            //gradle当前执行任务的名称
            val taskRequestsStr = gradle.startParameter.taskRequests.toString()
            //"[DefaultTaskExecutionRequest{args=[:app:assembleMasterDebug, :app:assembleMasterDebugUnitTest, :app:assembleMasterDebugAndroidTest, :arch:ui:theme:assembleDebug, :arch:ui:theme:assembleDebugUnitTest, :arch:ui:theme:assembleDebugAndroidTest, :arch:ui:picture-selector:assembleDebug, :arch:ui:picture-selector:assembleDebugUnitTest, :arch:ui:picture-selector:assembleDebugAndroidTest],projectPath='null',rootDir='null'}]"
            //比如当前project是":arch:ui:theme"那么就需要匹配到:arch:ui:theme:assembleMasterDebug
            //且将Master和Debug截取出来
            println(taskRequestsStr)
            // 使用正则表达式将当前project的buildTYPEHE FLAVOR解析出来
            val buildTypePattern = buildTypeArray.joinToString("|") { Pattern.quote(it) }
            val pattern: Pattern = if (taskRequestsStr.contains("assemble")) {
                //匹配buildTypeArray中的字符串，而且忽略大小写
                Pattern.compile(
                    "$suffix:assemble(\\w+)?($buildTypePattern)",
                    Pattern.CASE_INSENSITIVE
                )
            } else {
                Pattern.compile(
                    "$suffix:bundle(\\w+)?($buildTypePattern)",
                    Pattern.CASE_INSENSITIVE
                )
            }

            // 使用正则表达式进行匹配
            val matcher = pattern.matcher(taskRequestsStr)
            if (matcher.find()) {
                // 提取匹配的构建类型和口味名称，并转换为小写
                val variantName = matcher.group(1)?.lowercase()
                val buildTypeName = matcher.group(2)?.lowercase()
                println("Variant: $variantName")
                println("Build Type: $buildTypeName")
                // 返回构建类型和口味名称的Pair
                return VariantBean(buildTypeName, variantName)
            } else {
                // 如果没有找到匹配项，输出提示信息并返回null
                println("No match found")
                return null
            }
        }

        /**
         * 从任务的显示名称中截取出来项目的真实名称
         *
         * @param input project ':arch:ui:theme'··theme··extension 'base' property
         *    'archivesName'
         * @return :arch:ui:theme
         */
        private fun cutDisplayName(input: String): String? {
            val pattern = Pattern.compile("'([^']+)'")
            val matcher = pattern.matcher(input)

            if (matcher.find()) {
                val extractedContent = matcher.group(1)
                return extractedContent
            } else {
                println("No match found")
                return null
            }
        }

    }
}

/**
 * 在build.gradle文件中配置内容
 *
 * ```
 * xmlConfigs {
 *     enabled = true
 *     excludeFile += "test1.xml"
 * }
 * ```
 */
@Deprecated("use XmlProperties instead")
class AppPropertiesPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        target.pluginManager.apply("org.gradle.idea")

        target.extensions.create(
            AppProperties::class.java,
            "xmlConfigs",
            AppPropertiesImpl::class.java,
        )
        firstTest(target)
    }
}

/**
 * 判断是library module还是app module
 */
fun Project.parseLibraryOrApp2GetExtension(block: CommonExtension<*, *, *, *, *, *>.(isLibrary: Boolean) -> Unit) {
    val isLibrary: Boolean = pluginManager.hasPlugin("com.android.library")
    val extension = if (isLibrary) {
        extensions.getByType<LibraryExtension>()
    } else {
        extensions.getByType<ApplicationExtension>()
    }
    extension.block(isLibrary)
}


private fun firstTest(target: Project) {
    val outputDir: File =
        target.layout.buildDirectory.dir("generated/source/kotlin").get().asFile
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    target.afterEvaluate {
        val props = target.property("xmlConfigs") as AppProperties
        if (props.enabled) {
            println("afterEvaluate-准备生成代码：$props")
            target.parseLibraryOrApp2GetExtension { isLibrary ->
                //解析当前module信息
                println(target.displayName + "··" + target.name + "··" + target.getBaseName())

                //解析 buildType和 flavor
                val pair = VariantBean.parseCurrentBuildTypeAndFlavor(
                    target.displayName,
                    target.gradle,
                    buildTypes.map { it.name }) ?: props.defaultBV

                //在基础目录下，根据buildType和flavor生成代码目录
                val targetOutPutDir = props.targetDir(outputDir.absolutePath, pair)
                if (target.plugins.hasPlugin("idea")) {
                    //这个好像是能解决显示问题，但还是需要下面把源码添加到main的sourceSet
                    println("has idea plugin")
                    target.configure<IdeaModel> {
                        module {
                            sourceDirs = sourceDirs + targetOutPutDir
                            generatedSourceDirs = generatedSourceDirs + targetOutPutDir
                        }
                    }
                }

                //将本次的代码目录追加为源码目录
                target.extensions.findByType(AppExtension::class.java)?.let {
                    it.sourceSets.getByName("main") {
//                            kotlin.srcDirs(targetOutPutDir)
                        java.srcDirs(targetOutPutDir)
                    }
                }
                val canParsedXmlFiles = props.files(target.rootProject)
                canParsedXmlFiles.forEach { xmlFile ->
                    //生成代码文件
                    Xml2KotlinFileGenerator(
                        pair.buildType,
                        pair.flavor,
                        xmlFile = xmlFile,
                        outPutDir = targetOutPutDir,
                    ).generate()
                }
            }
        } else {
            println("AppPropertiesPlugin被禁用")
        }
    }
}