package com.ismartcoding.plain.enums

enum class QSTileServiceAction(val value: Int) {
    MSG_REGISTER_CLIENT(1),
    MSG_STATE_RUNNING(11),
    MSG_STATE_NOT_RUNNING(12),
    MSG_UNREGISTER_CLIENT(2),
    MSG_STATE_START(3),
    MSG_STATE_START_SUCCESS(31),
    MSG_STATE_START_FAILURE(32),
    MSG_STATE_STOP(4),
    MSG_STATE_STOP_SUCCESS(41),
    MSG_STATE_RESTART(5),
    MSG_MEASURE_DELAY(6),
    MSG_MEASURE_DELAY_SUCCESS(61),
    MSG_MEASURE_CONFIG(7),
    MSG_MEASURE_CONFIG_SUCCESS(71),
    MSG_MEASURE_CONFIG_CANCEL(72);

    companion object {
        fun fromValue(value: Int): QSTileServiceAction {
            return entries.first { it.value == value }
        }
    }
}