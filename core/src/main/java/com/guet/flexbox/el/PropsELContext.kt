package com.guet.flexbox.el

import org.json.JSONArray
import java.lang.reflect.Method
import java.util.*

class PropsELContext(
        val data: Any?
) : ELContext() {

    private val variableMapper = StandardVariableMapper()
    private val functionMapper = StandardFunctionMapper()
    private val standardResolver = CompositeELResolver()

    init {
        val props = createPropELResolver(data)
        if (props != null) {
            standardResolver.add(props)
        }
        standardResolver.add(stream)
        standardResolver.add(staticField)
        standardResolver.add(map)
        standardResolver.add(resources)
        standardResolver.add(list)
        standardResolver.add(array)
        standardResolver.add(bean)
        standardResolver.add(jsonObject)
        standardResolver.add(jsonArray)
    }

    override fun getELResolver(): ELResolver = standardResolver

    override fun getFunctionMapper(): FunctionMapper = functionMapper

    override fun getVariableMapper(): VariableMapper = variableMapper

    private class StandardVariableMapper : VariableMapper() {

        private lateinit var vars: MutableMap<String, ValueExpression>

        override fun resolveVariable(variable: String): ValueExpression? {
            return if (!this::vars.isInitialized) {
                null
            } else vars[variable]
        }

        override fun setVariable(variable: String,
                                 expression: ValueExpression?
        ): ValueExpression? {
            if (!this::vars.isInitialized)
                vars = HashMap()
            return if (expression == null) {
                vars.remove(variable)
            } else {
                vars.put(variable, expression)
            }
        }
    }

    private class StandardFunctionMapper : FunctionMapper() {

        private val methods = HashMap<String, Method>(ELFunction.functions)

        override fun resolveFunction(
                prefix: String,
                localName: String
        ): Method? {
            val key = "$prefix:$localName"
            return methods[key]
        }

        override fun mapFunction(
                prefix: String,
                localName: String,
                method: Method?
        ) {
            val key = "$prefix:$localName"
            if (method == null) {
                methods.remove(key)
            } else {
                methods[key] = method
            }
        }
    }

    private companion object {

        private val stream = expressionFactory.streamELResolver
        private val array = ArrayELResolver(false)
        private val bean = BeanELResolver(false)
        private val map = MapELResolver(false)
        private val list = ListELResolver(false)
        private val jsonObject = JSONObjectELResolver(false)
        private val jsonArray = JSONArrayELResolver(false)
        private val staticField = StaticFieldELResolver()
        private val resources = ResourceBundleELResolver()

        private fun createPropELResolver(data: Any?): ELResolver? {
            return when {
                data is Map<*, *> && data.keys.all { it is String } -> {
                    PropsELResolver(data, map)
                }
                data is JSONArray -> {
                    PropsELResolver(data, jsonObject)
                }
                data != null -> {
                    PropsELResolver(data, bean)
                }
                else -> {
                    return null
                }
            }
        }

    }

}