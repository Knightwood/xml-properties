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

import com.android.utils.forEach
import com.squareup.kotlinpoet.FileSpec
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.regex.Pattern

interface IXmlProcessor {
    fun processNode(node: Node, builder: FileSpec.Builder)
}


//<editor-fold desc="读取某个元素节点的属性值">

/**
 * 比如 <P age="19" />
 *
 * 调用attr("age) 将得到age节点
 *
 * @param name
 * @return
 */
fun Node.attr(name: String): Node? {
    return attributes.getNamedItem(name)
}

/**
 * 比如 <P age="19" />
 *
 * 调用attrValue("age) 将得到 19
 *
 * @param name
 * @return
 */
fun Node.attrValue(name: String): String? {
    return attributes.getNamedItem(name)?.nodeValue
}


//</editor-fold>
//<editor-fold desc="筛选出所有的元素节点，比如<P>,<Some>之类的">

fun Node.filterAllElementChild(): MutableList<Node> = childNodes.filterAllElementNode()

fun NodeList.filterAllElementNode(): MutableList<Node> {
    val list = mutableListOf<Node>()
    forEach {
        if (it.nodeType == Node.ELEMENT_NODE) {
            list.add(it)
        }
    }
    return list
}

fun NodeList.forEachElementNode(action: Node.() -> Unit) {
    forEach {
        if (it.nodeType == Node.ELEMENT_NODE) {
            action.invoke(it)
        }
    }
}


fun NodeList.hasElementNode(): Boolean {
    return filterAllElementNode().isNotEmpty()
}

fun Node.childsHasElementNode(): Boolean {
    return this.childNodes.filterAllElementNode().isNotEmpty()
}

fun NodeList.size(): Int = this.length

fun NodeList.findElementNode(name: String): Node? {
    return filterAllElementNode().find {
        it.nodeName == name
    }
}

fun Node.findElementChild(name: String): Node? {
    return childNodes.findElementNode(name)
}

fun Node.text(): String? {
    return this.nodeValue
}

//</editor-fold>

//<editor-fold desc="筛选出所有的元素的属性。">
fun NodeList.filterAllAttrs(): MutableList<Node> {
    val list = mutableListOf<Node>()
    forEach {
        if (it.nodeType == Node.ATTRIBUTE_NODE) {
            list.add(it)
        }
    }
    return list
}

fun NodeList.forEachAttrs(action: Node.() -> Unit) {
    forEach {
        if (it.nodeType == Node.ATTRIBUTE_NODE) {
            action.invoke(it)
        }
    }
}
//</editor-fold>

/**
 * 将List<String> 拼接为字符串
 *
 * @param separator
 * @return
 */
fun List<String>.toStr(separator: String = ","): String {
    return joinToString(separator) { Pattern.quote(it) }
}

/**
 * 将字符串转换为List
 *
 * @param separator
 * @return
 */
fun String?.toList(separator: String = ","): List<String> {
    return if (this.isNullOrEmpty()) {
        emptyList()
    } else {
        this.split(separator).map { it.trim() }
    }
}

/**
 * 在字符串中，判断是否完全匹配某个字符串
 *
 * @param name 要匹配的字符串
 * @param separator 分隔符
 * @return
 */
fun String?.fullMatch(name: String, separator: String = ","): Boolean {
    return this?.toList(separator)?.contains(name) ?: false
}

