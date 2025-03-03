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

plugins {
    alias(buildLibs.plugins.buildLogic.android.app)
    alias(buildLibs.plugins.buildLogic.android.compose)
    alias(buildLibs.plugins.buildLogic.android.xml.props)
    alias(buildLibs.plugins.buildLogic.i18n)
}
xmlProps {
    enabled = true // 是否启用
    dir = "resources" // 配置文件所在目录
    excludeFile += "example.xml" // 排除文件
}
android {
    namespace = "com.vines.test.app"

    defaultConfig {
        applicationId = "com.vines.test.app"
    }
    flavorDimensions += listOf("channels")
    productFlavors {
        create("baidu") {
            dimension = "channels"
        }
        create("bing") {
            dimension = "channels"
        }
    }
}

dependencies {
    implementation(buildLibs.bundles.bundleAndroidx)
    implementation(buildLibs.google.material) {
        exclude("androidx.activity", "activity")
        exclude("androidx.appcompat", "appcompat")
        exclude("androidx.constraintlayout", "constraintlayout")
        exclude("androidx.core", "core")
        exclude("androidx.recyclerview", "recyclerview")
    }
    implementation(buildLibs.bundles.kotlins)
}