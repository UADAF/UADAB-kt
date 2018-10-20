package com.uadaf.uadab

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.util.*

val PARSER = JsonParser()
val RAND = Random()
val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()