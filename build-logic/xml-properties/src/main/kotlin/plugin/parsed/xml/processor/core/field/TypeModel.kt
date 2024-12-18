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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.w3c.dom.Node
import plugin.parsed.xml.processor.core.attrValue
import plugin.parsed.xml.processor.core.filterAllElementNode
import plugin.parsed.xml.processor.core.childsHasElementNode
import plugin.parsed.xml.processor.core.parseClsInner
import plugin.parsed.xml.processor.core.parseModifier
import kotlin.reflect.KClass

/**
 * 类型
 *
 * @param typeName
 * @param typePackage
 */
open class TypeModel(
    val typeName: String,//字段名
    val typePackage: String,//包名
) {
    var nullable: Boolean = false

    /**
     * 泛型也是类型，用于描述泛型的修饰符，比如in、out等，默认为空
     */
    open var modifier: KModifier? = null
    var generics: MutableList<TypeModel> = mutableListOf()


    private fun getTypeName(): ClassName {
        return ClassName(typePackage, typeName)
    }

    /**
     * 构建类型,最终可以构建出来不带泛型和带泛型的类型
     * - String
     * - List<String>
     * - List<in String>
     * - List<out String>
     * - Map<String,List<String>>
     *
     *
     * |分类       |生成的类型           |JavaPoet 写法|也可以这么写 （等效的 Java 写法）|
     * |---------|----------------|---------------------------------------------------     |--------------------|
     * |内置类型     |int             |TypeName.INT                                         |int.class           |
     * |数组类型     |int[]           |ArrayTypeName.of(int.class)                          |int[].class         |
     * |需要引入包名的类型|java.io.File    |ClassName.get(“java.io”, “File”)                  |java.io.File.class  |
     * |参数化类型    |List            |ParameterizedTypeName.get(List.class, String.class) |                    |
     * |类型变量     |T               |TypeVariableName.get(“T”)                            |                    |
     * |通配符类型    |? extends String|WildcardTypeName.subtypeOf(String.class)            |                    |
     *
     *
     * 这些类型之间可以相互嵌套， 比如 `ParameterizedTypeName.get(List.class, String.class)` 其中 `List.class` 等价于 `ClassName.get("java.util", "List")`。
     *
     * 因此，`ParameterizedTypeName.get(List.class, String.class)`
     *
     * 可以写为`ParameterizedTypeName.get(ClassName.get("java.util", "List")`, `ClassName.get("java.lang", "String"))`。
     *
     * 前者的好处是简洁， 后者的好处是 “使用 ClassName 代表某个类型而无需引入该类型“。
     * 比如： 由于在 java 工程中是没有 android 的 sdk，
     * 所以你在 java 工程中想生成 android.app.Activity 这种类型是不能直接 Activity.class。
     * 这种情况下只能通过 ClassName 进行引用。”
     *
     * @return
     */
    fun buildParameterizedType(): TypeName {
        return if (generics.isEmpty()) {
            if (modifier != null) {
                if (modifier == KModifier.OUT) {
                    WildcardTypeName.producerOf(getTypeName())
                } else {
                    //in out处理 例如： List<in String>
                    WildcardTypeName.consumerOf(getTypeName())
                }
            } else {
                //普通类型 例如：String
                getTypeName()
            }
        } else {
            //构建泛型 例如： Map<String,List<String>>
            getTypeName().parameterizedBy(generics.map { it.buildParameterizedType() })
        }
    }


    override fun toString(): String {
        val sb = buildString {
            if (generics.isEmpty()) {
                if (modifier != null)
                    append(modifier)
                append(typeName)
                if (nullable)
                    append("? ")
            } else {
                append("$typeName<")
                var first: Boolean = true
                generics.forEach {
                    if (!first)
                        append(",")
                    append(it.toString())
                    first = false
                }
                append(">")
            }
        }
        return sb
    }

    companion object {
        private fun getPackageName(kClass: KClass<*>): String {
            val qualifiedName = kClass.qualifiedName ?: throw IllegalArgumentException("Class $kClass does not have a qualified name.")
            val lastDotIndex = qualifiedName.lastIndexOf('.')
            return if (lastDotIndex != -1) {
                qualifiedName.substring(0, lastDotIndex)
            } else {
                // 如果没有找到点号，说明没有包名
                throw IllegalArgumentException("Class $kClass does not have a package name.")
            }
        }

        operator fun invoke(clsName: String, kClass: KClass<*>): TypeModel {
            return TypeModel(clsName, getPackageName(kClass))
        }

        operator fun invoke(kClass: KClass<*>): TypeModel {
            return TypeModel(kClass.simpleName!!, getPackageName(kClass))
        }

        //<editor-fold desc="节点解析">

        /**
         * 1. 解析val/var的子节点: generics节点
         * 2. 解析type节点和type的子节点(依旧是type节点)
         *
         * @param typeName 类型
         * @param typePackage 类型的包名
         * @param genericsNode 类型节点或者类型节点的泛型节点。特点：子节点都是 type节点。
         *    type节点有name属性，package属性，modifier属性，nullable属性等
         * @return
         */
        fun parse(typeName: String, typePackage: String?, genericsNode: Node): TypeModel {
            if (!genericsNode.childsHasElementNode()) {
                throw IllegalArgumentException("泛型为空")
            }
            val type = parse(typeName, typePackage)
            type.generics.clear()
            //遍历解析泛型
            //todo 或许这里需要筛选一下节点名称
            genericsNode.childNodes.filterAllElementNode().forEach { node ->
                //读取属性
                val genericsType = node.attrValue("name") ?: "String"
                val genericsPackage = node.attrValue("package")
                val modifier = parseModifier(node.attrValue("modifier"))
                val nullable = node.attrValue("nullable")?.toBoolean() ?: false
                //生成描述
                val clsModel = if (node.childsHasElementNode()) {
                    //说明还有泛型
                    parse(genericsType, genericsPackage, node)
                } else {
                    val clsModel = parse(genericsType, genericsPackage)
                    clsModel.nullable = nullable
                    clsModel.modifier =
                        if (modifier.isNotEmpty()) modifier[0] else null //todo 泛型只能有一个修饰符前缀
                    clsModel
                }
                type.generics.add(clsModel)
            }
            return type
        }

        fun parse(typeName: String, typePackage: String?): TypeModel {
            if (typePackage == null) {
                //如果是内置类型，可以允许不写包名。如果不是内置类型，也不写包名，直接抛出错误
                val cls: KClass<*> = parseClsInner(typeName)
                return TypeModel(typeName, cls)
            }
            return TypeModel(typeName, typePackage)
        }

        //</editor-fold>
    }
}