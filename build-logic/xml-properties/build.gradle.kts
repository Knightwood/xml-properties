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
    `kotlin-dsl`
    //`java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.github.knightwood.gradle.plugin"
version = "0.0.1"


// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    implementation("com.squareup:kotlinpoet:2.0.0")
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {

    website = "https://github.com/ysb33r/gradleTest"
    vcsUrl = "https://github.com/ysb33r/gradleTest.git"

    plugins {
//        register("propsPlugin") {
//            id = "com.github.knightwood.gradle.plugin.props"
//            displayName = "Plugin for convert xml to kotlin of Gradle plugins"
//            description = "A plugin that helps you test your plugin against a variety of Gradle versions"
//            implementationClass = "plugin.AppPropertiesPlugin"
//        }
        register("xmlPlugin") {
            id = "com.github.knightwood.gradle.plugin.xml-properties"
            displayName = "Plugin for convert xml to kotlin of Gradle plugins"
            description = "A plugin that helps you test your plugin against a variety of Gradle versions"
            implementationClass = "plugin.XmlPropertiesPlugin"
            version = "0.0.1"
        }
        register("buildConfigPlugin") {
            id = "com.github.knightwood.gradle.plugin.buildConfig"
            displayName = "Plugin for BUILD CONFIG"
            description = "A plugin that helps you test your plugin against a variety of Gradle versions"
            implementationClass = "plugin.BuildConfigPlugin"
            version = "0.0.1"
        }
        register("i18nPlugin") {
            id = "com.github.knightwood.gradle.plugin.i18n"
            displayName = "Plugin for i18n"
            description = "A plugin that helps you test your plugin against a variety of Gradle versions"
            implementationClass = "plugin.I18nPlugin"
            version = "0.0.1"
        }
    }
}

publishing {
    publications {
        repositories {
            mavenLocal()
//            maven {
//                url = uri("../repo")
//            }
        }
    }
}