package com.uadaf.uadab.utils

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color
import java.time.temporal.TemporalAccessor

@DslMarker
private annotation class EmbedDsl


fun EmbedBuilder.clone(): EmbedBuilder {
    return if (isEmpty) EmbedBuilder() else EmbedBuilder(build())
}

@EmbedDsl
open class BaseEmbedCreater {

    var builder = EmbedBuilder()

    open operator fun String.unaryPlus() {
        builder.appendDescription(this)
    }

    var thumbnail: String? = null
        set(value) {
            field = value
            builder.setThumbnail(value)
        }

    var color: Color? = null
        set(value) {
            field = value
            builder.setColor(value)
        }

    var timestamp: TemporalAccessor? = null
        set(value) {
            field = value
            builder.setTimestamp(value)
        }

    var title: String? = null
        set(value) {
            field = value
            builder.setTitle(value, url)
        }

    var url: String? = null
        set(value) {
            field = value
            builder.setTitle(title, value)
        }

    open fun text(init: BaseEmbedCreater.() -> String) {
        +init()
    }

    open fun field(init: FieldBuilder.() -> Unit) {
        setElement(::FieldBuilder, init)
    }

    open fun author(init: AuthorBuilder.() -> Unit) {
        setElement(::AuthorBuilder, init)
    }

    open fun footer(init: FooterBuilder.() -> Unit) {
        setElement(::FooterBuilder, init)
    }

    private inline fun <T : ElementBuilder> setElement(eBuilder: (EmbedBuilder) -> T, init: T.() -> Unit) {
        val b = eBuilder(this.builder)
        b.init()
        b.complete()
    }

}

@EmbedDsl
interface ElementBuilder {
    fun complete()
}

class FieldBuilder(val builder: EmbedBuilder) : ElementBuilder {

    var name: String? = null
    var value: String? = null
    var inline = false


    override fun complete() {
        builder.addField(name, value, inline)
    }

}

class FooterBuilder(val builder: EmbedBuilder) : ElementBuilder {

    var text: String? = null
    var icon: String? = null

    init {
        val e = builder.build().footer
        text = e.text
        icon = e.iconUrl
    }

    override fun complete() {
        builder.setTitle(text, icon)
    }

}

class AuthorBuilder(val builder: EmbedBuilder) : ElementBuilder {
    var name: String? = null
    var url: String? = null
    var icon: String? = null

    init {
        val e = builder.build().author
        name = e.name
        url = e.url
        icon = e.iconUrl
    }

    override fun complete() {
        builder.setAuthor(name, url, icon)
    }

}

class PatternEmbedCreater : BaseEmbedCreater()

class PaginatedEmbedCreater : BaseEmbedCreater() {

    lateinit var sender: (MessageEmbed) -> Unit
    var postSendAction: PaginatedEmbedCreater.(overflow: Boolean) -> Unit = {}
    var preSendAction: PaginatedEmbedCreater.(overflow: Boolean) -> Unit = {}
    val pattern = PatternEmbedCreater()
    var pageId = 0
        private set

    fun postSend(action: PaginatedEmbedCreater.(overflow: Boolean) -> Unit) {
        this.postSendAction = action
    }

    fun preSend(action: PaginatedEmbedCreater.(overflow: Boolean) -> Unit) {
        this.preSendAction = action
    }

    fun pattern(init: PatternEmbedCreater.() -> Unit) {
        pattern.init()
        builder = pattern.builder.clone()
    }

    fun reset() {
        builder = pattern.builder.clone()
    }

    private fun send(overflow: Boolean) {
        if (!builder.isEmpty) {
            preSendAction(overflow)

            sender(builder.build())
            reset()
            pageId++

            postSendAction(overflow)
        }
    }

    fun send() {
        send(false)
    }

    fun finish() {
        if (!builder.isEmpty) {
            send()
        }
    }

    override fun String.unaryPlus() {
        if (builder.descriptionBuilder.length + length >= MessageEmbed.TEXT_MAX_LENGTH) {
            send(true)
        }
        builder.appendDescription(this) //Can't call super-extension
    }

    override fun field(init: FieldBuilder.() -> Unit) {
        if (builder.fields.size >= 24) {
            send(true)
        }
        super.field(init)
    }

}

fun embed(init: BaseEmbedCreater.() -> Unit): MessageEmbed {
    val e = BaseEmbedCreater()
    e.init()
    return e.builder.build()
}

fun paginatedEmbed(init: PaginatedEmbedCreater.() -> Unit) {
    val e = PaginatedEmbedCreater()
    e.init()
    e.finish()
}





