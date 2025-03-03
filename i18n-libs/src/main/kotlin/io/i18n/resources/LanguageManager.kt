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

package io.i18n.resources

import com.github.knightwood.slf4j.kotlin.logger
import org.gradle.internal.impldep.bsh.commands.dir
import java.util.Locale
import java.util.Properties

/**
 * 在compose中，的确可以根据locale的变化动态获取该locale下的字符串。 但是在非compose环境中，我们做不到这一点。
 * 而compose的语言文件的资源加载，在非compose环境中竟然需要使用suspend方法， 这简直是疯了。
 * 这个类通过读取properties文件来获取字符串，并且被设计为不需要suspend方式读取。
 *
 * @property dir 从resources目录或其子目录中读取语言文件，dir为相对于resources目录的子目录相对路径
 * @property locale 语言环境，用于加载语言文件， 默认为当前系统语言环境
 */
class LanguageManager(
    val dir: String,
    var locale: Locale,
) {
    lateinit var properties: Properties


    /**
     * 通过Locale获取文件名
     *
     * @return zh_CN.properties
     */
    private fun Locale.toFile(): String {
        val fileName = "${dir}/string_${this.toString()}.properties"
        return fileName
    }

    /**
     * 加载语言文件
     *
     * @param locale
     * @param path
     */
    fun load() {
        if (this::properties.isInitialized) {
            throw IllegalArgumentException("LanguageManager has been loaded")
        }
        val inputStream =
            javaClass.classLoader.getResourceAsStream(locale.toFile())
                ?: javaClass.classLoader.getResourceAsStream("$dir/string.properties")
        val properties = Properties()
        properties.load(inputStream)
        this.properties = properties
    }

    fun read(key: String, vararg args: Any): String {
        val value =
            properties.getProperty(key) ?: throw IllegalArgumentException("Key $key not found")
        return if (args.isEmpty()) value else String.format(value, *args)
    }

    companion object {
        @Volatile
        private var instance: LanguageManager? = null

        fun get(): LanguageManager {
            return instance!!
        }

        fun getInstance(dir: String, locale: Locale): LanguageManager {
            if (instance == null) {
                synchronized(LanguageManager::class.java) {
                    if (instance == null) {
                        instance = LanguageManager(dir, locale)
                    }
                }
            }
            return instance!!
        }
    }
}


fun main() {
    LanguageManager.getInstance("i18n", Locale.US).apply {
        load()
        println(read("app_name", "12", 31))
    }
}