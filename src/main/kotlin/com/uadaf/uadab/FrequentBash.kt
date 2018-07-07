package com.uadaf.uadab

import com.gt22.randomutils.Instances
import com.gt22.uadam.utils.get
import com.uadaf.uadab.utils.get
import com.uadaf.uadab.utils.obj
import com.uadaf.uadab.utils.str
import java.io.FileNotFoundException
import java.io.FileReader

object FrequentBash {

    val typeToUrl: Map<String, String> = mapOf(
            "bash" to "https://bash.im/quote/%s",
            "ith" to "https://ithappens.me/story/%s"
    )

    val fb: Map<String, String> by lazy {
        val json = try {
            Instances.getParser().parse(FileReader("bash.json")).obj
        } catch (e: FileNotFoundException) {
            return@lazy mapOf<String, String>()
        }
        val map = mutableMapOf<String, String>()
        json.entrySet().forEach { (name, data) ->
            map[name.toLowerCase()] = typeToUrl[data["type"]?.str]!!.format(data["id"]!!.str)
        }
        map.toMap()
    }
}