package com.uadaf.uadab

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.gt22.randomutils.Instances
import com.uadaf.uadab.utils.arr
import com.uadaf.uadab.utils.obj
import com.uadaf.uadab.utils.set
import com.uadaf.uadab.utils.str
import java.nio.file.Files
import java.nio.file.Paths

object SystemIntegrityProtection {
    private val sipFilePath = Paths.get("internal", "sip.json")
    lateinit var defaultNick: String
    lateinit var allowedNicks: MutableSet<String>
        private set

    fun load() {
        defaultNick = UADAB.bot.selfUser.name
        if(!Files.exists(sipFilePath)) {
            allowedNicks = mutableSetOf(defaultNick)
        } else {
            val json = Instances.getParser().parse(Files.newBufferedReader(sipFilePath)).obj
            allowedNicks = mutableSetOf()
            json["allowedNicks"].arr.forEach{ allowedNicks.add(it.str) }
        }
    }

    fun save() {
        val arr = JsonArray()
        allowedNicks.forEach(arr::add)
        val json = JsonObject()
        json["allowedNicks"] = arr
        Files.write(sipFilePath, Instances.getGson().toJson(json).toByteArray(Charsets.UTF_8))
    }

}