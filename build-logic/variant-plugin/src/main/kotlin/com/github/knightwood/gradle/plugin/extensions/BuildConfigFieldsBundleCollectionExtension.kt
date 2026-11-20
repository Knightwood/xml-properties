package com.github.knightwood.gradle.plugin.extensions

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import javax.inject.Inject
import kotlin.reflect.KClass

abstract class BuildConfigFieldsBundleCollectionExtension : ExtensionAware {
    @get:Inject
    abstract val objects: ObjectFactory

    internal val container = objects.domainObjectContainer(BuildConfigFieldBundleBuilder::class.java)
    var packageName: String = "com.github.knightwood.config"
    var className: String = "BuildConfig"

    /**
     * 向container中添加一个BuildConfigBuilder实例，
     * 每个BuildConfigBuilder实例都可以追加到一个构建目标且持有一组BuildConfig.kt字段，
     * 若有多个BuildConfigBuilder实例，会根据目标构建变体分组，并将追加到相同构建目标的Builder中所有字段合并。
     * 一个构建目标中只生成一个BuildConfig.kt文件，
     * 比如commonMain中生成expect object BuildConfig，jvmMain中生成actual object BuildConfig
     *
     * @param name 构建目标名称，比如commonMain， androidMain，jvmMain等
     * @param action 配置BuildConfigBuilder实例的闭包
     * @receiver
     */
    fun create(name: String, action: Action<BuildConfigFieldBundleBuilder>) {
        container.create(name, action)
    }
}

/**
 * Creates a new config field bundle builder
 *
 * @constructor Creates a new BuildConfigFieldBundleBuilder
 * @property name target名称，例如commonMain， androidMain，jvmMain等
 */
open class BuildConfigFieldBundleBuilder(val name: String) {
    internal val fields = mutableSetOf<BuildConfigField>()

    /**
     * 如果能识别此Bundle的target是commonMain，则生成expect object BuildConfig
     * 但有时候可能存在自定义的target，无法确定将BuildConfig设置为expect object还是actual object，又或者无修饰
     *
     * isExpect 为null时生成的BuildConfig类无修饰
     * isExpect 为true时生成的BuildConfig类为expect object
     * isExpect 为false时生成的BuildConfig类为actual object
     */
    var isExpect: Boolean? = null

    operator fun plus(field: BuildConfigField) {
        fields.add(field)
    }

    operator fun minus(name: String) {
        fields.removeIf { it.name == name }
    }

    fun addAll(fields: Collection<BuildConfigField>) {
        this.fields.addAll(fields)
    }

    fun replace(target: String, newField: BuildConfigField) {
        fields.removeIf { it.name == target }
        fields.add(newField)
    }

    fun intField(name: String, value: Int?) =
        BuildConfigField.intBuildConfigField(name, value)

    fun stringField(name: String, value: String?) =
        BuildConfigField.stringBuildConfigField(name, value)

    fun booleanField(name: String, value: Boolean?) =
        BuildConfigField.booleanBuildConfigField(name, value)

    fun doubleField(name: String, value: Double?) =
        BuildConfigField.doubleBuildConfigField(name, value)

    fun floatField(name: String, value: Float?) =
        BuildConfigField.floatBuildConfigField(name, value)

    fun longField(name: String, value: Long?) =
        BuildConfigField.longBuildConfigField(name, value)

    fun shortField(name: String, value: Short?) =
        BuildConfigField.shortBuildConfigField(name, value)

    fun byteField(name: String, value: Byte?) =
        BuildConfigField.byteBuildConfigField(name, value)

    fun charField(name: String, value: Char?) =
        BuildConfigField.charBuildConfigField(name, value)

    inline fun <reified T : Any> field(name: String, value: T?): BuildConfigField {
        return BuildConfigField(name, value, T::class)
    }
}

class BuildConfigField(val name: String, val value: Any?, val cls: KClass<*>) {

    companion object {
        fun intBuildConfigField(name: String, value: Int?): BuildConfigField {
            return BuildConfigField(name, value, Int::class)
        }

        fun stringBuildConfigField(name: String, value: String?): BuildConfigField {
            return BuildConfigField(name, value, String::class)
        }

        fun booleanBuildConfigField(name: String, value: Boolean?): BuildConfigField {
            return BuildConfigField(name, value, Boolean::class)
        }

        fun doubleBuildConfigField(name: String, value: Double?): BuildConfigField {
            return BuildConfigField(name, value, Double::class)
        }

        fun floatBuildConfigField(name: String, value: Float?): BuildConfigField {
            return BuildConfigField(name, value, Float::class)
        }

        fun longBuildConfigField(name: String, value: Long?): BuildConfigField {
            return BuildConfigField(name, value, Long::class)
        }

        fun shortBuildConfigField(name: String, value: Short?): BuildConfigField {
            return BuildConfigField(name, value, Short::class)
        }

        fun byteBuildConfigField(name: String, value: Byte?): BuildConfigField {
            return BuildConfigField(name, value, Byte::class)
        }

        fun charBuildConfigField(name: String, value: Char?): BuildConfigField {
            return BuildConfigField(name, value, Char::class)
        }

    }
}
