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

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.w3c.dom.Node
import plugin.parsed.xml.processor.core.FieldParser
import plugin.parsed.xml.processor.core.filterAllElementNode
import plugin.parsed.xml.processor.core.findElementNode
import plugin.parsed.xml.processor.core.parseModifier

abstract class BaseCls {
    /**
     * 判断是否添加默认的public修饰符
     *
     * <class name="HttpPath"> </class>
     *
     * @param node class节点
     * @param builder
     */
    fun fixModifier(node: Node, builder: TypeSpec.Builder) {
        //找到修饰符
        node.childNodes.findElementNode("modifiers")?.let {
            //读取修饰符列表
            val modifiers = it.childNodes.filterAllElementNode().map { it.nodeName }
            //如果存在自定义的修饰符，就不添加默认的public修饰符
            val tmp = modifiers.filter {
                it == "public" || it == "private" || it == "protected" || it == "internal"
            }
            if (tmp.isEmpty())
                builder.addModifiers(KModifier.PUBLIC)
        }
    }

    /**
     * 添加自定义修饰符
     *
     * <class name="HttpPath"> </class>
     *
     * @param node class节点
     * @param builder
     */
    fun processModifiers(node: Node, builder: TypeSpec.Builder) {
        val modifiers = node.childNodes.filterAllElementNode().map(::parseModifier)
        builder.addModifiers(modifiers)
    }

    /**
     * 添加类里的字段
     *
     * <class name="HttpPath"> </class>
     *
     * @param node class节点
     * @param builder
     */
    fun processField(node: Node, builder: TypeSpec.Builder) {
        FieldParser.buildType(node, builder)
//        FieldModel.printNode(node)
    }

    /**
     * 添加类里的方法
     *
     * <class name="HttpPath"> </class>
     *
     * @param node class节点
     * @param builder
     */
    fun processFunction(node: Node, builder: TypeSpec.Builder) {

    }

    /**
     * 添加自定义构造函数
     *
     * <class name="HttpPath"> </class>
     *
     * @param node class节点
     * @param builder
     */
    fun processConstructor(node: Node, builder: TypeSpec.Builder) {

    }
}