package com.uadaf.uadab.users

import com.uadaf.uadab.RAND

class SSN internal constructor(val intVal: Int) {

    fun getSSNString(redacted: Boolean): String {

        val ssns = Integer.toString(this.intVal)
        val zeros = 9 - ssns.length
        val ssn = CharArray(9) { i -> if (i < zeros) '0' else ssns[i - zeros]}
        return if (redacted) {
            String.format("XXX-XX-%c%c%c%c", ssn[5], ssn[6], ssn[7], ssn[8])
        } else {
            String.format("%c%c%c-%c%c-%c%c%c%c", ssn[0], ssn[1], ssn[2], ssn[3], ssn[4], ssn[5], ssn[6], ssn[7], ssn[8])
        }
    }

    override fun toString(): String {
        return getSSNString(false)
    }

    companion object {

        internal fun randomSSN(): SSN {
            return SSN(RAND.ints(9, 0, 10).reduce { i1, i2 -> i1 * 10 + i2}.asInt)
        }
    }
}
