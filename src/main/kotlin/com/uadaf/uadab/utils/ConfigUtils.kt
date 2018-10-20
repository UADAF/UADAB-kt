package com.uadaf.uadab.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.FileReader
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

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
        return loadConfig(configClass, JsonParser().parse(FileReader(configFilename)).obj, defaultConfig.obj)
    }

    @Throws(Exception::class)
    fun <T> loadConfig(configClass: Class<T>, config: JsonObject, defaultConfig: JsonObject): T {
        val cfg = configClass.newInstance()
        Arrays.stream<Field>(configClass.fields).forEach { f ->
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
        }
        try {
            val loadManual = configClass.getMethod("loadManual", JsonObject::class.java)
            loadManual.invoke(cfg, config)
        } catch (e: NoSuchMethodException) {
            //No manual load
        }

        return cfg
    }

    private fun convertElementToType(type: Class<*>, e: JsonElement): Any {
        when (type) {
            String::class.java -> return e.str
            Boolean::class.javaPrimitiveType, Boolean::class.java -> return e.bln
            Byte::class.javaPrimitiveType, Byte::class.java -> return e.asByte
            Short::class.javaPrimitiveType, Short::class.java -> return e.asShort
            Int::class.javaPrimitiveType, Int::class.java -> return e.int
            Long::class.javaPrimitiveType, Long::class.java -> return e.asLong
            Double::class.javaPrimitiveType, Double::class.java -> return e.dbl
            Float::class.javaPrimitiveType, Float::class.java -> return e.flt
            Char::class.javaPrimitiveType, Char::class.java -> return e.asCharacter
            Number::class.java -> return e.asNumber
            BigInteger::class.java -> return e.asBigInteger
            BigDecimal::class.java -> return e.asBigDecimal
            else -> {
                if(type.isArray) {
                    val arr = e.arr
                    val arrType = type.componentType
                    val ret = Array.newInstance(arrType, arr.size())

                    for (i in 0 until arr.size()) {
                        val o = convertElementToType(arrType, arr.get(i))
                        Array.set(ret, i, o)
                    }
                    return ret
                }
                else {
                    throw IllegalArgumentException("Invalid type "+type.name)
                }
            }
        }
    }

}
