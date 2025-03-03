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

import java.io.Serializable

interface BuildConfig : Serializable {
    /**
     * 是否启用该插件，默认启用
     */
    var enable: Boolean

    /**
     * [BuildType.release]、[BuildType.debug]、等
     */
    var buildType: BuildType
}

open class BuildConfigImpl : BuildConfig, Serializable {
    override var enable: Boolean = true
    override var buildType: BuildType = BuildType.debug
}

/**
 * 描述当前的构建类型
 *
 * @property isDebug
 * @property name buildType名称
 * @property flavor flavor名称，在非安卓项目中无意义，仅用于xml文件匹配，可以为null。
 */
data class BuildType(
    val isDebug: Boolean,
    val name: String,
    val flavor: String? = null,
) {
    companion object {
        val release = BuildType(false, "release")
        val debug = BuildType(true, "debug")
    }
}