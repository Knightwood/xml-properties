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

package com.kiylx.common.logic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    isLibrary: Boolean,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
        testOptions {
            unitTests {
                // For Robolectric
                isIncludeAndroidResources = true
            }
        }
    }
    configComposeModuleDeps()
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
        fun Provider<*>.relativeToRootProject(dir: String) = flatMap {
            rootProject.layout.buildDirectory.dir(projectDir.toRelativeString(rootDir))
        }.map { it.dir(dir) }

        project.providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
            .relativeToRootProject("compose-metrics")
            .let(metricsDestination::set)

        project.providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
            .relativeToRootProject("compose-reports")
            .let(reportsDestination::set)
        stabilityConfigurationFile =
            rootProject.layout.projectDirectory.file("compose_compiler_config.conf")

        enableStrongSkippingMode = true
    }
}

internal fun Project.configComposeModuleDeps() {
    dependencies {
        val bom ="2024.12.01"
        val composeBom = platform("androidx.compose:compose-bom:${bom}")
        implementationDeps(composeBom)
        androidTestImplementationDeps(composeBom)

        // Choose one of the following:
        // Material Design 3
        implementationDeps("androidx.compose.material3:material3")
        // or skip Material Design and build directly on top of foundational components
//          implementation("androidx.compose.foundation:foundation")
        // or only import the main APIs for the underlying toolkit systems,
        // such as input and measurement/layout
//          implementation("androidx.compose.ui:ui")

        // Android Studio Preview support
        implementationDeps("androidx.compose.ui:ui-tooling-preview")
        debugImplementationDeps("androidx.compose.ui:ui-tooling")

        // UI Tests
        androidTestImplementationDeps("androidx.compose.ui:ui-test-junit4")
        debugImplementationDeps("androidx.compose.ui:ui-test-manifest")

        // Optional - Included automatically by material, only add when you need
        // the icons but not the material library (e.g. when using Material3 or a
        // custom design system based on Foundation)
//    implementationDeps("androidx.compose.material:material-icons-core")
        implementationDeps("androidx.compose.material:material-icons-extended")
        // Optional - Add full set of material icons
//          implementation("androidx.compose.material:material-icons-extended")
        // Optional - Add window size utils
        implementationDeps("androidx.compose.material3:material3-window-size-class")
        // Optional - Integration with activities
        implementationDeps("androidx.activity:activity-compose:1.9.0")
        // Optional - Integration with ViewModels
        implementationDeps("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
        // Optional - Integration with LiveData
        implementationDeps("androidx.compose.runtime:runtime-livedata")

        //test
        androidTestImplementationDeps(platform("androidx.compose:compose-bom:${bom}"))

        // kotlinx 提供的 immutable 集合工具类
        implementationDeps("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
    }
}
