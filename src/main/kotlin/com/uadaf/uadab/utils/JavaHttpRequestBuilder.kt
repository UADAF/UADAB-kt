package com.uadaf.uadab.utils



import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets


class JavaHttpRequestBuilder(url: String) {
    private val url: URL = URL(url)
    private val postParams = mutableMapOf<String, String>()

    init {
        if (!this.url.protocol.startsWith("http")) {
            throw MalformedURLException("Url must be http or https")
        }
    }

    fun params(params: Map<String, String>): JavaHttpRequestBuilder {
        postParams.putAll(params)
        return this
    }

    fun addPostParam(name: String, value: String): JavaHttpRequestBuilder {
        postParams[name] = value
        return this
    }

    fun build(): HttpURLConnection {
        val ret = url.openConnection() as HttpURLConnection

        if (!postParams.isEmpty()) {
            val postData = postParams.entries.joinToString("&") { "${it.key}=${it.value}" }.toByteArray(StandardCharsets.UTF_8)
            ret.doOutput = true
            ret.requestMethod = "POST"
            ret.setRequestProperty("Content-Encoding", "UTF-8")
            ret.setRequestProperty("charset", "utf-8")
            ret.setRequestProperty("Content-Length", Integer.toString(postData.size))
            ret.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            ret.useCaches = false
            ret.outputStream.use { w -> w.write(postData) }
        }
        return ret
    }

}
