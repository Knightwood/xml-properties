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

package plugin.parsed.xml.processor.file

import com.squareup.kotlinpoet.FileSpec
import org.w3c.dom.Node
import plugin.parsed.xml.processor.core.IXmlProcessor
import plugin.parsed.xml.processor.core.attrValue
import plugin.parsed.xml.processor.core.filterAllElementNode


object ImportProcessor : IXmlProcessor {

    override fun processNode(node: Node, builder: FileSpec.Builder) {
        val packageName = node.attributes.getNamedItem("package")
        val className = node.attributes.getNamedItem("name")
        if (className == null) {
            /*
            *多个引入
                <import package="">
                        <import name="List" />
                        <import name="Set" />
                </import>
            */
            val array: List<String> = node.childNodes
                .filterAllElementNode()
                .mapNotNull {
                    it.attrValue("name")
                }
            builder.addImport(packageName.nodeValue, array)
        } else {
            //单个的引入
            // <import name="" package="" />
            builder.addImport(packageName.nodeValue, className.nodeValue)
        }

    }

}