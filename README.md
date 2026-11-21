# variant-plugin

一个面向 Kotlin JVM / Kotlin Multiplatform（含 Compose Multiplatform Desktop）项目的**构建变体（Variant）插件**。

它可以用来：

- 声明 `buildType`（如 `dev`、`release`）与 `flavor`（如 `free`、`paid`），用 `activeVariant` 指定当前生效的变体，命名规则为 `buildType-flavor`（flavor 可省略）, **flavor 的配置覆盖 buildType 的配置**。
- 为不同变体**追加源码目录 / 资源目录**到对应的 Kotlin 源集（`commonMain`、`jvmMain`、`main` 等）。
- 按变体**生成 `BuildConfig.kt`**（KMP 下自动生成 `expect object` 与 `actual object`），类似 Android 的 BuildConfig。
- 按变体**收集桌面分发配置**（包名、版本、vendor、Linux/Windows/macOS 平台参数），再同步给 Compose 插件的 `nativeDistributions`。


## 引入插件依赖
我还没有发布到 Maven Central，所以需要手动引入。
1. clone此项目到本地
2. 使用idea打开在右侧的gradle面板中找到build-logic→variant-plugin→Tasks→publish 执行 `publishAllPublicationsToMavenLocalRepository`
3. 在你项目的settings.gradle.kts和模块的build.gradle.kts中引入
```kotlin
// settings.gradle.kts
plugins {
    id("com.github.knightwood.gradle.plugin.VariantPlugin") version "0.0.2" apply false
}

// build.gradle.kts
plugins {
    id("com.github.knightwood.gradle.plugin.VariantPlugin") version "0.0.2"
}

```

## 使用插件

* 变体Variant由buildType与flavor组成，可以定义多个buildType与flavor，然后使用activeVariant决定当前项目的所用buildType与flavor配置。
* activeVariant值决定当前项目所用buildType与flavor配置，其命名使用"-"分隔`buildType名称-flavor名称`，例如`dev-free`即表示使用`dev` buildType和`free` flavor配置。
* 每个buildType与flavor均可以配置三种配置项：1.customSourceSets 2.buildConfigs 3.distribution。
* flavor是可选配置，你可以根本不调用flavor dsl块，或者仅给activeVariant设置buildType名称，例如activeVariant.set("dev")，表示仅使用`dev` buildType，不使用flavor。
* flavor存在且在activeVariant中被使用时，flavor的配置会覆盖buildType的配置。
* 可以通过variant.parseActiveVariant()方法获取当前激活的buildType与flavor合并后的配置。
* distribution配置我担心直接设置给compose插件的extension可能会有问题，所以需要你手动获取变体合并后的配置设置给compose插件，相同情况的还有composeResource文件夹配置。

### 在kotlin multiplatform项目中使用
示例代码可以在composeApp模块下找到

使用：
```kotlin

// 声明所有的 buildType 与 flavor配置
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
// 设置distribution配置
compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            // 方案一：整体同步变体里的分发配置
            variant.parseVariant().syncTo(this)

            // 方案二：按需读取单个属性，自己决定优先级
            // val info = variant.parseVariant().distributionInfo
            // packageName = info.packageName ?: "org.example.project"
            // menuGroup 在平台块里：info.windows.menuGroup
        }
    }
}

// 设置composeResource目录
compose.resources {
    val merged = variant.parseVariant()
    merged.customSourceSets.findByName("jvmMain")?.composeResourceDir?.let { dir ->
        customDirectory(
            sourceSetName = "jvmMain",
            directoryProvider = provider { layout.projectDirectory.dir(dir) }
        )
    }
}

```

### 在kotlin jvm项目中使用
示例代码可以在desktopApp模块下找到


使用：
```kotlin
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

```


## 其他
[详细文档由AI生成，写一个好文档太难了，凑活看吧->](build-logic/variant-plugin/README.md)
