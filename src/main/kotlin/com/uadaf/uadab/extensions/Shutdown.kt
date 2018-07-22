package com.uadaf.uadab.extensions

import com.uadaf.uadab.TokenManager
import com.uadaf.uadab.UADAB
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import java.sql.SQLException
import kotlin.system.exitProcess


@WebSocket
object Shutdown : IExtension {


    override val endpoint = "shutdown"

    private val sessions = mutableSetOf<Session>()

    @OnWebSocketConnect
    fun onConnect(s: Session) {
        s.remote.sendString("PROCEED")
        sessions.add(s)
        UADAB.log.info("Got /$endpoint connection from ${s.remoteAddress.hostName}")
    }

    @OnWebSocketMessage
    fun onMessage(s: Session, message: String) {
        val sp = message.split(":;:")
        if(sp.size != 2) {
            s.remote.sendString("Excepted 'name:;:token'")
            return
        }
        try {
            if (TokenManager.checkToken(sp[0], sp[1])) {
                s.remote.sendString("goodbye")
                UADAB.bot.shutdown()
                exitProcess(0)
            } else {
                s.remote.sendString("Invalid token")
            }
        } catch (e: SQLException) {
            s.remote.sendString("Something went wrong: ${e.message}")
        }
    }

    override fun shutdown() {
        sessions.forEach(Session::close)
    }

}