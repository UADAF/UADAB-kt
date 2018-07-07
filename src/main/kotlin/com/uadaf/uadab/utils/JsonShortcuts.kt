package com.uadaf.uadab.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.*
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Stream
import java.util.stream.StreamSupport

inline val JsonElement.str: String
    get() = asString

inline val JsonElement.bln: Boolean
    get() = asBoolean

inline val JsonElement.int: Int
    get() = asInt

inline val JsonElement.obj: JsonObject
    get() = asJsonObject

inline val JsonElement.arr: JsonArray
    get() = asJsonArray

inline val JsonElement.flt: Float
    get() = asFloat

inline val JsonElement.dbl: Double
    get() = asDouble


operator fun JsonObject.set(key: String, value: String) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: Char) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: Boolean) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: Number) = addProperty(key, value)

operator fun JsonObject.set(key: String, value: JsonElement) = add(key, value)

operator fun JsonObject.get(vararg keys: String): JsonElement {
    var o: JsonElement = this
    keys.forEach { o = o.obj[it] }
    return o
}

operator fun JsonObject.contains(key: String): Boolean = has(key)

fun JsonArray.stream(): Stream<JsonElement> = StreamSupport.stream(Spliterators.spliterator(this.iterator(), size().toLong(), 0), false)

fun <T> JsonArray.stream(mapper: (JsonElement) -> T): Stream<T> = stream().map(mapper)

fun JsonObject.stream(): Stream<Map.Entry<String, JsonElement>> = entrySet().stream()

fun <K, V> JsonObject.stream(mapper: (String, JsonElement) -> Pair<K, V>): Stream<Pair<K, V>> = stream()
        .map {(k, v) -> mapper(k, v)}

fun <T> JsonObject.stream(mapper: (JsonElement) -> T): Stream<Pair<String, T>> = stream { k, v -> k to mapper(v) }

fun <T> JsonArray.forEach(mapper: (JsonElement) -> T, block: (T) -> Unit) {
    stream(mapper).forEach(block)
}

val jsonArrayCollector: Collector<String, JsonArray, JsonArray> = Collector.of<String, JsonArray>(
        Supplier(::JsonArray),
        BiConsumer(JsonArray::add), BinaryOperator { l, r ->
    l.addAll(r)
    l
})