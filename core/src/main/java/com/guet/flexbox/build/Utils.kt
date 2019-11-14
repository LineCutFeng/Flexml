@file:JvmName("-Utils")

package com.guet.flexbox.build

import android.content.res.Resources
import androidx.annotation.ColorInt
import com.guet.flexbox.el.ELException
import lite.beans.Introspector
import org.json.JSONObject
import java.io.*
import java.lang.reflect.Array
import java.lang.reflect.Type
import kotlin.reflect.KClass

internal inline fun <reified T : Number> T.toPx(): Int {
    return (this.toFloat() * Resources.getSystem().displayMetrics.widthPixels / 360f).toInt()
}

internal inline fun <reified T : Any> DataBinding.tryGetValue(expr: String?, fallback: T): T {
    if (expr == null) {
        return fallback
    }
    return try {
        getValue(expr)
    } catch (e: ELException) {
        fallback
    }
}

internal inline fun <T> DataBinding.scope(scope: Map<String, Any>, action: () -> T): T {
    enterScope(scope)
    try {
        return action()
    } finally {
        exitScope()
    }
}

internal inline fun <reified T : Enum<T>> DataBinding.tryGetEnum(
        expr: String?,
        scope: Map<String, T>,
        fallback: T = T::class.java.enumConstants[0]): T {
    return when {
        expr == null -> fallback
        expr.isExpr -> scope(scope) {
            tryGetValue(expr, fallback)
        }
        else -> scope[expr] ?: fallback
    }
}

internal inline fun <reified T : Any> DataBinding.requestValue(
        name: String,
        attrs: Map<String, String>
): T {
    return getValue(attrs[name] ?: error("request attr '$name'"))
}

@ColorInt
internal fun DataBinding.tryGetColor(expr: String?, @ColorInt fallback: Int): Int {
    if (expr == null) {
        return fallback
    }
    return try {
        getColor(expr)
    } catch (e: ELException) {
        fallback
    }
}

internal inline val CharSequence?.isExpr: Boolean
    get() = this != null && length > 3 && startsWith("\${") && endsWith('}')

internal inline fun <reified N : Number> Number.safeCast(): N {
    return safeCast(N::class) as N
}

private fun Number.safeCast(type: KClass<*>): Any {
    return when (type) {
        Byte::class -> this.toByte()
        Char::class -> this.toChar()
        Int::class -> this.toInt()
        Short::class -> this.toShort()
        Long::class -> this.toLong()
        Float::class -> this.toFloat()
        Double::class -> this.toDouble()
        else -> error("no match number type ${type.java.name}")
    }
}

private typealias FromJson<T> = (T, Type) -> Any

private typealias JsonSupports = Map<Class<*>, FromJson<*>>

private typealias SupportMap = HashMap<Class<*>, FromJson<*>>

internal typealias Mapping<T> = T.(DataBinding, Map<String, String>, Boolean, String) -> Unit

internal typealias Mappings<T> = HashMap<String, Mapping<T>>

internal typealias Apply<T, V> = T.(Map<String, String>, Boolean, V) -> Unit

private inline fun <reified T> SupportMap.add(noinline action: FromJson<T>) {
    this[T::class.java] = action
}

private const val GSON_NAME = "com.google.gson.Gson"

private const val GSON_METHOD_NAME = "fromJson"

private const val FAST_JSON_NAME = "com.alibaba.fastjson.JSONObject"

private const val FAST_JSON_PRAM_NAME = "com.alibaba.fastjson.parser.Feature"

private const val FAST_JSON_METHOD_NAME = "parseObject"

private fun findGsonSupport(): JsonSupports? {
    try {
        val types = SupportMap(5)
        val gsonType = Class.forName(GSON_NAME)
        val gson = gsonType.newInstance()
        val readerMethod = gsonType.getMethod(
                GSON_METHOD_NAME,
                Reader::class.java,
                Type::class.java
        )
        val stringMethod = gsonType.getMethod(
                GSON_METHOD_NAME,
                String::class.java,
                Type::class.java
        )
        val converter: FromJson<InputStream> = { data, type ->
            readerMethod.invoke(gson, InputStreamReader(data), type)
        }
        types.add<Reader> { data, type ->
            readerMethod.invoke(gson, data, type)
        }
        types.add(converter)
        types.add<ByteArray> { data, type ->
            converter(ByteArrayInputStream(data), type)
        }
        types.add<File> { data, type ->
            converter(FileInputStream(data), type)
        }
        types.add<String> { data, type ->
            stringMethod.invoke(gson, data, type)
        }
        return types
    } catch (e: Exception) {
        return null
    }
}

private fun findFastJsonSupport(): JsonSupports? {
    try {
        val types = SupportMap(5)
        val fastJson = Class.forName(FAST_JSON_NAME)
        val stringMethod = fastJson.getMethod(
                FAST_JSON_METHOD_NAME,
                Class::class.java
        )
        val isMethod = fastJson.getMethod(
                FAST_JSON_METHOD_NAME,
                Type::class.java,
                Array.newInstance(Class.forName(FAST_JSON_PRAM_NAME), 0)
                        .javaClass
        )
        val converter: FromJson<InputStream> = { data, type ->
            isMethod.invoke(null, data, type, null)
        }
        types.add<String> { data, type ->
            stringMethod.invoke(null, data, type)
        }
        types.add<ByteArray> { data, type ->
            converter(ByteArrayInputStream(data), type)
        }
        types.add<File> { data, type ->
            converter(FileInputStream(data), type)
        }
        return types
    } catch (e: Exception) {
        return null
    }
}

private val jsonMatchTypes: JsonSupports = findGsonSupport() ?: findFastJsonSupport() ?: emptyMap()

internal inline fun <reified T> fromJson(data: Any): T? {
    return jsonMatchTypes[T::class.java]?.let {
        @Suppress("UNCHECKED_CAST")
        (it as FromJson<Any>).invoke(data, T::class.java)
    } as? T
}

internal inline fun Int.hasFlags(flag: Int, action: () -> Unit) {
    if (this and flag != 0) {
        action()
    }
}

internal fun tryToMap(o: Any): Map<String, Any> {
    val javaClass = o.javaClass
    when {
        o is Map<*, *> && o.keys.all { it is String } -> {
            @Suppress("UNCHECKED_CAST")
            return o as Map<String, Any>
        }
        o is JSONObject -> {
            val map = HashMap<String, Any>()
            o.keys().forEach {
                map[it] = o[it]
            }
            return map
        }
        jsonMatchTypes.keys.any { it.isAssignableFrom(javaClass) } -> {
            return fromJson(o) ?: error("convert ${javaClass.name} " +
                    "request $GSON_NAME or $FAST_JSON_NAME")
        }
        javaClass.methods.all { it.declaringClass == Any::class.java } -> {
            return javaClass.fields.map {
                it.name to it[o]
            }.toMap()
        }
        else -> {
            return Introspector.getBeanInfo(javaClass)
                    .propertyDescriptors
                    .filter {
                        it.propertyType != Class::class.java
                    }.map {
                        it.name to it.readMethod.invoke(o)
                    }.toMap()
        }
    }
}