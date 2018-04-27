package com.uadaf.uadab.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

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