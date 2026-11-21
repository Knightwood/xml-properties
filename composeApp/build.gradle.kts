import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(desktops.plugins.kotlinMultiplatform)
    alias(desktops.plugins.composeMultiplatform)
    alias(desktops.plugins.composeCompiler)
    alias(buildLibs.plugins.buildLogic.variant.plugin)
}

variant {
    activeVariant.set("dev-free")
    buildType {
        release {
            // configuration
        }
        create("dev") {
            distribution {
                linux {
                    // configuration
                }
                macOS {
                    // configuration
                }
            }
            buildConfigs {
                packageName = "com.github.knightwood"
                className = "BuildConfig"

                create("commonMain") {
                    expect = true
                }

                create("jvmMain") {
                    expect = false
                    this + stringField("NAME", "Knightwood")
                    this + intField("AGE", 18)
                    this + booleanField("IS_DEBUG", false)
                    this + stringField("GENDER", null)
                }
            }
        }
    }
    flavor {
        this.create("free") {
            // configuration
            sourceSets {
                create("commonMain") {
                    sourceDir = "buildSrc/free/commonMain/kt"
                }
                create("jvmMain") {
                    sourceDir = "buildSrc/free/jvmMain/kt"
                    resourceDir = "buildSrc/free/jvmMain/res"
                }
            }
        }
    }
}

kotlin{
    compilerOptions {
        // 启用 context-receivers 特性
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
    jvm()
    jvmToolchain(17)
    sourceSets {
        commonMain {
            dependencies {
                //kotlin
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization)
                //compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.materialIconsExtended)
            }
        }
        jvmMain.dependencies {

            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
            }
            implementation(desktops.kotlinx.coroutinesSwing)
            implementation(desktops.compose.uiToolingPreview)
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
