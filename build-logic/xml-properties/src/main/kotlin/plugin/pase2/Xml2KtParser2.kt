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

package plugin.pase2

import java.io.*

/**
 * xml匹配规则：
 * 1. xml不存在match元素，则忽略此文件
 * 2. match元素指定了build-type和flavor，则按顺序匹配
 * 3. 如果第二点匹配失败，或者没有指定build-type和flavor，则使用default属性，如果default属性为false或者不存在，则不生成文件
 *
 * @property buildType BuildType名称
 * @property flavor Flavor名称
 * @property xml 要解析的xml文件
 * @property outDir 输出到此目录
 */
class Xml2KtParser2(
    private val buildType: String = "debug",//默认debug
    private val flavor: String? = null,
    val xml: File,
    val outDir: File,
) {

    fun generate() {

    }
}
