import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(desktops.plugins.kotlinJvm)
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

                create("main") {
                    this + intField("AGE", 18)
                    this + booleanField("IS_DEBUG", false)
                    this + stringField("GENDER", null)
//                    this + field<IntRange>("AGE", 2..10)
                }
                create("jvmMain") {
                    isExpect = false
                    this + stringField("NAME", "Knightwood")
                }
            }
        }
    }
    flavor {
        this.create("free") {
            // configuration
            sourceSets {
                create("main") {
                    sourceDir = "buildSrc/free/main/kt"
                }
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
dependencies {
    implementation(compose.desktop.currentOs)
    implementation(desktops.kotlinx.coroutinesSwing)
    implementation(desktops.compose.uiToolingPreview)
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
