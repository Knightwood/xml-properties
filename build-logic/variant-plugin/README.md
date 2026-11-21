# variant-plugin

一个面向 Kotlin JVM / Kotlin Multiplatform（含 Compose Multiplatform Desktop）项目的**构建变体（Variant）插件**。

它把 Android 中 `buildType + flavor` 的思路搬到 JVM/KMP 工程上，用来：

- 声明 `buildType`（如 `dev`、`release`）与 `flavor`（如 `free`、`paid`），用 `activeVariant` 指定当前生效的变体，命名规则为 `buildType-flavor`（flavor 可省略）。
- 为不同变体**追加源码目录 / 资源目录**到对应的 Kotlin 源集（`commonMain`、`jvmMain`、`main` 等）。
- 按变体**生成 `BuildConfig.kt`**（KMP 下自动生成 `expect object` 与 `actual object`），类似 Android 的 BuildConfig。
- 按变体**收集桌面分发配置**（包名、版本、vendor、Linux/Windows/macOS 平台参数），再同步给 Compose 插件的 `nativeDistributions`。
- 合并规则：**flavor 的配置覆盖 buildType 的同名配置**。

插件 ID：`com.github.knightwood.gradle.plugin.VariantPlugin`

---

## 一、环境要求

| 项目 | 要求 |
| --- | --- |
| JDK | 17（插件自身以 JVM 17 编译） |
| Gradle | 8.x（本仓库使用 8.11.1） |
| Kotlin Gradle Plugin | 2.2.x（本插件编译期对齐 `2.2.10`） |
| Compose Multiplatform | 仅在使用 `distribution { }` 时需要（插件运行期依赖 `1.8.2`） |

> 插件以 `compileOnly` 方式依赖 AGP，因此**目前不适用于 `com.android.*` 项目**，仅支持 `org.jetbrains.kotlin.jvm` 与 `org.jetbrains.kotlin.multiplatform`。

---

## 二、发布到本地 Maven 仓库

`variant-plugin/build.gradle.kts` 使用 `com.vanniktech.maven.publish` 发布，坐标为：

```
com.github.knightwood.gradle.plugin:variant-plugin:<version>
```

同时会自动发布 Gradle 插件标记（Plugin Marker），供 `plugins { id(...) }` 解析：

```
com.github.knightwood.gradle.plugin.VariantPlugin:com.github.knightwood.gradle.plugin.VariantPlugin.gradle.plugin:<version>
```

### 1. 版本号

版本号在 `build-logic/variant-plugin/build.gradle.kts` 中维护：

```kotlin
group = "com.github.knightwood.gradle.plugin"
val versionName = "0.0.2"   // 改这里
version = versionName
```

> 如果外部工程通过 version catalog 以 `version` 引用本插件，记得让 catalog 中的版本与此处保持一致。

### 2. 发布命令

`variant-plugin` 是 `build-logic` 这个 included build 的子模块，用根目录 wrapper 加 `-p build-logic` 指定构建目录即可：

```powershell
# 发布到 Gradle 的 maven local（默认 %USERPROFILE%\.m2\repository，
# 若存在 ~/.m2/settings.xml 的 <localRepository> 则以该路径为准）
.\gradlew.bat -p build-logic :variant-plugin:publishToMavenLocal

# 发布到 build.gradle.kts 里显式声明的仓库 F:\.m2\repository
.\gradlew.bat -p build-logic :variant-plugin:publishAllPublicationsToMyMavenLocalRepository
```

其它可用任务（`--group publishing` 可见）：

```powershell
.\gradlew.bat -p build-logic :variant-plugin:tasks --group publishing
```

- `publishToMavenCentral` / `publishAndReleaseToMavenCentral`：发布到 Maven Central，需要在 `gradle.properties` 或环境变量中配置 Sonatype 凭据与签名，本地开发不用。
- 增量验证：发布后检查产物目录，`.../variant-plugin/0.0.2/` 下应包含 `.jar`、`.module`、`.pom`、`-sources.jar`，以及同级的 `VariantPlugin/` 标记目录。

### 3. 仓库路径提醒

本项目在 `settings.gradle.kts` 中同时声明了两处本地仓库：

```kotlin
mavenLocal()                                   // 默认 ~/.m2/repository
maven { url = uri("F:/.m2/repository") }       // 自定义本地仓库
```

发布到哪个仓库，外部工程就必须在 `pluginManagement.repositories` 中声明**同一个**仓库，否则会报 `Plugin [id: 'com.github.knightwood.gradle.plugin.VariantPlugin'] was not found`。

---

## 三、引入插件

### 方式 A：源码方式（included build，本仓库当前用法）

适合在本仓库内开发调试，改动即时生效，无需发布：

`settings.gradle.kts`（根工程）

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

`build-logic/gradle/libs.versions.toml` 中注册别名，供业务模块引用：

```toml
[plugins]
buildLogic-variant-plugin = { id = "com.github.knightwood.gradle.plugin.VariantPlugin", version = "0.0.2" }
```

> 使用 included build 时，`version` 不参与解析（直接使用源码），但仍需写全三个字段才符合 catalog 语法。

### 方式 B：已发布坐标方式（外部工程推荐）

`settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        // 与上一节发布的目标仓库保持一致
        mavenLocal()
        // 或： maven { url = uri("F:/.m2/repository") }

        // 插件的运行期依赖（kotlin-gradle-plugin、compose-gradle-plugin、kotlinpoet、semver）
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
```

模块 `build.gradle.kts`

```kotlin
plugins {
    id("com.github.knightwood.gradle.plugin.VariantPlugin") version "0.0.2"
}
```

或通过 version catalog：

```toml
[versions]
variant-plugin = "0.0.2"

[plugins]
variant-plugin = { id = "com.github.knightwood.gradle.plugin.VariantPlugin", version.ref = "variant-plugin" }
```

```kotlin
plugins {
    alias(libs.plugins.variant.plugin)
}
```

### 方式 C：buildscript classpath

```kotlin
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("com.github.knightwood.gradle.plugin:variant-plugin:0.0.2")
    }
}

apply(plugin = "com.github.knightwood.gradle.plugin.VariantPlugin")
```

> 插件需要 Kotlin 插件已经（或随后）被应用；它通过 `plugins.withId("org.jetbrains.kotlin.jvm" / "org.jetbrains.kotlin.multiplatform")` 挂接逻辑，因此对 `plugins {}` 中的顺序不敏感。

---

## 四、最简示例（5 分钟上手）

本节只做一件事：**让插件生成一个 `BuildConfig`，并在代码里读到它的值**。不涉及 flavor、不涉及桌面分发，把变量降到最少。

### 4.1 Kotlin JVM 最小可运行示例

目录结构：

```
demo/
├── settings.gradle.kts
├── build.gradle.kts
└── src/main/kotlin/com/example/app/Main.kt
```

`settings.gradle.kts`（只打开仓库，让插件能被解析）

```kotlin
pluginManagement {
    repositories {
        mavenLocal()          // 或 maven { url = uri("F:/.m2/repository") }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "demo"
```

`build.gradle.kts`（**全部插件配置只有这几行**）

```kotlin
plugins {
    kotlin("jvm") version "2.2.10"
    application
    id("com.github.knightwood.gradle.plugin.VariantPlugin") version "0.0.2"
}

variant {
    // ① 必须：指定当前生效的变体，这里只有 buildType，没有 flavor
    activeVariant.set("dev")

    // ② 声明一个名为 dev 的 buildType
    buildType {
        create("dev") {
            // ③ 声明要生成的 BuildConfig 内容
            buildConfigs {
                packageName = "com.example.app"     // 生成类所在包
                className = "BuildConfig"           // 生成类名（默认就是 BuildConfig）

                create("main") {                    // Kotlin JVM 唯一源集就叫 main
                    this + stringField("ENV", "dev")
                    this + booleanField("IS_DEBUG", true)
                    this + intField("API_LEVEL", 1)
                }
            }
        }
    }
}

application {
    mainClass.set("com.example.app.MainKt")
}
```

`src/main/kotlin/com/example/app/Main.kt`

```kotlin
package com.example.app

fun main() {
    // BuildConfig 由插件生成，IDE 里可能需要先同步一次工程才会出现
    println("env=${BuildConfig.ENV}, debug=${BuildConfig.IS_DEBUG}, api=${BuildConfig.API_LEVEL}")
}
```

先跑一次生成任务，再运行：

```powershell
.\gradlew.bat GenerateBuildConfigTask   # 生成 BuildConfig.kt
.\gradlew.bat run                       # 输出：env=dev, debug=true, api=1
```

生成的文件：

```kotlin
// build/generated/source/buildConfig/main/kotlin/com/example/app/BuildConfig.kt
public object BuildConfig {
  public val ENV: String = "dev"
  public val IS_DEBUG: Boolean = true
  public val API_LEVEL: Int = 1
}
```

**注意：你不需要写任何 `srcDir(...)`**。插件会在配置阶段把 `build/generated/source/buildConfig/main/kotlin` 自动加入 `main` 源集。

### 4.2 切换到另一个变体

再加一个 buildType，然后改 `activeVariant` 即可：

```kotlin
variant {
    activeVariant.set("release")     // 改这一行切换

    buildType {
        create("dev") {
            buildConfigs {
                packageName = "com.example.app"
                create("main") {
                    this + stringField("ENV", "dev")
                    this + booleanField("IS_DEBUG", true)
                }
            }
        }
        create("release") {
            buildConfigs {
                packageName = "com.example.app"
                create("main") {
                    this + stringField("ENV", "release")
                    this + booleanField("IS_DEBUG", false)
                }
            }
        }
    }
}
```

重新执行后，`BuildConfig.ENV` 变成 `"release"`。**同一份源码，两个变体两套常量**，这就是本插件的核心价值。

### 4.3 Kotlin Multiplatform 最小示例

KMP 唯一的差别在 `buildConfigs` 里要多写一个"公共源集" bundle，并把 `expect` 标出来：

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.github.knightwood.gradle.plugin.VariantPlugin") version "0.0.2"
}

variant {
    activeVariant.set("dev")
    buildType {
        create("dev") {
            buildConfigs {
                packageName = "com.example.app"

                create("commonMain") { expect = true }   // 生成 expect object
                create("jvmMain") {                      // 生成 actual object
                    expect = false
                    this + stringField("ENV", "dev")
                }
            }
        }
    }
}

kotlin {
    jvm()
}
```

生成两个文件：

```kotlin
// build/generated/source/buildConfig/commonMain/kotlin/com/example/app/BuildConfig.kt
public expect object BuildConfig {
  public val ENV: String
}

// build/generated/source/buildConfig/jvmMain/kotlin/com/example/app/BuildConfig.jvmMain.kt
public actual object BuildConfig {
  public actual val ENV: String = "dev"
}
```

`commonMain` 里的代码直接写 `BuildConfig.ENV` 即可，无需任何 import 以外的声明。

### 4.4 上手检查清单

| 步骤 | 是否必须 | 说明 |
| --- | --- | --- |
| 应用插件 `com.github.knightwood.gradle.plugin.VariantPlugin` | 必须 | Kotlin JVM / KMP 插件建议先于或同时应用 |
| `variant { activeVariant.set("...") }` | 必须 | 不设置会直接抛异常 |
| 至少一个 `buildType { create(...) }` | 必须 | 名称不存在时会回退到内置 `debug` 配置 |
| `buildConfigs { packageName = ... }` | 使用 BuildConfig 时 | `packageName` 有默认值 `com.github.knightwood.config` |
| `create("<源集名>")` | 使用 BuildConfig 时 | JVM 是 `main`，KMP 通常是 `commonMain` + 各平台源集 |
| `sourceSets { }` / `distribution { }` | 可选 | 不用就整块省略 |

---

## 五、核心概念

### 5.1 变体（Variant）= buildType [+ flavor]

一个**变体名**由 `buildType` 与可选的 `flavor` 用 `-` 连接而成，由 `activeVariant` 指定：

| `activeVariant` | 使用的 buildType | 使用的 flavor |
| --- | --- | --- |
| `"dev"` | `dev` | 无 |
| `"dev-free"` | `dev` | `free` |
| `"release-paid"` | `release` | `paid` |

解析规则（源码 `VariantExtensions.parseVariant()`）：

- `buildType` 名 = `-` 之前的部分；`flavor` 名 = `-` 之后的部分。
- 因此**变体名中不允许出现第二个 `-`**，`flavor` 名称里也不要带 `-`。

### 5.2 三种可配置内容

`buildType` 与 `flavor` 都是 `VariantBuilder` 的子类，可配置内容完全相同，共三块：

| 配置块 | 产出物 | 作用阶段 |
| --- | --- | --- |
| `sourceSets { }` | 追加到 Kotlin 源集的额外目录 | 配置阶段（构建脚本可用） |
| `buildConfigs { }` | 生成的 `BuildConfig.kt` 源码 | 执行阶段（`GenerateBuildConfigTask`） |
| `distribution { }` | 桌面分发元数据（纯数据） | 配置阶段被读取，由你同步给 Compose |

### 5.3 合并模型：flavor 覆盖 buildType

插件**不会**简单地"二选一"，而是把 buildType 与 flavor 的配置**逐项合并**，同名项 flavor 优先：

```
        buildType(dev)                flavor(free)              MergedVariantExtension
   ┌────────────────────┐      ┌────────────────────┐      ┌──────────────────────────┐
   │ distribution       │      │ distribution       │      │ distribution             │
   │  packageName=A     │ ──▶  │  packageName=null  │ ──▶  │  packageName=A (保留旧值) │
   │  vendor=V          │      │  vendor=W          │      │  vendor=W     (被覆盖)    │
   ├────────────────────┤      ├────────────────────┤      ├──────────────────────────┤
   │ sourceSets         │      │ sourceSets         │      │ sourceSets               │
   │  jvmMain/kt-a      │      │  jvmMain/res-b     │      │  jvmMain/{kt-a, res-b}   │
   ├────────────────────┤      ├────────────────────┤      ├──────────────────────────┤
   │ buildConfigs       │      │ buildConfigs       │      │ buildConfigs             │
   │  ENV, IS_DEBUG     │      │  ENV=paid          │      │  ENV=paid, IS_DEBUG      │
   └────────────────────┘      └────────────────────┘      └──────────────────────────┘
```

"是否已配置"的判定规则（决定了能不能被覆盖）：

| 类型 | 视为"已配置"的条件 |
| --- | --- |
| 可空属性（`String?`、`Boolean?`…） | 值非 `null` |
| Gradle `Property<T>` | `isPresent == true` |
| `Boolean`（分发平台的布尔开关） | **与 Compose 默认值不同**才算已配置 |
| 集合（如 `targetFormats`） | 非空 |

> 由此得出两个容易踩的结论：
> 1. flavor 里写 `packageName = null` 或干脆不写，**不会**清空 buildType 的值；
> 2. 平台布尔开关**无法从 `true` 覆盖回 `false`**（`false` 等于默认值，被认为"没配置"）。

### 5.4 BuildConfig 的生成模型

`buildConfigs { create("<源集名>") { ... } }` 里的每个 bundle 对应**一个源集**，插件为每个 bundle 生成一个 `object`，修饰符由 `expect` 决定：

| `expect` | 生成的声明 | 文件 | 适用场景 |
| --- | --- | --- | --- |
| `null`（默认） | `object BuildConfig` | `BuildConfig.kt` | Kotlin JVM（无 KMP 语义） |
| `true` | `expect object BuildConfig` | `BuildConfig.kt` | KMP 公共源集（`commonMain`） |
| `false` | `actual object BuildConfig` | `BuildConfig.<源集名>.kt` | KMP 平台源集（`jvmMain` 等） |

关键行为：**字段会自动"下沉/上浮"**。合并阶段会把所有 `expect = false` bundle 的字段回填到 `expect = true` 的 bundle 里，因此在 `jvmMain` 里定义的字段，`commonMain` 的 `expect object` 会自动声明，不需要在 `commonMain` 再写一遍（写了反而会生成重复属性）。

### 5.5 两个阶段：配置阶段 vs 执行阶段

| 阶段 | 插件做的事 |
| --- | --- |
| 配置阶段（`afterEvaluate`） | 解析并合并 DSL → 把生成目录 `srcDir(...)` 注册进各源集 → 注册 `GenerateBuildConfigTask` |
| 执行阶段（任务运行） | 读取可序列化快照 → 用 KotlinPoet 写 `BuildConfig.kt` |

源集目录之所以必须在**配置阶段**注册，是因为 Gradle 编译任务的源集模型在执行阶段不可再修改（源码注释里记录了当初尝试用 `TaskProvider` 延迟注册而失败的教训）。

### 5.6 与 Android 概念对照

| Android（AGP） | 本插件 |
| --- | --- |
| `buildTypes { debug {} release {} }` | `variant { buildType { debug {} release {} } }` |
| `productFlavors { free {} }` | `variant { flavor { create("free") {} } }` |
| `variantFilter` / `flavorDimensions` | 无（单维度，靠 `activeVariant` 手动切换） |
| `buildConfigField("String", "ENV", "\"dev\"")` | `this + stringField("ENV", "dev")`（类型安全，无需手写引号） |
| `src/<flavor>/kotlin` 自动源集 | `sourceSets { create("main") { sourceDir = "..." } }` |
| `BuildConfig` 自动生成 | `buildConfigs { }` 按需生成，命名与包名可自定义 |

---

## 六、插件工作原理

这一节面向想改动插件、或想判断"这样做为什么可以/不可以"的读者。

### 6.1 应用入口

`VariantPlugin.apply(project)` 只做两件事：

```kotlin
val extension = target.extensions.create<VariantExtensions>("variant", VariantExtensions::class.java)
target.afterEvaluate { /* ... */ }
```

- 创建名为 `variant` 的扩展 → 这就是 `build.gradle.kts` 里 `variant { }` 的来源（Kotlin DSL 会为它生成类型安全访问器）。
- `VariantExtensions` 自身再持有两个容器：`buildTypeCollectionExtension`、`flavorCollectionExtension`。

### 6.2 为什么逻辑写在 `afterEvaluate` 里

`parseVariant()` 需要读完整份 DSL（多个 `buildType` / `flavor` 可能分散在脚本不同位置，甚至来自其他插件或 `apply from`），所以在 `afterEvaluate` 中才解析。副作用：**构建脚本里 `evaluationDependsOn` 之前读取变体配置拿到的是空值**，请只在脚本尾部或 `compose.desktop { }` 这类配置块里调用 `variant.parseVariant()`。

### 6.3 按 Kotlin 插件类型分流

```kotlin
plugins.withId("org.jetbrains.kotlin.multiplatform") { /* KMP 分支 */ }
plugins.withId("org.jetbrains.kotlin.jvm")           { /* JVM 分支 */ }
```

两个分支做的事一样，只是取扩展的类型不同（`KotlinMultiplatformExtension` vs `KotlinJvmProjectExtension`）：

1. `applyCustomSourceSetToCurrentProject(config, sourceSets)` —— 遍历项目**已存在的**源集，按名称匹配 `config.customSourceSets`，把 `sourceDir` / `resourceDir` 追加进去（`kotlin.srcDir(...)`、`resources.srcDir(...)`）。
2. `generateBuildConfigForAllTargets(config, sourceSets)` —— 注册生成任务并把输出目录注册为源集目录。

> 注意：源集是"遍历已存在的"，所以 `sourceSets` 里配置的源集名必须真实存在，写错不会报错，只是**静默不生效**。

### 6.4 BuildConfig 生成任务

```kotlin
tasks.register<BuildConfigGeneratorTask>("GenerateBuildConfigTask") {
    buildConfigPackageName.set(config.buildConfigPackageName)
    buildConfigClassName.set(config.buildConfigClassName)
    allTargetBuildConfigFields.set(config.buildConfigFields.map { it.toInput() })
    outputFolder.set(layout.buildDirectory.dir("generated/source/buildConfig"))
    targetSourceSetNames.set(targets.names)
}
```

任务内部用 KotlinPoet 生成 `TypeSpec.objectBuilder(className)`，逐字段生成 `PropertySpec`：

- `isExpect == null` → 带初始化器，无修饰符；
- `isExpect == true` → `expect` + 只有类型，无初始化器（`expect` 属性不能有初始化器，也不能 `const`）；
- `isExpect == false` → `actual` + 带初始化器。

字面量处理（`BuildConfigField.initializer()`）：`null` → `null`；`String` → 转义后的字符串字面量；`Char` → 转义后的字符字面量；`Float` → 追加 `f`；`Long` → 追加 `L`；其它类型 → 直接 `toString()` 输出。

### 6.5 输入快照：为配置缓存而设计

Gradle 任务的 `@Input` 必须可序列化。插件没有把 DSL 里的活对象丢进任务，而是转成纯数据快照：

```kotlin
data class BuildConfigBundleInput(val name: String, val isExpect: Boolean?, val fields: List<BuildConfigFieldInput>)
data class BuildConfigFieldInput(val name: String, val typeName: String, val value: java.io.Serializable?)
```

对应转换函数 `BuildConfigFieldBundleBuilder.toInput()`；`targetSourceSetNames` 也**只保存字符串集合**而非 KGP 的 `KotlinSourceSet` 对象。这样在 `--configuration-cache` 下任务输入可被正确序列化与复用。

### 6.6 IDE 集成

```kotlin
tasks.configureEach {
    if (task.name == "prepareKotlinIdeaImport") task.dependsOn(tasks.withType(BuildConfigGeneratorTask::class.java))
}
```

`prepareKotlinIdeaImport` 是 Kotlin 插件在 IDE 同步时执行的任务，让它依赖生成任务，就能保证"同步后 BuildConfig 已在 `build/generated/...` 里"，避免刚 clone 项目时 `BuildConfig` 一片红。

### 6.7 数据流总览

```
build.gradle.kts
  variant { buildType { dev { distribution{} sourceSets{} buildConfigs{} } }
            flavor    { free { ... } } }
        │
        ▼  afterEvaluate
  VariantExtensions.parseVariant()
        │  合并（flavor > buildType）
        ▼
  MergedVariantExtension
        ├── customSourceSets ──▶ kotlin.srcDir / resources.srcDir（配置阶段）
        ├── distributionInfo ──▶ variant.parseVariant().syncTo(nativeDistributions)（你手动调用）
        └── buildConfigFields ─▶ GenerateBuildConfigTask ──▶ build/generated/source/buildConfig/<srcSet>/kotlin
```

---

## 七、DSL 详解

完整骨架：

```kotlin
variant {
    // 设置当前激活的 buildType 与 flavor，必需设置此变量。
    activeVariant.set("dev-free")
    // 以下都是合法的 activeVariant 设置
    // activeVariant.set("dev-paid")  // 使用 dev buildType 和 paid flavor
    // activeVariant.set("dev")       // 仅使用 dev buildType，不使用 flavor

    buildType {
        release { /* VariantBuilder 配置 */ }
        debug { /* 内置名称 */ }
        create("dev") { /* 自定义 buildType */ }
    }

    // flavor 配置是可选项，仅使用 buildType 时 flavor 块可以不写
    flavor {
        create("free") { /* 与 buildType 相同的 VariantBuilder 配置 */ }
        create("paid") { }
    }
}
```

### 7.1 `variant { }` —— `VariantExtensions`

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| `activeVariant` | `Property<String>` | 当前变体名，必填；未设置时解析会抛 `InvalidUserDataException` |
| `buildType(action)` | `Action<BuildTypeCollectionExtension>` | 声明构建类型集合 |
| `flavor(action)` | `Action<FlavorCollectionExtension>` | 声明风味集合（可选） |
| `parseVariant()` | `MergedVariantExtension` | 解析并合并当前变体配置（见第十节） |

### 7.2 `buildType { }` —— `BuildTypeCollectionExtension`

| 成员 | 说明 |
| --- | --- |
| `release { }` / `debug { }` | 内置便捷写法，等价于 `create("release" / "debug")`，并把 `debug` 标记为 `true`/`false` |
| `create(name) { }` | 自定义 buildType，接收者为 `BuiltTypeBuilder`（含 `name`、`debug: Boolean`） |
| 重名 | 重复 `create` 同名会打印 warn 并忽略后一次 |

`BuiltTypeBuilder.debug` 只是元数据，用于 `MergedVariantExtension.debug` 与未知名称时的回退选择，**不会**自动切换 Kotlin 编译参数或构建类型。

### 7.3 `flavor { }` —— `FlavorCollectionExtension`

| 成员 | 说明 |
| --- | --- |
| `create(name) { }` | 自定义 flavor，接收者为 `FlavorBuilder`（含 `name`） |

`buildType` 与 `flavor` 都是 `VariantBuilder` 的子类，因此三大配置块完全通用；区别只在合并优先级（flavor > buildType）。

### 7.4 `distribution { }` —— `DistributionInfoBuilderExtension`

继承自 Compose 的 `AbstractDistributions`，可用属性与 Compose 的 `nativeDistributions` 基本一致：

| 属性 / 方法 | 说明 |
| --- | --- |
| `packageName` | 安装包名 / 应用名 |
| `packageVersion` | 版本号（Windows MSI 要求 x.y.z 且 major > 0） |
| `vendor` / `copyright` / `description` | 厂商、版权、描述 |
| `targetFormats(...)` | 目标格式集合，如 `TargetFormat.Msi`、`TargetFormat.Deb` |
| `appResourcesRootDir` / `licenseFile` | 资源根目录与许可文件 |
| `linux { }` | `menuGroup`、`appCategory`、`debMaintainer`、`rpmLicenseType`、`shortcut` 等 |
| `windows { }` | `menuGroup`、`upgradeUuid`、`console`、`dirChooser`、`perUserInstall`、`shortcut`、`menu` 等 |
| `macOS { }` | `bundleID`、`dockName`、`appStore`、`signing { }`、`notarization { }`、`entitlementsFile` 等 |
| `fileAssociation(...)` | 一次性为三大平台注册文件关联 |

插件只是**收集**这些配置，不会自动写到 Compose 插件上；同步需要你显式调用 `syncTo(...)`（见第十节）。原因是 Compose 的 `nativeDistributions` 在插件 `apply` 时无法安全拿到，需要由构建脚本在 `compose.desktop { }` 内部发起。

### 7.5 `sourceSets { }` —— `SourceSetCollectionExtension`

```kotlin
sourceSets {
    create("commonMain") {
        sourceDir = "buildSrc/free/commonMain/kt"       // Kotlin/Java 源码目录
    }
    create("jvmMain") {
        sourceDir = "buildSrc/free/jvmMain/kt"
        resourceDir = "buildSrc/free/jvmMain/res"       // 资源目录
        composeResourceDir = "buildSrc/free/jvmMain/composeResources" // compose 资源目录
    }
}
```

| 属性 | 说明 | 是否自动加入源集 |
| --- | --- | --- |
| `sourceDir` | Kotlin 源码目录，相对当前模块 | 是（`kotlin.srcDir`） |
| `resourceDir` | 资源目录，相对当前模块 | 是（`resources.srcDir`） |
| `composeResourceDir` | Compose Resources 目录 | **否**，需手动接入 `compose.resources` |

推荐目录约定（插件不强制，但能保持多模块一致）：

```
moduleA/
├── build/
├── src/                    # 公共主源码
├── buildSrc/               # 变体专属源码（按 flavor 划分）
│   └── free/
│       ├── commonMain/kt
│       ├── jvmMain/kt
│       └── jvmMain/res
└── build.gradle.kts
```

### 7.6 `buildConfigs { }` —— `BuildConfigFieldsBundleCollectionExtension`

| 成员 | 默认值 | 说明 |
| --- | --- | --- |
| `packageName: String` | `com.github.knightwood.config` | 生成类所在包 |
| `className: String` | `BuildConfig` | 生成类名 |
| `create(target) { }` | — | 为名为 `target` 的源集创建字段 bundle |

`BuildConfigFieldBundleBuilder`（bundle 内可用）：

| 成员 | 说明 |
| --- | --- |
| `expect: Boolean?` | `true` → `expect object`；`false` → `actual object`；`null`（默认）→ 无修饰符 |
| `intField` / `longField` / `shortField` / `byteField` | 整型字段 |
| `floatField` / `doubleField` | 浮点字段 |
| `booleanField` / `charField` / `stringField` | 布尔、字符、字符串字段 |
| `field<T : Serializable>(name, value)` | 自定义类型字段（类型名取 `T::class.asTypeName()`） |
| `plus(field)` → `this + field` | 追加字段 |
| `minus(name)` → `this - "NAME"` | 按名称移除字段 |
| `addAll(fields)` | 批量追加 |
| `replace(name, newField)` | 替换同名字段 |

完整字段类型一览：

| 工厂函数 | 生成类型 | 示例 |
| --- | --- | --- |
| `intField("A", 18)` | `Int` | `val A: Int = 18` |
| `longField("A", 18L)` | `Long` | `val A: Long = 18L` |
| `shortField("A", 18)` | `Short` | `val A: Short = 18` |
| `byteField("A", 18)` | `Byte` | `val A: Byte = 18` |
| `floatField("A", 1.5f)` | `Float` | `val A: Float = 1.5f` |
| `doubleField("A", 1.5)` | `Double` | `val A: Double = 1.5` |
| `booleanField("A", true)` | `Boolean` | `val A: Boolean = true` |
| `charField("A", 'x')` | `Char` | `val A: Char = 'x'` |
| `stringField("A", "s")` | `String` | `val A: String = "s"` |
| 任意字段传 `null` | 可空类型 | `val A: String? = null` |

示例与生成结果（KMP）：

```kotlin
buildConfigs {
    packageName = "com.github.knightwood"
    className = "BuildConfig"

    create("commonMain") { expect = true }          // 不需要重复声明字段
    create("jvmMain") {
        expect = false
        this + stringField("NAME", "Knightwood")
        this + intField("AGE", 18)
        this + booleanField("IS_DEBUG", false)
        this + stringField("GENDER", null)
    }
}
```

```kotlin
// build/generated/source/buildConfig/commonMain/kotlin/com/github/knightwood/BuildConfig.kt
public expect object BuildConfig {
  public val NAME: String
  public val AGE: Int
  public val IS_DEBUG: Boolean
  public val GENDER: String?
}

// build/generated/source/buildConfig/jvmMain/kotlin/com/github/knightwood/BuildConfig.jvmMain.kt
public actual object BuildConfig {
  public actual val NAME: String = "Knightwood"
  public actual val AGE: Int = 18
  public actual val IS_DEBUG: Boolean = false
  public actual val GENDER: String? = null
}
```

> 字段顺序＝声明顺序（内部使用有序集合），便于 diff 时保持稳定。

---

## 八、Kotlin Multiplatform 项目中的完整用法

参考 `composeApp/build.gradle.kts`：

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(desktops.plugins.kotlinMultiplatform)
    alias(desktops.plugins.composeMultiplatform)
    alias(desktops.plugins.composeCompiler)
    alias(buildLibs.plugins.buildLogic.variant.plugin)   // 本插件
}

variant {
    activeVariant.set("dev-free")

    buildType {
        release { }
        create("dev") {
            distribution {
                linux { menuGroup = "org.example.project" }
                macOS { }
            }
            buildConfigs {
                packageName = "com.github.knightwood"
                className = "BuildConfig"

                create("commonMain") { expect = true }
                create("jvmMain") {
                    expect = false
                    this + stringField("NAME", "Knightwood")
                    this + intField("AGE", 18)
                }
            }
        }
    }

    flavor {
        create("free") {
            sourceSets {
                create("commonMain") { sourceDir = "buildSrc/free/commonMain/kt" }
                create("jvmMain") {
                    sourceDir = "buildSrc/free/jvmMain/kt"
                    resourceDir = "buildSrc/free/jvmMain/res"
                }
            }
        }
    }
}

kotlin {
    jvm()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies { /* ... */ }
        jvmMain.dependencies { /* ... */ }
    }
}
```

要点：

1. `buildConfigs` 里的 `create("<target>")` 名称必须是项目真实存在的源集名（`commonMain`、`jvmMain`…），否则该 bundle 会被静默跳过。
2. KMP 下"公共源集"必须显式列出并设 `expect = true`，否则生成的 `BuildConfig` 会变成普通 `object` 落进平台源集，`commonMain` 里就用不到。
3. 每个平台源集都应有一个 `expect = false` 的 bundle，且**各平台的字段集合应保持一致**（值可不同），详见第十一节。
4. `sourceSets` 中的 `sourceDir` / `resourceDir` 是**相对当前模块**的路径。
5. 新增 target（如 `js()`、`androidTarget()`）时，记得同步补上对应的 bundle，否则该 target 没有 `actual`。
6. 生成目录 `build/generated/source/buildConfig/<sourceSet>/kotlin` 由插件自动注册，无需手工 `srcDir`。

---

## 九、Kotlin JVM（Compose Desktop）项目中的完整用法

参考 `desktopApp/build.gradle.kts`：

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import com.github.knightwood.gradle.plugin.extensions.syncTo   // 使用 syncTo 时需要

plugins {
    alias(desktops.plugins.kotlinJvm)
    alias(desktops.plugins.composeMultiplatform)
    alias(desktops.plugins.composeCompiler)
    alias(buildLibs.plugins.buildLogic.variant.plugin)   // 本插件
}

variant {
    activeVariant.set("dev-free")

    buildType {
        release { }
        create("dev") {
            distribution {
                vendor = "Example Inc."
                linux { menuGroup = "org.example.project.hello" }
                windows { menuGroup = "Example"; upgradeUuid = "e2f1..." }
            }
            buildConfigs {
                packageName = "com.github.knightwood"
                className = "BuildConfig"

                // Kotlin JVM 只有一个 main 源集，且没有 expect/actual 语义，
                // 因此不要设置 expect（保持默认 null）
                create("main") {
                    this + intField("AGE", 18)
                    this + booleanField("IS_DEBUG", false)
                    this + stringField("GENDER", null)
                }
            }
        }
    }

    flavor {
        create("free") {
            sourceSets {
                create("main") { sourceDir = "buildSrc/free/main/kt" }
            }
        }
    }
}

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
```

要点：

1. Kotlin JVM 下不要给 `create("main")` 设置 `expect`，否则会生成 `expect`/`actual` 修饰导致编译失败。
2. `syncTo(...)` 只写入"已配置"的项，未配置项保持 `nativeDistributions` 里原有的值；它与手写属性冲突时，**后执行者生效**，建议二者择一。
3. `nativeDistributions` 里必须至少有一个 `targetFormats(...)`，否则 Compose 的打包任务无输出——`targetFormats` 属于"非空即覆盖"，可在变体里配置。
4. 若 `activeVariant` 指向不存在的 buildType，会 warn 并回退到内置 `debug`；打包类任务最好保持变体名正确。

---

## 十、读取合并后的变体信息

### 10.1 `variant.parseVariant()`

在筛选源码、拼装分发参数、打印调试信息等场景下，可直接读取合并结果（返回类型 `MergedVariantExtension`）：

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| `debug` | `Property<Boolean>` | 当前 buildType 的 `debug` 标记（默认 `false`） |
| `buildTypeName` / `flavorName` | `Property<String>` / `Property<String?>` | 解析出的名称 |
| `distributionInfo` | `DistributionInfoBuilderExtension` | 合并后的桌面分发配置 |
| `customSourceSets` | `NamedDomainObjectContainer<CustomSourceSet>` | 合并后的源集目录，可 `findByName("jvmMain")` |
| `buildConfigFields` | `NamedDomainObjectContainer<BuildConfigFieldBundleBuilder>` | 合并后的字段 bundle |
| `buildConfigPackageName` / `buildConfigClassName` | `Property<String>` | 生成 BuildConfig 使用的包名/类名 |

```kotlin
val merged = variant.parseVariant()
logger.lifecycle(
    "variant=${merged.buildTypeName.get()}-${merged.flavorName.orNull} debug=${merged.debug.get()} " +
    "pkg=${merged.buildConfigPackageName.get()} vendor=${merged.distributionInfo.vendor}"
)
```

- `parseVariant()` 每次调用都会基于当前 DSL **重新合并并返回新对象**，可作为纯读取操作反复调用；但不要缓存到全局变量跨项目使用。
- 必须等 DSL 求值完成（脚本尾部、`afterEvaluate`、或 Compose/Kotlin 扩展块内）再调用。

### 10.2 `syncTo(...)`：把分发配置交给 Compose

```kotlin
import com.github.knightwood.gradle.plugin.extensions.syncTo

compose.desktop {
    application {
        nativeDistributions {
            syncTo(variant.parseVariant())   // 或 variant.parseVariant().syncTo(this)
        }
    }
}
```

- 等价写法两种：`MergedVariantExtension.syncTo(nativeDistributions)` 或直接在 `nativeDistributions { }` 内调用。
- 只同步"已配置"项；Compose 的 `fileAssociations`、`infoPlist` 是 internal 属性，插件无法合并，需手写。
- macOS 的 `signing.sign` 默认 `false`，只有为 `true` 时才会被同步（无法用 `false` 覆盖 `true`）。

### 10.3 `composeResourceDir` 的手动接入

Compose Resources 的自定义目录必须在 Compose 插件上声明，插件只负责"读出来"：

```kotlin
import org.jetbrains.compose.resources.ResourcesExtension   // 视 Compose 版本可能不同

val merged = variant.parseVariant()

compose.resources {
    merged.customSourceSets.findByName("jvmMain")?.composeResourceDir?.let { dir ->
        customDirectory(
            sourceSetName = "jvmMain",
            directoryProvider = provider { layout.projectDirectory.dir(dir) }
        )
    }
}
```

> `customDirectory(...)` 的方法名与签名随 Compose Multiplatform 版本有变化（旧版本为 `customDirectory(sourceSetName, directoryProvider)`，更早为 `customDirectory(sourceSetName, directory)`），请以所用 Compose 版本的 API 为准。

---

## 十一、细节与注意事项（容易踩的坑）

### 11.1 源集名称必须真实存在

插件只遍历项目里**已存在**的源集做匹配：

| 项目类型 | 可用源集名 |
| --- | --- |
| Kotlin JVM | `main`、`test`（变体场景通常只用 `main`） |
| KMP | `commonMain`、`jvmMain`、`jsMain`、`androidMain`、`iosMain`、自定义 `create("xxxMain")` … |

写错名称**不会报错**，只是对应配置被静默跳过——表现为"明明配了却没生效"。排查时优先打印 `kotlin.sourceSets.names` 对照。

### 11.2 KMP 的字段集合必须跨平台一致

`commonMain` 的 `expect object` 声明的是**所有平台 bundle 字段的并集**：

- 若 `jvmMain` 有 `NAME`、`jsMain` 没有 → `jsMain` 的 `actual object` 缺 `NAME` → 编译报错。
- 正确做法：每个平台 bundle 都声明同一组字段名（值可以不同），或者把平台无关的字段统一放在各平台 bundle 里。
- 同一字段不要既在 `commonMain` bundle 又在平台 bundle 里写，会生成重复属性。

### 11.3 `expect` 的使用规则

| 项目类型 | `expect` 取值 |
| --- | --- |
| Kotlin JVM | 不设置（`null`） |
| KMP 公共源集 | `true` |
| KMP 平台源集 | `false` |

错误取值会得到"纯 `object` 缺 `actual`"或"`expect` 出现在非公共源集"之类的编译错误。

### 11.4 分发配置的合并语义

- 可空属性 / `Property` / 集合：**未配置不覆盖**。
- 平台布尔开关：**只能由 `false`→`true`，不能 `true`→`false`**（`false` 与 Compose 默认值相同，被判为未配置）。
- 嵌套块（`linux`、`windows`、`macOS`、`signing`、`notarization`）递归按上述规则合并。

### 11.5 路径写法

`sourceDir` / `resourceDir` / `composeResourceDir` 都按 `project.files(...)` 解析，通常写**相对模块目录**的路径；也可以写绝对路径或使用 `layout.projectDirectory.dir(...).asFile.path`，但不建议跨模块引用路径（多模块下会与 "project isolation" 冲突）。

### 11.6 配置缓存与增量

- 任务输入只包含可序列化快照（`BuildConfigBundleInput`），因此 `--configuration-cache` 可用。
- 输出目录固定为 `build/generated/source/buildConfig`，改字段值 → 输入变化 → 任务重跑；仅切换 `activeVariant` 也会正确触发。
- 清理 `BuildConfig` 残留：`build/generated` 属于可安全删除的构建产物。

### 11.7 多模块项目

- 变体配置是**按模块**的：每个应用插件并声明 `variant { }` 的模块各自解析 `activeVariant`。
- 若希望所有模块统一变体，可在根工程或 `gradle.properties` 中集中维护一个变量，再在各模块读入：
  ```kotlin
  val activeVariantName: String = providers.gradleProperty("activeVariant").getOrElse("dev-free")
  variant { activeVariant.set(activeVariantName) }
  ```
  然后用 `-PactiveVariant=release-paid` 切换全项目变体。
- 共享 `BuildConfig` 建议只在一个"常量模块"生成，其它模块依赖它，避免多处生成同名类。

### 11.8 与 Android 的关系

`PluginConsts` 中虽有 `org.jetbrains.kotlin.android`、`org.jetbrains.kotlin.js` 常量，但当前实现只处理 `kotlin.jvm` 与 `kotlin.multiplatform`；AGP 仅以 `compileOnly` 引入，Android 模块请勿使用本插件（请直接用 AGP 原生的变体模型）。

### 11.9 版本对齐

- 插件内 `implementation` 了 `kotlin-gradle-plugin`（2.2.10）与 `compose-gradle-plugin`（1.8.2），会被传递到消费端构建脚本类路径。消费端 Kotlin / Compose 版本差异过大时可能出现二进制不兼容，建议主版本对齐。
- 插件自身以 JVM 17 目标编译，构建时使用的 JDK 需 ≥ 17。

### 11.10 不要让 `GenerateBuildConfigTask` 名字冲突

任务名固定为 `GenerateBuildConfigTask`；同一模块**不要重复应用本插件**（例如同时 `apply` 与 `plugins {}` 引入），否则会因任务重名报错。

---

## 十二、API 参考

### 12.1 类总览

| 类 / 接口 | 包 | 角色 |
| --- | --- | --- |
| `VariantPlugin` | `...gradle.plugin` | 插件入口，创建 `variant` 扩展并挂接 Kotlin 插件 |
| `PluginConsts` | `...gradle.plugin` | 常量：插件 ID、IDE 导入任务名（`internal`） |
| `VariantExtensions` | `...plugin.extensions` | `variant { }` 的接收者，持有 buildType/flavor 集合并解析合并 |
| `MergedVariantExtension` | `...plugin.extensions` | 合并结果模型（`parseVariant()` 返回） |
| `BuildTypeCollectionExtension` | `...plugin.extensions` | `buildType { }` 接收者 |
| `BuiltTypeBuilder` | `...plugin.extensions` | 单个 buildType，含 `debug` |
| `FlavorCollectionExtension` | `...plugin.extensions` | `flavor { }` 接收者 |
| `FlavorBuilder` | `...plugin.extensions` | 单个 flavor |
| `VariantBuilder` | `...plugin.extensions` | buildType/flavor 基类：`distribution` / `sourceSets` / `buildConfigs` |
| `DistributionInfoBuilderExtension` | `...plugin.extensions` | 桌面分发配置模型（继承 Compose `AbstractDistributions`） |
| `SourceSetCollectionExtension` | `...plugin.extensions` | `sourceSets { }` 接收者 |
| `CustomSourceSet` | `...plugin.extensions` | 单个源集的目录配置 |
| `BuildConfigFieldsBundleCollectionExtension` | `...plugin.extensions` | `buildConfigs { }` 接收者（`packageName`/`className`） |
| `BuildConfigFieldBundleBuilder` | `...plugin.extensions` | 单个源集的字段 bundle（`expect` + 字段增删） |
| `BuildConfigField` | `...plugin.extensions` | 字段模型（`name` / `value` / `cls`） |
| `BuildConfigGeneratorTask` | `...buildconfig_generator` | 生成 `BuildConfig.kt` 的任务 |
| `BuildConfigBundleInput` / `BuildConfigFieldInput` | `...buildconfig_generator` | 任务可序列化输入快照 |
| `syncTo(...)` | `...plugin.extensions` | 扩展函数，把分发配置写入 Compose `JvmApplicationDistributions` |

### 12.2 关键成员速查

**`VariantExtensions`**

| 成员 | 签名 |
| --- | --- |
| `activeVariant` | `Property<String>` |
| `buildType` | `fun buildType(action: Action<BuildTypeCollectionExtension>)` |
| `flavor` | `fun flavor(action: Action<FlavorCollectionExtension>)` |
| `parseVariant` | `fun parseVariant(): MergedVariantExtension` |

**`VariantBuilder`（buildType / flavor 共用）**

| 成员 | 签名 |
| --- | --- |
| `distribution` | `fun distribution(action: Action<DistributionInfoBuilderExtension>)` |
| `sourceSets` | `fun sourceSets(action: Action<SourceSetCollectionExtension>)` |
| `buildConfigs` | `fun buildConfigs(action: Action<BuildConfigFieldsBundleCollectionExtension>)` |

**`SourceSetCollectionExtension`**：`fun create(target: String, action: CustomSourceSet.() -> Unit)`

**`BuildConfigFieldsBundleCollectionExtension`**：`packageName`、`className`、`fun create(name: String, action: Action<BuildConfigFieldBundleBuilder>)`

**`BuildConfigGeneratorTask`**

| 输入/输出 | 类型 |
| --- | --- |
| `buildConfigPackageName` | `Property<String>`（@Input） |
| `buildConfigClassName` | `Property<String>`（@Input） |
| `targetSourceSetNames` | `SetProperty<String>`（@Input） |
| `allTargetBuildConfigFields` | `ListProperty<BuildConfigBundleInput>`（@Input） |
| `outputFolder` | `DirectoryProperty`（@OutputDirectory，默认 `build/generated/source/buildConfig`） |

**`MergedVariantExtension`**：`debug`、`buildTypeName`、`flavorName`、`distributionInfo`、`customSourceSets`、`buildConfigFields`、`buildConfigPackageName`、`buildConfigClassName`

### 12.3 容错与回退

| 场景 | 行为 |
| --- | --- |
| 未设置 `activeVariant` | 抛 `InvalidUserDataException`，提示设置格式 |
| buildType 名称不存在 | `logger.warn("buildType X not found, fallback to debug buildType")`，使用内置 `debug` |
| flavor 名称不存在 | `logger.warn("flavor X not found, only buildType configuration is applied")`，仅用 buildType |
| bundle 名称对应源集不存在 | 静默跳过（不生成文件） |
| 重复 `create` 同名 buildType / flavor | warn 并忽略后一次 |

---

## 十三、任务与生成物

### 13.1 任务

| 任务 | 来源 | 说明 |
| --- | --- | --- |
| `GenerateBuildConfigTask` | 本插件 | 生成各源集 `BuildConfig.kt`；输入为可序列化快照，兼容配置缓存 |
| `prepareKotlinIdeaImport` | Kotlin 插件 | 本插件已让其 `dependsOn` 生成任务，保证 IDE 同步后源码就绪 |
| `publishToMavenLocal` 等 | vanniktech-publish | 插件自身的发布任务（见第二节） |

常用命令：

```powershell
.\gradlew.bat :composeApp:GenerateBuildConfigTask      # 只生成 BuildConfig
.\gradlew.bat :desktopApp:GenerateBuildConfigTask --rerun-tasks   # 强制重跑
.\gradlew.bat :composeApp:build --configuration-cache  # 验证配置缓存兼容性
```

### 13.2 生成物布局

```
<module>/build/generated/source/buildConfig/
├── commonMain/kotlin/<package>/BuildConfig.kt          # expect = true（无源集后缀）
├── jvmMain/kotlin/<package>/BuildConfig.jvmMain.kt     # expect = false（文件名带源集名）
├── main/kotlin/<package>/BuildConfig.kt                # expect = null（Kotlin JVM）
└── <sourceSet>/kotlin/<package>/BuildConfig.<sourceSet>.kt
```

命名规则（`BuildConfigGeneratorTask.run()`）：

- `expect == false` → 文件名 `<className>.<sourceSetName>.kt`；
- 其它（`true` / `null`）→ `<className>.kt`。

### 13.3 生成代码的形态

| 变体配置 | 生成代码 |
| --- | --- |
| `expect = true` | `public expect object BuildConfig { public val A: Int }`（无初始化器、无 `const`） |
| `expect = false` | `public actual object BuildConfig { public actual val A: Int = 1 }` |
| 未设置 `expect` | `public object BuildConfig { public val A: Int = 1 }` |
| 值传 `null` | 类型自动可空：`public val A: String? = null` |

---

## 十四、常见问题（FAQ）

### 引入与解析

| 现象 | 原因与处理 |
| --- | --- |
| `Plugin [id: 'com.github.knightwood.gradle.plugin.VariantPlugin'] was not found` | 发布仓库与 `pluginManagement.repositories` 不匹配；或 catalog 中的版本与已发布版本不一致 |
| 找不到 `kotlinpoet` / `semver` / `kotlin-gradle-plugin` | 消费端 `pluginManagement.repositories` 缺少 `mavenCentral()`（或其镜像） |
| 改了插件源码但行为没变 | included build 场景下应即时生效；若曾用发布坐标引入，检查是否仍在解析已发布版本，需重新发布 |
| `variant { }` 提示 unresolved reference | 插件未成功应用（`plugins {}` 中 id/version 解析失败），或 IDE 尚未同步 |

### 变体解析

| 现象 | 原因与处理 |
| --- | --- |
| `activeVariant is not configured, set it like: activeVariant.set("debug")` | 未设置 `activeVariant` |
| 变体名解析出错误的 buildType/flavor | 变体名里有多个 `-`，只会按第一个 `-` 切分 |
| 配置似乎"没生效"且无报错 | buildType / flavor / 源集名称写错，触发了回退或静默跳过；用 `logger.warn` 输出排查 |

### BuildConfig

| 现象 | 原因与处理 |
| --- | --- |
| 没有生成 `BuildConfig.kt` | `create("<target>")` 名称与真实源集不一致（JVM 是 `main`，KMP 是 `commonMain`/`jvmMain`…） |
| 编译报重复属性 | 同一字段同时在 `commonMain` 与平台 bundle 定义 |
| `expect object` 没有对应的 `actual` | 某个 target 没有 `expect = false` 的 bundle（新增 target 后忘记补） |
| `actual object` 缺少属性 | 各平台 bundle 字段集合不一致（见 11.2） |
| Kotlin JVM 下 `expect`/`actual` 报错 | JVM 项目不应设置 `expect` |
| 字段值类型不对 / 语法错误 | 自定义类型走 `field<T>`，字面量取 `toString()`；字符串请用 `stringField`，字符请用 `charField` |
| IDE 里 `BuildConfig` 飘红 | 执行一次 `GenerateBuildConfigTask` 或重新 Sync Gradle |

### 源集与分发

| 现象 | 原因与处理 |
| --- | --- |
| `sourceSets { }` 配了目录但编译时不包含 | 源集名不存在；或路径写错导致目录为空（Gradle 不报错） |
| `composeResourceDir` 不生效 | 该目录不会自动加源集，需按 10.3 接入 `compose.resources` |
| `distribution { }` 无法解析 | 未应用 `org.jetbrains.compose` 插件，或 Compose 版本与插件编译期（1.8.2）差异过大 |
| `syncTo(...)` 没生效 | 未在 `nativeDistributions { }` 内调用；或对应项"未配置"（布尔项为默认值、可空项为 null） |
| flavor 的 `false` 覆盖不掉 buildType 的 `true` | 平台布尔开关只在非默认值时才被视为已配置（见 11.4） |
| 打包版本号非法（MSI） | `packageVersion` 需形如 `x.y.z` 且 major > 0 |

---

## 十五、许可

Apache License 2.0，详见仓库根目录 `LICENSE`。
