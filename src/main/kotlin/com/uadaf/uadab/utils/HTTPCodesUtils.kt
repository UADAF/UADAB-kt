package com.uadaf.uadab.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.uadaf.uadab.PARSER
import com.uadaf.uadab.UADAB
import khttp.get
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.apache.http.HttpException

object HTTPCodesUtils {

    private lateinit var codesData: Map<Int, HTTPStatusCode>
    private const val codesURL = "https://raw.githubusercontent.com/for-GET/know-your-http-well/master/json/status-codes.json"

    fun getDataSet(): Deferred<Map<Int, HTTPStatusCode>> {
        return if (::codesData.isInitialized) {
            CompletableDeferred(codesData)
        } else {
            async { loadCodes() }
        }
    }

    private fun loadCodes(): Map<Int, HTTPStatusCode> {
        try {
            return PARSER.parse(get(codesURL).text).arr
                    .map(JsonElement::obj)
                    .filter { "x" !in it["code"].str }
                    .map(::HTTPStatusCode)
                    .associateBy(HTTPStatusCode::code)
        } catch (e: Exception) {
            UADAB.log.error(e)
            throw HttpException(e.message ?: e::class.java.simpleName)
        }
    }
}

data class HTTPStatusCode(val code: Int, val phrase: String, val description: String) {
    constructor(data: JsonObject) : this(data["code"].int, data["phrase"].str,
            data["description"].str.removeSurrounding("\"").capitalize())
}