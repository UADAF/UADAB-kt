package com.uadaf.uadab.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

val JsonElement.str: String
    get() = asString

val JsonElement.bln: Boolean
    get() = asBoolean

val JsonElement.int: Int
    get() = asInt

val JsonElement.obj: JsonObject
    get() = asJsonObject

val JsonElement.arr: JsonArray
    get() = asJsonArray

val JsonElement.flt: Float
    get() = asFloat

val JsonElement.dbl: Double
    get() = asDouble