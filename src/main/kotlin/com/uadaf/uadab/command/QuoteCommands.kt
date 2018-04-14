package com.uadaf.uadab.command

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.gt22.randomutils.Instances
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.uadaf.uadab.UADAB
import com.uadaf.uadab.command.base.AdvancedCategory
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.users.ASSETS
import com.uadaf.uadab.utils.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color
import java.awt.Color.RED
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object QuoteCommands : ICommandList {

    val cat = AdvancedCategory("Quotes", Color(0x434BAA), "http://52.48.142.75/images/quote_.png")
    private val NFENumberExtract = Pattern.compile("For input string: \"(.*)\"")

    override fun getCategory(): AdvancedCategory {
        return cat
    }

    private val totalQuotes: Int
        get() {
            val rep = Instances.getParser().parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                    "task" to "GET",
                    "mode" to "total"))
                    .build().inputStream, StandardCharsets.UTF_8)).obj
            if (rep["error"].bln) {
                throw RuntimeException("Something went wrong: " + rep["msg"].str)
            }
            return rep["count"].int
        }

    override fun init(): Array<Command> {
        return arrayOf(command("quote", "add ore get quote") { e ->
            val args = e.args
            try {
                when {
                    args.isEmpty() -> //Random
                        sendQuoteByPos(Instances.getRand().nextInt(totalQuotes) + 1, e)

                    args.matches("^\\d+$".toRegex()) -> //Position
                        sendQuoteByPos(Integer.parseInt(args), e)

                    args.matches("^\\d+:\\d+$".toRegex()) -> { //From : To
                        val fromto = args.split(":".toRegex(), 2)
                        getQuotesFromTo(fromto[0], fromto[1], e)
                    }

                    args.matches("^\\* \\d+$".toRegex()) -> { //Count
                        getQuoteCount(Integer.parseInt(args.substring(2)), e)
                    }

                    else -> {
                        reply(e, RED, "Invalid args", "Args should be '(add|(*none*|i%pos%|i%from%:i%to%|\\* i%count%))'", cat.img)
                        e.reactWarning()
                        return@command
                    }
                }
                e.reactSuccess()
            } catch (e1: IOException) {
                e.reply("Something went wrong: ${e1.localizedMessage}")
                e.reactError()
            } catch (e1: NumberFormatException) {
                val m = NFENumberExtract.matcher(e1.message)
                if (m.matches()) {
                    e.reply("Invalid number '${m.group(1)}'")
                } else {
                    e.reply(e1.message)
                }
                e.reactError()
            }
        }.setArguments("(add|(*none*|i%pos%|i%from%:i%to%|\\* i%count%))").setChildren(
                command("add", "adds quote") { e ->
                    val args = e.args.split(' ', limit = 2)
                    if (args.size != 2) {
                        reply(e, RED, "Invalid args", "Args should be '%author% %quote%'", cat.img)
                        e.reactError()
                    }
                    Instances.getExecutor().submit {
                        try {
                            val r = Instances.getParser().parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                                    "task" to "ADD",
                                    "addby" to e.member.user.name,
                                    "author" to args[0],
                                    "quote" to args[1],
                                    "key" to UADAB.config.QUOTER_KEY))
                                    .build().inputStream)).obj
                            if (r["error"].bln) {
                                reply(e, RED, "Something went wrong:", r["msg"].str, cat.img)
                                e.reactError()
                            } else {
                                reply(e, cat.color, "Success", "", cat.img)
                                e.reactSuccess()
                            }
                        } catch (e1: IOException) {
                            reply(e, RED, "Something went wrong:", e1.localizedMessage, cat.img)
                            e.reactWarning()
                        }
                    }
                }.setAllowedClasses(ASSETS).setGuildOnly(false).setArguments("%author% %quote%").build()
        ).build())
    }

    private fun getQuoteByPos(pos: Int): JsonObject {
        return Instances.getParser().parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                "task" to "GET",
                "mode" to "pos",
                "pos" to Integer.toString(pos)))
                .build().inputStream, StandardCharsets.UTF_8)).obj
    }

    private fun sendQuoteByPos(pos: Int, e: CommandEvent) {
        Instances.getExecutor().submit {
            val rep = getQuoteByPos(pos)
            sendQuote(rep, e, pos)
        }
    }

    private fun createEmbeds(quotes: Iterable<JsonObject>): List<MessageEmbed> {
        val ret = mutableListOf<MessageEmbed>()
        var fieldCount = 0
        var embed = EmbedBuilder()
                .setColor(cat.color)
                .setThumbnail(cat.img)
        for (quote in quotes) {
            embed.addField("#${quote["id"].str} ${quote["author"].str}:", quote["quote"].str, false)
            if (++fieldCount >= 25) {
                ret.add(embed.build())
                fieldCount = 0
                embed = EmbedBuilder()
                        .setColor(cat.color)
                        .setThumbnail(cat.img)
            }
        }
        ret.add(embed.build())
        return ret
    }

    private fun getQuoteCount(count: Int, e: CommandEvent) {
        Instances.getExecutor().submit {
            try {
                val total = totalQuotes
                val sentQuotes = mutableSetOf<Int>()
                val quotes = mutableListOf<JsonObject>()
                for (i in 0 until count) {
                    var id = Instances.getRand().nextInt(total) + 1
                    if (sentQuotes.contains(id)) {
                        id = Instances.getRand().nextInt(total) + 1
                    }
                    val rep = getQuoteByPos(id)
                    quotes.add(rep)
                    sentQuotes.add(id)
                    if (sentQuotes.size >= total) {
                        break
                    }
                }
                createEmbeds(quotes).forEach(e::reply)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getQuotesFromTo(from: String, to: String, e: CommandEvent) {
        Instances.getExecutor().submit {
            val rep = Instances.getParser().parse(InputStreamReader(JavaHttpRequestBuilder(UADAB.config.QUOTER_URL).params(mapOf(
                    "task" to "GET",
                    "mode" to "fromto",
                    "from" to if (from.toInt() < 1) "1" else from,
                    "to" to to))
                    .build().inputStream, StandardCharsets.UTF_8)).obj
            createEmbeds(rep["quotes"].arr.map(JsonElement::obj)).forEach(e::reply)
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
