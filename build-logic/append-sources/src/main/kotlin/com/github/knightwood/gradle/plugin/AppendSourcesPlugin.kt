package com.github.knightwood.gradle.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class AppendSourcesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
      target.extensions.create(
            IAppendSources::class.java,
            "appendSources",
            AppendSources::class.java
        )
        target.tasks.register("appendSource", AppendSourcesTask::class.java) {
            val appendSourceSettings =
                target.extensions.getByType(IAppendSources::class.java)

        }

        //只要触发 import操作，就让task执行
        // 抄自compose multiplatform 插件
        target.tasks.configureEach(object : Action<Task> {
            override fun execute(task: Task) {
                if (task.name == IDEA_IMPORT_TASK_NAME) {
                    task.dependsOn(target.tasks.withType(AppendSourcesTask::class.java))
                }
            }
        })
    }
}

abstract class AppendSourcesTask : DefaultTask() {

    @TaskAction
    fun action() {
        println("appendSourceTask")

        project.plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
            println("apply kotlin multiplatform plugin,输出文件")
            val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            val commonMain = KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
            //将源码放到 "build/generateKtCode/" 目录并追加到源集
            appendSourceSet(kotlinExtension, commonMain, dir)
        }

        project.plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
            println("apply kotlin jvm plugin,输出文件")
            val kotlinExtension =
                project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
            val main = SourceSet.MAIN_SOURCE_SET_NAME
            appendSourceSet(kotlinExtension, main, dir)
        }

    }
}

private fun appendSourceSet(
    kotlinExtension: KotlinProjectExtension,
    sourceSetName: String,
    dir: DirectorySet,
) {
    kotlinExtension.sourceSets.all(object : Action<KotlinSourceSet> {
        override fun execute(sourceSet: KotlinSourceSet) {
            if (sourceSet.name == sourceSetName) {
                sourceSet.kotlin.srcDir(dir.ktDir())
                sourceSet.resources.srcDir(dir.resDir())
            }
        }
    })
}
