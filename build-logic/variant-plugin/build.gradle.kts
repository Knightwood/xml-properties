import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar

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
//    id("com.gradle.plugin-publish") version "1.2.1"
    alias(libs.plugins.publish)
}

group = "com.github.knightwood.gradle.plugin"
val versionName = "0.0.2"
version = versionName


// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
//    withSourcesJar()
//    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(gradleApi())
    //实质上就是org.jetbrains.kotlin:kotlin-gradle-plugin:version
//    compileOnly(kotlin("gradle-plugin"))
    compileOnly(libs.pluginApi.agp)
    compileOnly(libs.pluginApi.agp.api)
    compileOnly(libs.pluginApi.android.tools.common)
    implementation(libs.semver)
    implementation(libs.pluginApi.gradle.kts)
    implementation(libs.pluginApi.compose.multiplatform)
    //似乎引入上面的libs.pluginApi.gradle.kts，这个libs.pluginApi.compose.multiplatform就用不上了
    implementation(libs.pluginApi.kotlin.multiplatform)
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
        register("VariantPlugin") {
            id = "com.github.knightwood.gradle.plugin.VariantPlugin"
            displayName = "Plugin for Variant"
            description = "A plugin that helps you test your plugin against a variety of Gradle versions"
            implementationClass = "com.github.knightwood.gradle.plugin.VariantPlugin"
            version = versionName
        }
    }
}

publishing {
    publications {
//        create<MavenPublication>("maven") {
//            pom {
//                groupId = "com.github.knightwood.gradle.plugin"
//                artifactId = "variant-plugin"
//                version = versionName
//            }
//        }
        repositories {
            mavenLocal()
            maven {
                name = "MyMavenLocal"
                url = uri("F:\\.m2\\repository")
            }
        }
    }
}
val Project.groupName
    get() = project.group.toString()
val Project.artifactId
    get() = project.name.toString()
val Project.moduleVersion
    get() = project.version.toString()
val Project.moduleDescription
    get() = project.description.toString()
mavenPublishing {
    publishToMavenCentral()
    afterEvaluate {
        coordinates(groupName, artifactId, moduleVersion)
    }
    configure(
        GradlePlugin(
            // configures the -javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an empty jar
            // - `JavadocJar.Javadoc()` to publish standard javadocs
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            javadocJar = JavadocJar.Javadoc(),
            // whether to publish a sources jar
            sourcesJar = true,
        )
    )
    pom {
        name.set(artifactId)
        description.set(moduleDescription)
        inceptionYear.set("2020")
        url.set("https://github.com/username/mylibrary/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("username")
                name.set("User Name")
                url.set("https://github.com/username/")
            }
        }
        scm {
            url.set("https://github.com/username/mylibrary/")
            connection.set("scm:git:git://github.com/username/mylibrary.git")
            developerConnection.set("scm:git:ssh://git@github.com/username/mylibrary.git")
        }
    }
}

