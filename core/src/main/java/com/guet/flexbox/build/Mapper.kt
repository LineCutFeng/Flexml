package com.guet.flexbox.build

import android.graphics.Color
import com.facebook.litho.Component

internal abstract class Mapper<T : Component.Builder<*>> {

    protected abstract val mappings: Mappings<T>

    protected inline fun <reified V : Any> scopeAttr(
            name: String,
            scope: Map<String, V>,
            fallback: V,
            crossinline action: Apply<T, V>
    ) {
        mappings[name] = { c, map, display, value ->
            action(map, display, if (value.isExpr) {
                c.scope(scope) {
                    c.tryGetValue(value, fallback)
                }
            } else {
                scope[value] ?: fallback
            })
        }
    }

    protected inline fun <reified V : Enum<V>> enumAttr(
            name: String,
            scope: Map<String, V>,
            fallback: V = enumValues<V>()[0],
            crossinline action: Apply<T, V>
    ) {
        scopeAttr(name, scope, fallback, action)
    }

    protected inline fun textAttr(
            name: String,
            fallback: String = "",
            crossinline action: Apply<T, String>) {
        mappings[name] = { c, map, display, value ->
            action(map, display, c.tryGetValue(value, fallback))
        }
    }

    protected inline fun boolAttr(
            name: String,
            fallback: Boolean = false,
            crossinline action: Apply<T, Boolean>) {
        mappings[name] = { c, map, display, value ->
            action(map, display, if (value.isExpr) {
                c.tryGetValue(value, fallback)
            } else {
                try {
                    value.toBoolean()
                } catch (e: Exception) {
                    fallback
                }
            })
        }
    }

    protected inline fun <reified N : Number> numberAttr(
            name: String,
            fallback: N = 0.safeCast(),
            crossinline action: Apply<T, N>) {
        mappings[name] = { c, map, display, value ->
            action(map, display, c.tryGetValue(value, fallback))
        }
    }

    protected inline fun colorAttr(
            name: String,
            fallback: Int = Color.TRANSPARENT,
            crossinline action: Apply<T, Int>) {
        mappings[name] = { c, map, display, value ->
            action(map, display, if (value.isExpr) {
                c.tryGetColor(value, fallback)
            } else {
                try {
                    Color.parseColor(value)
                } catch (e: Exception) {
                    fallback
                }
            })
        }
    }
}