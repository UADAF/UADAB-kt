package com.uadaf.uadab.users

import com.gt22.randomutils.Instances

class SSN internal constructor(val intVal: Int) {

    fun getSSNString(redacted: Boolean): String {
        val ssn = CharArray(9)
        val ssns = Integer.toString(this.intVal)
        val zeros = 9 - ssns.length
        for (i in 0 until zeros) {
            ssn[i] = '0'
        }
        ssns.toCharArray(ssn, zeros, 0, ssns.length)
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
            val ssnc = CharArray(9)
            for (i in ssnc.indices) {
                ssnc[i] = ('0'.toInt() + Instances.getRand().nextInt(10)).toChar()
            }
            var ssn = 0
            for (i in ssnc.indices) {
                ssn += Character.digit(ssnc[i], 10) * Math.pow(10.0, (8 - i).toDouble()).toInt()
            }
            return SSN(ssn)
        }
    }
}
