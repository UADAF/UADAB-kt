package com.uadaf.uadab.command

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.PARSER
import com.uadaf.uadab.RAND
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.ASSETS
import com.uadaf.uadab.utils.*
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color
import java.awt.Color.RED
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object QuoteCommands : ICommandList {

    override val cat = AdvancedCategory("Quotes", Color(0x434BAA), "http://52.48.142.75/images/quote_.png")
    private val NFENumberExtract = Pattern.compile("For input string: \"(.*)\"")


    private val totalQuotes: Int
        get() {
            val rep = PARSER.parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                    "task" to "GET",
                    "mode" to "total"))
                    .build().inputStream, StandardCharsets.UTF_8)).obj
            if (rep["error"].bln) {
                throw RuntimeException("Something went wrong: " + rep["msg"].str)
            }
            return rep["count"].int
        }

    override fun init(): Array<Command> {
        return arrayOf(command("quote", "add ore get quote") {
            val args = args
            try {
                when {
                    args.isEmpty() -> //Random
                        sendQuoteByPos(RAND.nextInt(totalQuotes) + 1, this)

                    args.matches("^\\d+$".toRegex()) -> //Position
                        sendQuoteByPos(Integer.parseInt(args), this)

                    args.matches("^\\d+:\\d+$".toRegex()) -> { //From : To
                        val fromto = args.split(":".toRegex(), 2)
                        getQuotesFromTo(fromto[0], fromto[1], this)
                    }

                    args.matches("^\\* \\d+$".toRegex()) -> { //Count
                        getQuoteCount(Integer.parseInt(args.substring(2)), this)
                    }

                    else -> {
                        reply(RED, "Invalid args", "Args should be '(add|(*none*|i%pos%|i%from%:i%to%|\\* i%count%))'", cat.img)
                        reactWarning()
                        return@command
                    }
                }
                reactSuccess()
            } catch (e1: IOException) {
                reply("Something went wrong: ${e1.localizedMessage}")
                reactError()
            } catch (e1: NumberFormatException) {
                val m = NFENumberExtract.matcher(e1.message)
                if (m.matches()) {
                    reply("Invalid number '${m.group(1)}'")
                } else {
                    reply(e1.message)
                }
                reactError()
            }
        }.setArguments("(add|(*none*|i%pos%|i%from%:i%to%|\\* i%count%))").setChildren(
                command("add", "adds quote") {
                    val args = args.split(' ', limit = 2)
                    if (args.size != 2) {
                        reply(RED, "Invalid args", "Args should be '%author% %quote%'", cat.img)
                        reactError()
                    }
                    launch {
                        try {
                            val r = PARSER.parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                                    "task" to "ADD",
                                    "addby" to member.user.name,
                                    "author" to args[0],
                                    "quote" to args[1],
                                    "key" to UADAB.config.QUOTER_KEY))
                                    .build().inputStream)).obj
                            if (r["error"].bln) {
                                reply(RED, "Something went wrong:", r["msg"].str, cat.img)
                                reactError()
                            } else {
                                reply(cat.color, "Success", "", cat.img)
                                reactSuccess()
                            }
                        } catch (e1: IOException) {
                            reply(RED, "Something went wrong:", e1.localizedMessage, cat.img)
                            reactWarning()
                        }
                    }
                }.setAllowedClasses(ASSETS).setGuildOnly(false).setArguments("%author% %quote%").build()
        ).build())
    }

    private fun getQuoteByPos(pos: Int): JsonObject {
        return PARSER.parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                "task" to "GET",
                "mode" to "pos",
                "pos" to Integer.toString(pos)))
                .build().inputStream, StandardCharsets.UTF_8)).obj
    }

    private fun sendQuoteByPos(pos: Int, e: CommandEvent) {
        launch {
            val rep = getQuoteByPos(pos)
            sendQuote(rep, e, pos)
        }
    }

    private fun createEmbeds(quotes: Iterable<JsonObject>, e: CommandEvent) {
        paginatedEmbed {
            pattern {
                color = cat.color
                thumbnail = cat.img
            }
            sender = e::reply
            quotes.forEach {
                field {
                    name = "#${it["id"].str} ${it["author"].str}:"
                    value = it["quote"].str
                    inline = false
                }
            }
        }
    }

    private fun getQuoteCount(count: Int, e: CommandEvent) {
        launch {
            try {
                val total = totalQuotes
                val sentQuotes = mutableSetOf<Int>()
                val quotes = mutableListOf<JsonObject>()
                for (i in 0 until count) {
                    var id: Int
                    do {
                        id = RAND.nextInt(total) + 1
                    } while (sentQuotes.contains(id))
                    val rep = getQuoteByPos(id)
                    quotes.add(rep)
                    sentQuotes.add(id)
                    if (sentQuotes.size >= total) {
                        break
                    }
                }
                createEmbeds(quotes, e)
            } catch (ex: Exception) {
                UADAB.log.log(ex)
            }
        }
    }

    private fun getQuotesFromTo(from: String, to: String, e: CommandEvent) {
        launch {
            val rep = PARSER.parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                    "task" to "GET",
                    "mode" to "fromto",
                    "from" to if (from.toInt() < 1) "1" else from,
                    "to" to to))
                    .build().inputStream, StandardCharsets.UTF_8)).obj
            createEmbeds(rep["quotes"].arr.map(JsonElement::obj), e)
        }
    }

    private fun sendQuote(quote: JsonObject, e: CommandEvent, id: Int) {
        e.reply(EmbedBuilder()
                .setColor(cat.color)
                .setThumbnail(cat.img)
                .addField("${quote["author"].str}:", quote["quote"].str, false)
                .setFooter("ID: $id", null)
                .build()
        )
    }
}
