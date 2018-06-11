package com.uadaf.uadab.command.base

import com.jagrosh.jdautilities.command.Command
import java.awt.Color

data class AdvancedCategory(private val catName: String, val color: Color, val img: String?) : Command.Category(catName)