# 前言

用于在不同构建变体下解析xml配置文件，生成kotlin源码

这个插件可以根据当前构建项目的buildType和flavor，解析匹配条件的xml配置文件，生成kotlin代码放入源集。

比如在debug和release下配置不同的请求地址，或是秘钥。

类似于BuildConfig，可以在项目源码中直接使用。

## 示例：

### xml 文件
```
<?xml version="1.0" encoding="UTF-8" ?>
<file name="AppConfig" package="com.android.config">
    <match build-type="debug" default="true" />
    <object-class name="AppConfig">
        <val name="user">"admin"</val>
        <val name="pwd">"123456"</val>
        <val name="host">"http://192.168.0.33:6767"</val>
        <val name="isDebug" type ="Boolean">true</val>

        <!--对于有泛型的类型-->
        <val name="list" type="List">
            <generics>
                <type name="String" />
            </generics>
            <code>mutableListOf()</code>
        </val>
        <!--val map:Map<String,List<Student>> = mutableMapOf()-->
        <val name="map" type="Map">
            <generics>
                <type name="String" />
                <type name="List" package="kotlin.collections">
                    <type name="Student" package="com.vines.test.app" />
                </type>
            </generics>
            <code>mutableMapOf()</code>
        </val>

    </object-class>
</file>


```
### 生成的kotlin文件
```
package com.android.config

import com.vines.test.app.Student
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

public object AppConfig {
  public val user: String = "admin"

  public val pwd: String = "123456"

  public val host: String = "http://192.168.0.33:6767"

  public val isDebug: Boolean = true

  public val list: List<String> = mutableListOf()

  public val map: Map<String, List<Student>> = mutableMapOf()
}


```

### 在项目中使用生成的代码

```
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        //使用生成的代码
        val isDebug = AppConfig.isDebug
        val user =AppConfig.user
    }
}
```


## xml文件与构建变体的匹配

在xml文件中写match元素，声明此文件可以匹配的变体条件。
如果xml文件中没有match元素，此文件将被忽略，不用于解析生成代码。

```
在buildType为release,debug1时生成kotlin代码
<match build-type="release,debug1" />

在buildType为release,debug1且flavor为master时生成kotlin代码
<match build-type="release,debug1" flavor="master" />

不论什么构建变体，这个文件都将用于生成kotlin代码
<match default="true" />

```




# 使用方式：

1. 拉取项目到本地

2. 构建并发布到本地仓库
  gradle命令： ./gradlew :build-logic:xml-properties:publish
  
   1. 发布到本地maven仓库
        在`xml-properties`下的build.gradle.kts中配置maven仓库地址
      ```
      publishing {
          publications {
              repositories {
                  mavenLocal()
              }
          }
      }
      ```
   2. 发布到本地指定仓库（任意文件夹地址）
      ```
      publishing {
          publications {
              repositories {
                  maven {
                      url = uri("../repo") //这里的文件夹位置是build-logic/repo
                  }
              }
          }
      }
      ```



3. 使用插件
例如在example项目中使用此插件
   1. 在example项目的`settings.gradle.kts`文件中配置插件的仓库地址
      ```
      pluginManagement {
          includeBuild("build-logic")
          repositories {
              mavenLocal() //如果发布到maven本地仓库使用这个
              maven("./build-logic/repo") //如果发布到了本地指定仓库地址，则需要自己指定文件夹地址
          }
      }
      ```
    2. 在example项目的`build.gradle.kts`文件中配置插件
      ```
      plugins {
          id("com.github.knightwood.gradle.plugin.xml-properties") version "0.0.1" apply false
          //或者你可以在version catlog 中声明插件后引入
          //alias(buildLibs.plugins.buildLogic.android.xml.props) apply false
      }
      ```
      3. 在example项目的app module或者其他module的`build.gradle.kts`文件中引入插件
      ```
      plugins {
          id("com.github.knightwood.gradle.plugin.xml-properties")
          //或者你可以在version catlog 中声明插件后引入
          //alias(buildLibs.plugins.buildLogic.android.xml.props)
      }
      ```
4. 插件使用配置

例如，在app module中引入了该插件
将xml文件放入rootProject下的resources目录。
build一下，即可看到在`app/build/generated/java`生成的代码文件

该插件可以进行的一些配置
在app module的`build.gradle.kts`中配置
```
xmlProps {
    enabled = true // 是否启用
    dir = "examples" // 配置文件所在目录（文件夹需要在rootProject下，不能放在项目之外的位置）
    excludeFile += "example.xml" // 排除文件
}

```      
5. xml 文件支持的内容

```
<?xml version="1.0" encoding="UTF-8" ?>
<file name="Test" package="com.example.app">
    <!--match元素只能出现一次，没有match的会被忽略-->
    <match build-type="release" default="true" flavor="master" />

    <!--可以手动引入一些类，实际上下面的解析会自动引入，不需要手动引入-->
    <import package="com.squareup.kotlinpoet">
        <import name="CodeBlock" />
        <import name="FileSpec" />
    </import>

    <import name="TypeSpec" package="com.squareup.kotlinpoet" />

    <!--默认类型是String,支持修饰符，默认是public的-->
    <val name="first" modifier="private">"http://www.bing.com"</val>
    <!--内置类型可以简单的使用一个type指定-->
    <val name="b" type="Boolean">true</val>
    <!--也可以有表达式 c : Boolean = 1<2 -->
    <val name="c" type="Boolean">1&lt;2</val>
    <!--如果不是内置类型，也没有泛型，则可以简单的用package指明类型的包名-->
    <!--nullable暂不支持-->
    <val name="second" package="com.example.entity" type="ClassRoom">
        ClassRoom()
    </val>
    <!--对于有泛型的类型-->
    <val name="list" type="List">
        <generics>
            <type name="String" />
        </generics>
    </val>
    <!--val map:Map<String,List<out Student>> = mutableMapOf()-->
    <val name="map" type="Map">
        <generics>
            <type name="String" />
            <type name="List" package="kotlin.collections.List">
                <!--type内使用type的嵌套来描述泛型参数 -->
                <!--如果是val、var这样的属性，内部会含有代码块，但对于type，内部不会有代码块-->
                <!--因此，属性使用了generics将泛型包裹，类型的泛型直接嵌套即可-->
                <!--type还支持修饰符，比如 in out等-->
                <type name="Student" modifier="out" package="com.example.entity" />
            </type>
        </generics>
        <code>mutableMapOf()</code>
    </val>

    <object-class name="MainApi">
        <val name="first" type="String">"http://www.bing.com"</val>
    </object-class>

</file>

```