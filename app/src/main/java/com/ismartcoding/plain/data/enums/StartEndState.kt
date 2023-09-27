package com.ismartcoding.plain.data.enums

enum class StartEndState(val value: Int) {
    START(1),
    END(2),
    ;

    fun isEnd(): Boolean {
        return (this.value and END.value) == END.value
    }

    fun isStart(): Boolean {
        return (this.value and START.value) == START.value
    }
}
