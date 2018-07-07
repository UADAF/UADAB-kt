package com.uadaf.uadab.utils

import com.google.gson.JsonElement
import com.gt22.randomutils.Instances
import kotlinx.coroutines.experimental.launch
import org.apache.http.client.methods.HttpGet

object HTTPCodesUtils {

    private var codesData = emptyList<HTTPStatusCode>()
    private const val codesURL = "https://raw.githubusercontent.com/for-GET/know-your-http-well/master/json/status-codes.json"

    fun getDataSet(onSuccess: (List<HTTPStatusCode>) -> Unit, onError: (String) -> Unit) {
        if(codesData.isNotEmpty()) {
            onSuccess(codesData)
        }else{
            loadCodes(onSuccess, onError)
        }
    }

    private fun loadCodes(onSuccess: (List<HTTPStatusCode>) -> Unit, onError: (String) -> Unit) {
        launch {
            val request = Instances.getHttpClient().execute(HttpGet(codesURL))
            if(request.statusLine.statusCode != 200) {
                onError(request.statusLine.reasonPhrase)
                return@launch
            }
            try {
                val json = Instances.getParser().parse(request.entity.content.bufferedReader()).arr
                codesData = json.map(JsonElement::obj).filter { it["code"].asString.toIntOrNull() != null }.map {
                    HTTPStatusCode(it["code"].str.toInt(), it["phrase"].str, it["description"].str.removeSurrounding("\"").capitalize())
                }.sortedBy { it.code }
                onSuccess(codesData)
            }catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: e::class.java.simpleName)
            }
        }
    }

}

data class HTTPStatusCode(val code: Int, val phrase: String, val description: String)