package com.uadaf.uadab

import java.sql.SQLException

object TokenManager {



    private val CHECK_USER = DatabseManager.connection.prepareStatement("SELECT COUNT(*) as `count` FROM `tokens` WHERE `user` = ?")
    private val CHECK_TOKEN = DatabseManager.connection.prepareStatement("SELECT COUNT(*) as `count` FROM `tokens` WHERE `user` = ? AND `token` = ?")
    private val INSERT_TOKEN = DatabseManager.connection.prepareStatement("INSERT INTO `tokens` VALUES (?, ?)")
    private val DELETE_TOKEN = DatabseManager.connection.prepareStatement("DELETE FROM `tokens` WHERE `user` = ?")
    private val UPDATE_TOKEN = DatabseManager.connection.prepareStatement("UPDATE `tokens` SET `token` = ? WHERE `user` = ?")

    fun hasToken(usr: String): Boolean {
        CHECK_USER.setString(1, usr)
        val countRes = CHECK_USER.executeQuery()
        if (countRes.next()) {
            return countRes.getInt(1) != 0
        } else {
            throw SQLException("Something went wrong when retrieving token count for user")
        }
    }

    fun checkToken(usr: String, token: String): Boolean {
        with(CHECK_TOKEN) {
            setString(1, usr)
            setString(2, token)
            val res = executeQuery()
            res.next()
            return res.getInt("count") > 0
        }
    }

    fun putToken(usr: String, token: String) {
        with(INSERT_TOKEN) {
            setString(1, usr)
            setString(2, token)
            execute()
        }
    }

    fun updateToken(usr: String, token: String) {
        with(UPDATE_TOKEN) {
            setString(1, token)
            setString(2, usr)
            execute()
        }
    }

    fun deleteToken(usr: String) {
        with(DELETE_TOKEN) {
            setString(1, usr)
            execute()
        }
    }

}