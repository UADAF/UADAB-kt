package com.uadaf.uadab.utils

import kotlin.reflect.KProperty

private object UNINITIALIZED

class MutableLazy<T>(var initializer: () -> T) {
    var value: Any? = UNINITIALIZED


    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if(value == UNINITIALIZED) {
            value = initializer()
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun clearValue() {
        if(value != UNINITIALIZED) {
            value = UNINITIALIZED
        }
    }

    fun isInitialized() = value != UNINITIALIZED

}