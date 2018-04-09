package com.uadaf.uadab

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.gt22.randomutils.Instances
import com.gt22.randomutils.log.SimpleLog
import com.jagrosh.jdautilities.command.CommandClient
import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.uadaf.uadab.command.ClassificationCommands
import com.uadaf.uadab.command.MusicCommands
import com.uadaf.uadab.command.QuoteCommands
import com.uadaf.uadab.command.SystemCommands
import com.uadaf.uadab.command.base.ICommandList
import com.uadaf.uadab.extensions.ExtensionRegistry
import com.uadaf.uadab.users.Users
import com.uadaf.uadab.utils.ConfigUtils
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.hooks.EventListener
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


object UADAB {

    val log: SimpleLog = SimpleLog.getLog("UADAB")
    var claimCode: UUID? = null
    lateinit var config: Config
        private set
    lateinit var bot: JDA //Initialized in UADABEventListener#onReady
        private set
    lateinit var commands: CommandClient
        private set
    var startTime: Long = 0

    private var queuedListeners: MutableList<EventListener>? = mutableListOf()

    @JvmStatic
    fun main(args: Array<String>) {
        startTime = System.currentTimeMillis()
        if (args.contains("-d") || args.contains("--debug")) log.level = SimpleLog.Level.DEBUG
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out") //Redirect slf4j-simple to System.out from System.err
        log.info("Initializing")
        config = ConfigUtils.loadConfig(Config::class.java, "config.json", JsonObject() as JsonElement)
        log.info("Loaded config, owner: ${config.OWNER}")
        log.info("Loading extensions")
        ExtensionRegistry.init()
        log.info("Extension loaded")
        buildCommands()
        log.info("Connecting...")
        buildClient()
        log.info("Connected")
    }

    fun initBot(b: JDA) {
        if(::bot.isInitialized) {
            throw IllegalStateException("Bot already initialized")
        } else {
            this.bot = b
        }
    }

    fun CommandClientBuilder.addCommands(vararg cmds: ICommandList): CommandClientBuilder {
        cmds.forEach { addCommands(*it.init()) }
        return this
    }

    private fun buildCommands() {
        commands = CommandClientBuilder()
                .setOwnerId(config.OWNER)
                .setCoOwnerIds(*config.CO_OWNERS)
                .setGame(Game.of(Game.GameType.DEFAULT, "Type 'sudo help'"))
                .setPrefix("sudo ")
                .useHelpBuilder(false)
                .addCommands(SystemCommands, ClassificationCommands, QuoteCommands, MusicCommands)
                .setEmojis("<:asset:230288765724131329>", "<:irrelevant_threat:340157734248775680>", "<:relevant_threat:340157956286840844>")
                .build()
    }

    private fun buildClient() {
        JDABuilder(AccountType.BOT)
                .setToken(config.TOKEN)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.of(Game.GameType.DEFAULT, "Loading..."))
                .addEventListener(commands, UADABEventListener)
                .addEventListener(*queuedListeners!!.toTypedArray())
                .setBulkDeleteSplittingEnabled(false)
                .buildBlocking()
        queuedListeners = null
    }

    fun addListener(l: EventListener) {
        if(queuedListeners != null) {
            queuedListeners!!.add(l)
        } else {
            bot.addEventListener(l)
        }
    }

    fun parse(file: Path): JsonElement {
        val r = Files.newBufferedReader(file)
        val ret = Instances.getParser().parse(r)
        r.close()
        return ret
    }


    fun contactAdmin(embed: MessageEmbed) {
        Users.of("Admin")?.discordUser?.openPrivateChannel()?.queue { ch -> ch.sendMessage(embed).queue() }
    }
}