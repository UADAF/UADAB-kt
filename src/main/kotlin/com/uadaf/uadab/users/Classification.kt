package com.uadaf.uadab.users

import com.uadaf.uadab.utils.Boxes
import com.uadaf.uadab.utils.EmbedUtils
import com.uadaf.uadab.utils.ImageUtils
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.collections4.bidimap.DualHashBidiMap
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*

class Classification private constructor(val name: String, val primaryColor: Color, val secondaryColor: Color = primaryColor, val color: Color = primaryColor) {
    var codename = name.toLowerCase()
    fun getBufImg(size: Int = 200): BufferedImage {
        return Boxes.getBox(size, size, primaryColor, secondaryColor)
    }

    fun asyncGetImgUrl(size: Int = 200) =
            EmbedUtils.convertImgToURL("Box:;:$primaryColor:;:$secondaryColor:;:$size") { getBufImg(size) }


    fun getImg(size: Int = 200): String {
        return runBlocking { asyncGetImgUrl(size).await() }
    }

    var role
        get() = ROLE_MAP[this]
        set(r) {
            ROLE_MAP[this] = r
        }

    override fun toString(): String {
        return codename
    }

    companion object {
        val IRRELEVANT = Classification("Irrelevant", Color.WHITE)
        val ASSET = Classification("Asset", Color.YELLOW)
        val ANALOG_INTERFACE = Classification("Analog Interface", Color.YELLOW, Color.BLACK)
        val IRRELEVANT_THREAT = Classification("Irrelevant Threat", Color.WHITE, Color.RED, Color.RED)
        val RELEVANT_THREAT = Classification("Relevant Threat", Color.RED)
        val CATALYST = Classification("Catalyst", Color.BLUE)
        val RELEVANT_ONE = Classification("Relevant-One", Color.WHITE, Color.BLUE, Color.BLUE)
        val UNKNOWN = Classification("Unknown", Color.GRAY)

        //Hidden classes
        val ADMIN = Classification("Asset", Color.YELLOW)
        val SYSTEM = Classification("Unknown", Color.GRAY)

        private val CLASS_MAP = HashMap<String, Classification>()
        private val ROLE_MAP = DualHashBidiMap<Classification, String>()
        private val UNKNOWN_IMAGE_CACHE = HashMap<Int, BufferedImage>()

        init {
            registerClass(IRRELEVANT, "[IRRELEVANT]")
            registerClass(ASSET, "[ASSET]")
            registerClass(ANALOG_INTERFACE, "[ANALOG INTERFACE]")
            registerClass(CATALYST, "[POTENTIAL ASSET]")

            registerClass(RELEVANT_ONE, "[RELEVANT-ONE]")
            CLASS_MAP["relevant one"] = RELEVANT_ONE

            registerClass(UNKNOWN, null)

            registerClass(IRRELEVANT_THREAT, "[POTENTIAL THREAT]")
            CLASS_MAP["threat"] = IRRELEVANT_THREAT

            registerClass(RELEVANT_THREAT, "[THREAT]")
            CLASS_MAP["primary threat"] = RELEVANT_THREAT
            CLASS_MAP["competing system"] = RELEVANT_THREAT

            ADMIN.codename = "admin"
            registerClass(ADMIN, null)

            SYSTEM.codename = "thesystem"
            registerClass(SYSTEM, null)
        }

        private fun registerClass(classification: Classification, role: String?) {
            CLASS_MAP[classification.codename.toLowerCase()] = classification
            if (role != null) {
                ROLE_MAP[classification] = role
            }
        }

        fun getClassification(uname: String, strict: Boolean): Classification? {
            val name = uname.toLowerCase()
            if (CLASS_MAP.containsKey(name)) {
                return CLASS_MAP[name]
            }
            for ((key, value) in CLASS_MAP) {
                if (name.contains(key)) {
                    return value
                }
            }
            return if (strict) null else IRRELEVANT
        }

        /**
         * Non-Strict classification, defaulted to irrelevant, so can't be null
         */
        fun getClassification(uname: String): Classification {
            return getClassification(uname, false)!!
        }

        fun getClassificationByRole(role: String): Classification? {
            return ROLE_MAP.inverseBidiMap()[role]
        }

    }

}
