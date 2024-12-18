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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(buildLibs.plugins.android.application) apply false
    alias(buildLibs.plugins.android.library) apply false
    alias(buildLibs.plugins.kotlin.android) apply false
    alias(buildLibs.plugins.kotlin.serialization) apply false
    alias(buildLibs.plugins.compose.compiler) apply false
    alias(buildLibs.plugins.kotlin.jvm) apply false

    //插件发布后，需要在这添加依赖
//    id("com.github.knightwood.gradle.plugin.xml-properties") version "0.0.1" apply false
//    alias(buildLibs.plugins.buildLogic.android.xml.props) apply false

}