package com.uadaf.uadab.utils.getters

import com.uadaf.uadab.UADAB
import com.uadaf.uadab.users.UADABUser
import com.uadaf.uadab.users.Users
import net.dv8tion.jda.core.entities.Member
import java.util.stream.Collectors

object Getters {

    private val mention = Regex("<?@!?(\\d+)+>?")
    private val ssn = Regex("\\d{3}-\\d{2}-\\d{4}")
    fun getUser(from: String): Wrapper<UADABUser> {
        val name = extractMention(from)
        val user: UADABUser? = Users[name] ?: bySSN(name) ?: byId(name)
        return user?.let { Wrapper(it) } ?: byName(name)?.let { Wrapper(it) } ?: Wrapper()
    }

    private fun extractMention(from: String): String {
        val matcher = mention.find(from)
        return if (matcher != null) matcher.groupValues[1] else from
    }

    private fun bySSN(from: String): UADABUser? = if (from.matches(ssn)) {
        var ssn = 0
        ssn += Integer.parseInt(from.substring(0, 3)) * 1000000
        ssn += Integer.parseInt(from.substring(4, 6)) * 10000
        ssn += Integer.parseInt(from.substring(7))
        Users[ssn]
    } else null

    private fun byId(from: String) = if (from.matches("\\d+".toRegex())) {
        Users[UADAB.bot.getUserById(from)]
    } else null

    private fun byName(from: String): List<UADABUser>? {
        var users = UADAB.bot.getUsersByName(from, true)
        if (users.isEmpty()) {
            users = UADAB.bot.guilds.parallelStream()
                    .flatMap { g -> g.getMembersByEffectiveName(from, true).parallelStream() }.map(Member::getUser)
                    .distinct()
                    .collect(Collectors.toList())
        }
        return users?.map(Users::get)
    }

}
