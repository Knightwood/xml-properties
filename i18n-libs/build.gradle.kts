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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(buildLibs.plugins.buildLogic.i18n)
}

val myCodeGenOutDir = "build/generated/mycode"

i18n {
    inDir = "src/main/resources/i18n"
    outDir = myCodeGenOutDir
}

group = "com.github.knightwood.gradle.plugin"
version = "0.0.1"

kotlin {
    compilerOptions {
        jvmToolchain(17)
    }
    jvm() {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(myCodeGenOutDir)
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.slf4j.api.kotlin)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.components.resources)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.logback.classic)
            }
        }
    }
}
/*
 crosspasted没有使用插件生成资源id，使用时需要手动拼写id，从property文件中读取数据，使用String.format格式化
 downloader使用了插件生成资源id，仅支持{{name}}格式，使用replace替换。
 还提供了compose函数，方便参数变更时自动更新。代码质量不高。
 */
