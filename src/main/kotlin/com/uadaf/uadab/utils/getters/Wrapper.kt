package com.uadaf.uadab.utils.getters

class Wrapper<T: Any> {

    private var single: T? = null
    private var multi: List<T> = emptyList()
    lateinit var state: WrapperState
        private set


    enum class WrapperState {
        SINGLE,
        MULTI,
        NONE
    }

    constructor() {
        none()
    }

    constructor(single: T?) {
        if (single == null) {
            none()
        } else {
            single(single)
        }
    }

    constructor(multi: List<T>) {
        when (multi.size) {
            0 -> {
                none()
            }
            1 -> {
                single(multi[0])
            }
            else -> {
                multi(multi)
            }
        }
    }

    fun getSingle(): T? {
        return single
    }

    fun getMulti(): List<T>? {
        return multi
    }

    private fun single(e: T) {
        single = e
        state = WrapperState.SINGLE
    }

    private fun multi(e: List<T>) {
        multi = e
        state = WrapperState.MULTI
    }

    private fun none() {
        state = WrapperState.NONE
    }


}
