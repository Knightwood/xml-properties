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

package plugin.parsed.xml.processor.core

import com.squareup.kotlinpoet.KModifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * 解析属性的类型
 *
 * @param s
 */
fun parseClsInner(s: String?): KClass<*> {
    return when (s) {
        "MutableMap" -> MutableMap::class
        "MutableList" -> MutableList::class
        "MutableSet" -> MutableSet::class
        "Any" -> Any::class
        "String", null -> String::class
        "Boolean" -> Boolean::class
        "Int" -> Int::class
        "Long" -> Long::class
        "Float" -> Float::class
        "Double" -> Double::class
        "Char" -> Char::class
        "Byte" -> Byte::class
        "Short" -> Short::class
        "Unit" -> Unit::class
        "Array" -> Array::class
        "List" -> List::class
        "Map" -> Map::class
        "Set" -> Set::class
        "BooleanArray" -> BooleanArray::class
        "IntArray" -> IntArray::class
        "LongArray" -> LongArray::class
        "FloatArray" -> FloatArray::class
        "DoubleArray" -> DoubleArray::class
        "CharArray" -> CharArray::class
        "ByteArray" -> ByteArray::class
        "ShortArray" -> ShortArray::class
        "ArrayList" -> ArrayList::class
        "HashSet" -> HashSet::class
        "HashMap" -> HashMap::class
        "IntRange" -> IntRange::class
        "ClosedRange" -> ClosedRange::class
        "Enum" -> Enum::class
        "Annotation" -> Annotation::class
        "KClass" -> KClass::class
        "KFunction" -> KFunction::class
        else -> throw IllegalArgumentException("不支持的内置类型:$s")
    }
}

fun parseModifier(s: String?): List<KModifier> {
    val modifier: List<KModifier> = s?.split(",")?.map {
        it.parseModifier()
    } ?: emptyList()
    return modifier
}

fun String.parseModifier(): KModifier {
    return when (this) {
        "public" -> KModifier.PUBLIC
        "private" -> KModifier.PRIVATE
        "protected" -> KModifier.PROTECTED
        "internal" -> KModifier.INTERNAL
        "abstract" -> KModifier.ABSTRACT
        "final" -> KModifier.FINAL
        "open" -> KModifier.OPEN
        "lateinit" -> KModifier.LATEINIT
        "const" -> KModifier.CONST
        "inline" -> KModifier.INLINE
        "suspend" -> KModifier.SUSPEND
        "infix" -> KModifier.INFIX
        "operator" -> KModifier.OPERATOR
        "out" -> KModifier.OUT
        "in" -> KModifier.IN

        else -> throw IllegalArgumentException("unknown modifier $this ")
    }
}