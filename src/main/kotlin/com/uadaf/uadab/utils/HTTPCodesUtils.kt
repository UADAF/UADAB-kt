package com.uadaf.uadab.utils

import com.google.gson.JsonElement
import com.uadaf.uadab.PARSER
import com.uadaf.uadab.UADAB
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import org.apache.http.HttpException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

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
        val req: HttpURLConnection = URL(codesURL).openConnection() as HttpURLConnection
        if (req.responseCode != 200) {
            throw ConnectException(req.responseMessage)
        }
        try {
            val json = PARSER.parse(req.inputStream.bufferedReader()).arr
            codesData = json.map(JsonElement::obj).filter { it["code"].asString.toIntOrNull() != null }.map {
                HTTPStatusCode(it["code"].str.toInt(), it["phrase"].str, it["description"].str.removeSurrounding("\"").capitalize())
            }.stream().collect(Collectors.toMap<HTTPStatusCode, Int, HTTPStatusCode, TreeMap<Int, HTTPStatusCode>>(
                            Function { c -> c.code },
                            Function.identity(),
                            BinaryOperator { u, _ -> throw IllegalArgumentException("Duplicate key $u") },
                            Supplier { TreeMap<Int, HTTPStatusCode>() })
                    )
            return codesData
        } catch (e: Exception) {
            UADAB.log.log(e)
            throw HttpException(e.message ?: e::class.java.simpleName)
        }
    }
}

data class HTTPStatusCode(val code: Int, val phrase: String, val description: String)