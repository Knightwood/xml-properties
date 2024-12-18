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

package plugin

import com.android.build.api.dsl.LibraryExtension
import com.kiylx.common.dependences.AndroidBuildCode
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure


class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("kotlin-parcelize")
            }
            extensions.configure<LibraryExtension> {
                compileSdk = AndroidBuildCode.compileSdk
                defaultConfig {
                    minSdk = AndroidBuildCode.minSdk
                    lint.targetSdk = AndroidBuildCode.targetSdk
//                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//                    vectorDrawables {
//                        useSupportLibrary = true
//                    }
                    consumerProguardFiles("consumer-rules.pro")
                    ndk {
                        abiFilters.addAll(AndroidBuildCode.abi)
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        consumerProguardFiles("consumer-rules.pro")
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                packagingOptions {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildFeatures {
                    viewBinding =true
                    buildConfig =true
                }
                kotlinOptions {
                    jvmTarget = JavaVersion.VERSION_17.toString()
                }
            }
        }
    }
}

