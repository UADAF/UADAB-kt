package com.uadaf.uadab.extensions

import com.uadaf.uadab.UADAB
import spark.Spark

object ExtensionRegistry {

    private val extensions: MutableMap<String, IExtension> = mutableMapOf()

    fun registerExtension(e: IExtension) {
        UADAB.log.debug("Loading extension /${e.endpoint}")
        extensions[e.endpoint] = e
        Spark.webSocket("/${e.endpoint}", e)
    }

    fun init() {
        Spark.port(8080)
        registerExtension(Web)
        registerExtension(Alerter)
        registerExtension(Shutdown)
        Spark.init()
    }

    fun shutdown() {
        extensions.values.forEach(IExtension::shutdown)
    }

    fun getExtensions(): Map<String, IExtension> = extensions

}
