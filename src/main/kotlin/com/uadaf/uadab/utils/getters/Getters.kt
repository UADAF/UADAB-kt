package com.uadaf.uadab.utils.getters

import com.uadaf.uadab.UADAB
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import java.util.regex.Pattern
import java.util.stream.Collectors

object Getters {

    private val mention = Pattern.compile("<?@!?(\\d+)+>?")
    fun getUser(from: String): Wrapper<User> {
        var user: User? = null
        val matcher = mention.matcher(from)
        val name = if (matcher.matches()) matcher.group(1) else from
        if (name.matches("\\d+".toRegex())) {
            user = UADAB.bot.getUserById(name)
        }
        if (user == null) {
            var users = UADAB.bot.getUsersByName(name, true)
            if (users.isEmpty()) {
                users = UADAB.bot.guilds.parallelStream()
                        .flatMap{ g -> g.getMembersByEffectiveName(name, true).parallelStream() }.map(Member::getUser)
                        .distinct()
                        .collect(Collectors.toList())
            }
            return Wrapper(users)
        }
        return Wrapper(user)
    }

}
