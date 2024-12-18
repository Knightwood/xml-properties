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

package plugin.parsed.xml.generagor

import com.android.utils.forEach
import com.squareup.kotlinpoet.FileSpec
import org.w3c.dom.Document
import org.w3c.dom.Node
import plugin.parsed.xml.processor.core.attrValue
import plugin.parsed.xml.processor.core.findElementNode
import plugin.parsed.xml.processor.core.fullMatch
import plugin.parsed.xml.processor.file.FileProcessor
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * xml匹配规则：
 * 1. xml不存在match元素，则忽略此文件
 * 2. match元素指定了build-type和flavor，则按顺序匹配
 * 3. 如果第二点匹配失败，或者没有指定build-type和flavor，则使用default属性，如果default属性为false或者不存在，则不生成文件
 *
 * @property pair <BuildType名称 , Flavor名称>
 * @property xmlFile 要解析的xml文件
 * @property outPutDir 输出到此目录
 */
class Xml2KotlinFileGenerator(
    private val buildType: String = "debug",//默认debug
    private val flavor: String? = null,
    private val xmlFile: File,
    private val outPutDir: File,
) {
    fun generate() {
        val dom: Document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xmlFile)

        //根元素：<file/>
        dom.firstChild.let { root ->
            //解析<file>的属性
            val fileName = root.attrValue("name")!!
            val packageName = root.attrValue("package")!!

            //xml不存在match元素，则直接return，忽略此文件
            val matcher = root.childNodes.findElementNode("match") ?: return

            var continueParse = false
            //获取文件中定义的匹配条件范围，使用“,”隔开
            //例如：<match build-type="release,prod" />
            val buildTypeMatchScope = matcher.attrValue("build-type")
            val flavorMatchScope = matcher.attrValue("flavor")

            //2024-12-17 14:13 fix- "debug1,releasenodaoijd" 这样的字符串，debug和release都会匹配成功的
            //匹配build-type
            continueParse = buildTypeMatchScope.fullMatch(buildType)
            //如果build-type匹配成功，flavor也存在，则继续匹配flavor
            if (continueParse && flavorMatchScope != null && flavor != null) {
                continueParse = flavorMatchScope.fullMatch(flavor)
            }
            //如果build-type和flavor都没有定义，则使用default属性判断是否生成文件
            if ((buildTypeMatchScope == null && flavorMatchScope == null)) {
                continueParse = matcher.attrValue("default")?.takeIf { it == "true" } != null
            }

            if (continueParse) {
                println(
                    "variant:$flavor,buildType:${buildType}\n" +
                            "开始解析文件：${xmlFile.absolutePath} \n" +
                            "输出位置: ${outPutDir.absolutePath} \n"
                )
                //生成kotlin文件
                val fileBlock = FileSpec.builder(packageName, fileName)
                    .apply {
                        //处理文件的其他内容
                        root.childNodes.forEach { node: Node ->
                            FileProcessor.processNode(node, this)
                        }
                    }
                    .build()
                fileBlock.writeTo(outPutDir)
            } else {
                println("文件${xmlFile.absolutePath} 匹配失败，不生成文件")
                /*
                这里删除文件的逻辑是错误的。
                外面传入的输出位置是当前构建变体的输出位置，比如当前构建变体是master/debug，
                而解析release.xml文件，他定义自己的build-type为release，flavor为sen，
                那么它的输出位置应该是sen/release,但是这里删除却是用的master/release，导致错误删除文件。
                 */

//                val tmp = packageName.replace(".", File.separator)
//                val file =
//                    "${outPutDir.absolutePath}${File.separator}$tmp${File.separator}$fileName.kt"
//                println("删除旧文件：$file")
//                val oldFile = File(file)
//                if (oldFile.exists()) oldFile.delete()
//                Unit
            }
        }
    }
}
