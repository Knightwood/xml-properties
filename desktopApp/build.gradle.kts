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
                    menuGroup = "org.example.project.hello"
                }
                macOS {
                    // configuration
                }
            }
            buildConfigs {
                packageName = "com.github.knightwood"
                className = "BuildConfig"

                // kotlin jvm只有一个main源集，且不需要生成 expect/actual object BuildConfig类，所以不要指定expect变量，让它保持默认值null
                create("main") {
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
                create("main") {
                    sourceDir = "buildSrc/free/main/kt"
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

        // Example of how to access variant configuration
        println(variant.parseVariant().distributionInfo.linux.menuGroup)

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
