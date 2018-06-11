package com.uadaf.uadab.utils.getters

import java.util.Optional

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

    init {
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

    fun getSingle(): Optional<T> {
        return Optional.ofNullable(single)
    }

    fun getMulti(): Optional<List<T>> {
        return Optional.of(multi)
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
