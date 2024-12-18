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
import plugin.parsed.xml.processor.cls.ClassProcessor
import plugin.parsed.xml.processor.cls.ConstructorProcessor
import plugin.parsed.xml.processor.cls.ObjectClassProcessor
import plugin.parsed.xml.processor.cls.FunProcessor
import plugin.parsed.xml.processor.core.FieldParser
import plugin.parsed.xml.processor.core.IXmlProcessor

/**
 * file节点解析
 */
object FileProcessor : IXmlProcessor {
    override fun processNode(node: Node, builder: FileSpec.Builder) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            when (node.nodeName) {
                "import" -> {
                    ImportProcessor.processNode(node, builder)
                }

                "val", "var" -> {
                    FieldParser.buildType(node, builder)
//                    FieldModel.printNode(node)
                }

                "fun" -> {
                    FunProcessor.processNode(node, builder)
                }

                "constructor" -> {
                    ConstructorProcessor.processNode(node, builder)
                }

                "class" -> {
                    ClassProcessor.processNode(node, builder)
                }

                "object-class" -> {
                    ObjectClassProcessor.processNode(node, builder)
                }
            }
        }
    }
}
