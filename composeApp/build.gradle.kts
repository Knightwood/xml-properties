import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(desktops.plugins.kotlinMultiplatform)
    alias(desktops.plugins.composeMultiplatform)
    alias(desktops.plugins.composeCompiler)
    alias(buildLibs.plugins.buildLogic.variant.plugin)
}

variant {
    // 设置当前激活的 buildType 与 flavor，必需设置此变量。
    activeVariant.set("dev-free")
    // 以下都是合法的 activeVariant 设置
    // activeVariant.set("dev-paid")  // 使用 dev buildType 和 paid flavor
    // activeVariant.set("dev")       // 仅使用 dev buildType，不使用 flavor

    buildType {
        // 如果没有定义buildType，默认会使用debug的buildType，
        // 调用release方法配置release的buildType，其实相当于调用create("release"){}
        // dev是自定义的buildType
        release {
            // configuration
        }
        // 创建一个新的 buildType
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

                // 配置 commonMain 源集下的BuildConfig，字段可以在jvmMain等源集中定义，这里仅需指定 expect = true 即可
                create("commonMain") {
                    expect = true
                }

                // 配置 jvmMain 源集下的BuildConfig，这里需要生成 actual的object BuildConfig类 所以这里需指定 expect = false
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
            // 配置 free flavor变体额外追加的源码目录
            sourceSets {
                //为commonMain添加额外的源码目录
                create("commonMain") {
                    sourceDir = "buildSrc/free/commonMain/kt"
                }
                //为jvmMain添加额外的源码目录和资源目录
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
