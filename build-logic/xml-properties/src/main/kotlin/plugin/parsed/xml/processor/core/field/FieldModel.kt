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

package plugin.parsed.xml.processor.core.field

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.w3c.dom.Node
import plugin.parsed.xml.processor.core.attrValue
import plugin.parsed.xml.processor.core.filterAllElementNode
import plugin.parsed.xml.processor.core.findElementNode
import plugin.parsed.xml.processor.core.childsHasElementNode
import plugin.parsed.xml.processor.core.findElementChild
import plugin.parsed.xml.processor.core.parseModifier

/**
 * 字段
 *
 * @property data
 */
open class FieldModel(
    val name: String,//字段名
    val data: String? = null,//字段的默认值
    val mutablable: Boolean = false
) {
    open var modifier = mutableListOf<KModifier>(KModifier.PUBLIC)

    /**
     * 字段类型，无泛型的内置类型用不到此字段，直接使用[cls]属性即可
     */
    var clsType: TypeModel? = null

    override fun toString(): String {
        val sb = buildString {
            append(modifier.joinToString())
            append(" ")
            if (mutablable)
                append("var")
            else
                append("val")
            append(" ")
            append(name)
            append(": ")
            append(clsType.toString())
            if (data != null)
                append(" = $data")
        }
        return sb
    }

    companion object {
        //<editor-fold desc="kotlin poet">

        /**
         * 根据fieldModel生成PropertySpec
         *
         * @param fieldModel
         * @return
         */
        private fun buildPropertySpec(fieldModel: FieldModel): PropertySpec {
            val field = PropertySpec.builder(
                fieldModel.name,
                fieldModel.clsType!!.buildParameterizedType(),
                fieldModel.modifier
            )
                .apply {
                    mutable(fieldModel.mutablable)
                    if (fieldModel.data != null)
                        initializer(fieldModel.data)
                }
                .build()
            return field
        }

        fun buildType(node: Node, pbuilder: FileSpec.Builder) {
            val fieldModel = parse(node)
            pbuilder.addProperty(buildPropertySpec(fieldModel))
        }

        fun buildType(node: Node, pbuilder: TypeSpec.Builder) {
            val fieldModel = parse(node)
            pbuilder.addProperty(buildPropertySpec(fieldModel))
        }

        //</editor-fold>

        fun printNode(node: Node) {
            val model = parse(node)
            println(model)
        }

        //<editor-fold desc="节点解析">

        /**
         * 解析 val或者var节点，生成字段描述
         *
         * @param node
         * @return
         */
        private fun parse(node: Node): FieldModel {
            val fieldName = node.attrValue("name")!!
            val modifier = parseModifier(node.attrValue("modifier"))

            val typeName = node.attrValue("type") ?: "String"
            val typePackage = node.attrValue("package")

            //确定有几个child
            val childCount = node.childNodes.filterAllElementNode()
            //最终可以解析出来的类型
            var type: TypeModel? = null
            //字段默认值
            var data: String? = null
            if (childCount.size == 0) {
                //说明只有代码块
                data = node.firstChild.nodeValue
                type = TypeModel.parse(typeName, typePackage)
            } else {
                //说明有泛型和代码块
                data = node.findElementChild("code")?.firstChild?.nodeValue
                //解析泛型
                val genericsNode = node.findElementChild("generics")
                if (genericsNode != null && genericsNode.childsHasElementNode()) {
                    type = TypeModel.parse(typeName, typePackage, genericsNode)
                } else {
                    //忽略泛型
                    type = TypeModel.parse(typeName, typePackage)
                }
            }

            val nullable = node.attrValue("nullable")?.toBoolean() ?: false
            type.nullable = nullable

            return FieldModel(fieldName, data).also {
                it.clsType = type
                it.modifier.clear()
                it.modifier.addAll(modifier)
            }
        }
        //</editor-fold>

    }
}