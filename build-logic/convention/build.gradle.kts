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
}

group = "com.kiylx.common.build_logic"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

//tasks.withType<KotlinCompile>().configureEach {
//    kotlinOptions {
//        jvmTarget = JavaVersion.VERSION_17.toString()
//    }
//}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
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

    plugins {
        register("androidApplication") {
            id = "kiylx.build_logic.android.application"
            implementationClass = "plugin.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "kiylx.build_logic.android.library"
            implementationClass = "plugin.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "kiylx.build_logic.android.compose"
            implementationClass = "plugin.AndroidComposeConventionPlugin"
        }
        register("aarPlugin") {
            id = "kiylx.build_logic.android.aar"
            implementationClass = "plugin.PublishAARConventionPlugin"
        }
        register("jarPlugin") {
            id = "kiylx.build_logic.jvm.jar"
            implementationClass = "plugin.PublishJARConventionPlugin"
        }
    }
}
