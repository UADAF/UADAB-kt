package com.uadaf.uadab

import java.sql.Connection
import java.sql.DriverManager

object DatabseManager {

    val connection: Connection

    init {
        val url = "jdbc:mysql://${UADAB.config.DB_HOST}:3306/uadabdb?useUnicode=yes&characterEncoding=UTF-8"
        connection = DriverManager.getConnection(url, UADAB.config.DB_LOGIN, UADAB.config.DB_PASS)
    }


}