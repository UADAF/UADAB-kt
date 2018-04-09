package com.uadaf.uadab.users

import org.apache.commons.collections4.bidimap.DualHashBidiMap
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*

class Classification private constructor(val name: String, private val img: String, val color: Color) {
    var codename = name.toLowerCase()
    fun getImg(size: Int = 200): String {
        return if (!img.contains(".svg")) {
            img
        } else img + size
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
        val IRRELEVANT = Classification("Irrelevant", "https://vignette3.wikia.nocookie.net/pediaofinterest/images/a/a1/S03-WhiteSquare.svg/revision/latest/scale-to-width-down/", Color.WHITE)
        val ASSET = Classification("Asset", "https://vignette1.wikia.nocookie.net/pediaofinterest/images/a/a4/S03-YellowSquare.svg/revision/latest/scale-to-width-down/", Color.YELLOW)
        val ANALOG_INTERFACE = Classification("Analog Interface", "https://vignette1.wikia.nocookie.net/pediaofinterest/images/2/2e/S03-BlackSquareYellowCorners.svg/revision/latest/scale-to-width-down/", Color.YELLOW)
        val IRRELEVANT_THREAT = Classification("Irrelevant Threat", "https://vignette3.wikia.nocookie.net/pediaofinterest/images/d/d2/S03-WhiteSquareRedCorners.svg/revision/latest/scale-to-width-down/", Color.RED)
        val RELEVANT_THREAT = Classification("Relevant Threat", "https://vignette4.wikia.nocookie.net/pediaofinterest/images/4/4c/S03-RedSquare.svg/revision/latest/scale-to-width-down/", Color.RED)
        val CATALYST = Classification("Catalyst", "https://vignette2.wikia.nocookie.net/pediaofinterest/images/2/2e/S03-BlueSquare.svg/revision/latest/scale-to-width-down/", Color.BLUE)
        val RELEVANT_ONE = Classification("Relevant-One", "https://vignette3.wikia.nocookie.net/pediaofinterest/images/a/a3/S05-BlueSquareWhiteCorners.svg/revision/latest/scale-to-width-down/", Color.BLUE)
        val UNKNOWN = Classification("Unknown", "https://cdn.discordapp.com/attachments/197699632841752576/338403812576329728/classes.png", Color.GRAY)

        //Hidden classes
        val ADMIN = Classification("Asset", "https://vignette1.wikia.nocookie.net/pediaofinterest/images/a/a4/S03-YellowSquare.svg/revision/latest/scale-to-width-down/", Color.YELLOW)
        val SYSTEM = Classification("Unknown", "https://cdn.discordapp.com/attachments/197699632841752576/338403812576329728/classes.png", Color.GRAY)

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

        fun getClassification(uname: String, strict: Boolean = false): Classification? {
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

        fun getClassificationByRole(role: String): Classification? {
            return ROLE_MAP.inverseBidiMap()[role]
        }

    }

}
