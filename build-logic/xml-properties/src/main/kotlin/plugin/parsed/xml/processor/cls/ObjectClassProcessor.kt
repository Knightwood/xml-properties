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

package plugin.parsed.xml.processor.cls

import com.android.utils.forEach
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.w3c.dom.Node
import plugin.parsed.xml.processor.core.IXmlProcessor

object ObjectClassProcessor : BaseCls(), IXmlProcessor {
    override fun processNode(node: Node, builder: FileSpec.Builder) {
        //class name
        val name = node.attributes.getNamedItem("name").nodeValue
        builder.addType(TypeSpec.objectBuilder(name)
            .apply {
                addModifiers(KModifier.PUBLIC)
                //处理字段
                node.childNodes.forEach {
                    when (it.nodeName) {
                        "modifiers"-> processModifiers(it, this)
                        "val", "var" -> processField(it, this)
                        "fun" -> processFunction(it,this)
                    }
                }
            }
            .build()
        )
    }


}