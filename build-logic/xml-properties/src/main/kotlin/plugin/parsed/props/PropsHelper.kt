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

package plugin.parsed.props

import org.gradle.api.Project
import java.util.Properties


object PropsHelper {
/*
    /**
     * ```
     * //            val properties = Properties()
     * //            properties.load(target.rootProject.file(props.file).reader())
     * //            properties.values.forEach {
     * //                println(it)
     * //            }
     * ```
     *
     * @param dimension
     * @param type
     */
    fun Project.build(dimension: VariantDimension, type: String) {
        rootProject.read("const_field.properties") {
            dimension.buildConfigField("String", "acra_user", readStr("acra_user", type))
            dimension.buildConfigField("String", "acra_pwd", readStr("acra_pwd", type))
            dimension.buildConfigField("String", "host", readStr("host", type))
        }
    }

    fun Properties.readStr(key: String, suffix: String): String {
        val props = getProperty("${key}_${suffix}", getProperty(key)!!)
        return "\"$props\""
    }

    fun Properties.read(key: String, suffix: String): String {
        return getProperty("${key}_${suffix}", getProperty(key)!!)
    }


*/
}

/**
 * TODO
 *
 * @param file
 * @param reader
 */
fun Project.read(file: String, relativePath: String? = null, reader: Properties.() -> Unit) {
    val properties = Properties()
    properties.load(rootProject.file(file).reader())
    properties.reader()
}