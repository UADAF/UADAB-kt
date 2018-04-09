package com.uadaf.uadab.users
import com.uadaf.uadab.users.Classification.Companion.ADMIN
import com.uadaf.uadab.users.Classification.Companion.ANALOG_INTERFACE
import com.uadaf.uadab.users.Classification.Companion.ASSET
import com.uadaf.uadab.users.Classification.Companion.CATALYST
import com.uadaf.uadab.users.Classification.Companion.IRRELEVANT
import com.uadaf.uadab.users.Classification.Companion.IRRELEVANT_THREAT
import com.uadaf.uadab.users.Classification.Companion.RELEVANT_ONE
import com.uadaf.uadab.users.Classification.Companion.RELEVANT_THREAT
import com.uadaf.uadab.users.Classification.Companion.SYSTEM
import com.uadaf.uadab.users.Classification.Companion.UNKNOWN

val ADMIN_ONLY = setOf(ADMIN)
val ADMIN_OR_INTERFACE = setOf(ADMIN, ANALOG_INTERFACE)
val ASSETS = setOf(ADMIN, ANALOG_INTERFACE, ASSET, CATALYST)
val NORMAL = setOf(ADMIN, SYSTEM, ASSET, ANALOG_INTERFACE, CATALYST, IRRELEVANT, RELEVANT_ONE)
val EVERYONE = setOf(ADMIN, SYSTEM, ASSET, ANALOG_INTERFACE, CATALYST, IRRELEVANT, RELEVANT_ONE,
        IRRELEVANT_THREAT, RELEVANT_THREAT, UNKNOWN)