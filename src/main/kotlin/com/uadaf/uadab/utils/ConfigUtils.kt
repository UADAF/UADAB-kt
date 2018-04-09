package com.uadaf.uadab.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jooq.lambda.Unchecked

import java.io.FileReader
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Arrays

//===============================================================================================================
//				Я серьёзно советую не читать код который идёт дальше если он ещё работает
//				Чтение этого кода может закончится психической травмой от переизбытка рефлексии
//===============================================================================================================
object ConfigUtils {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class ManualConfigProperty

    @Throws(Exception::class)
    fun <T> loadConfig(configClass: Class<T>, configFilename: String, defaultConfig: JsonElement): T {
        return loadConfig(configClass, JsonParser().parse(FileReader(configFilename)).asJsonObject, defaultConfig.asJsonObject)
    }

    @Throws(Exception::class)
    fun <T> loadConfig(configClass: Class<T>, config: JsonObject, defaultConfig: JsonObject): T {
        val cfg = configClass.newInstance()
        Arrays.stream<Field>(configClass.fields).forEach(Unchecked.consumer<Field> { f ->
            if (!f.isAnnotationPresent(ManualConfigProperty::class.java)) {
                val name = f.name
                f.isAccessible = true
                val value = if (config.has(name)) config.get(name) else defaultConfig.get(name)
                if (value == null) {
                    f.set(cfg, null)
                } else {
                    f.set(cfg, convertElementToType(f.getType(), value))
                }
            }
        })
        try {
            val loadManual = configClass.getMethod("loadManual", JsonObject::class.java)
            loadManual.invoke(cfg, config)
        } catch (e: NoSuchMethodException) {
            //No manual load
        }

        return cfg
    }

    private fun convertElementToType(type: Class<*>, e: JsonElement): Any {
        if (type == String::class.java) {
            return e.asString
        } else if (type == Boolean::class.javaPrimitiveType || type == Boolean::class.java) {
            return e.asBoolean
        } else if (type == Byte::class.javaPrimitiveType || type == Byte::class.java) {
            return e.asByte
        } else if (type == Short::class.javaPrimitiveType || type == Short::class.java) {
            return e.asShort
        } else if (type == Int::class.javaPrimitiveType || type == Int::class.java) {
            return e.asInt
        } else if (type == Long::class.javaPrimitiveType || type == Long::class.java) {
            return e.asLong
        } else if (type == Double::class.javaPrimitiveType || type == Double::class.java) {
            return e.asDouble
        } else if (type == Float::class.javaPrimitiveType || type == Float::class.java) {
            return e.asFloat
        } else if (type == Char::class.javaPrimitiveType || type == Char::class.java) {
            return e.asCharacter
        } else if (type == Number::class.java) {
            return e.asNumber
        } else if (type == BigInteger::class.java) {
            return e.asBigInteger
        } else if (type == BigDecimal::class.java) {
            return e.asBigDecimal
        } else if (type.isArray) {
            val arr = e.asJsonArray
            val arrType = type.componentType
            val ret = Array.newInstance(arrType, arr.size())

            for (i in 0 until arr.size()) {
                val o = convertElementToType(arrType, arr.get(i))
                Array.set(ret, i, o)
            }
            return ret
        } else {
            throw IllegalArgumentException("Invalid type " + type.name)
        }
    }

}
