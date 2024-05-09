package com.ismartcoding.plain.ui.base.pinchzoomgrid

@JvmInline
value class PinchItemTransitions private constructor(private val value: Int) {
    fun has(other: PinchItemTransitions): Boolean {
        return value and other.value != 0
    }

    operator fun plus(other: PinchItemTransitions): PinchItemTransitions {
        val newValue = value or other.value
        return PinchItemTransitions(newValue)
    }

    operator fun minus(other: PinchItemTransitions): PinchItemTransitions {
        val newValue = value and other.value.inv()
        return PinchItemTransitions(newValue)
    }

    companion object {
        /**
         * No transitions.
         */
        val None = PinchItemTransitions(0)

        /**
         * Alpha transition for offscreen items. Has no effect on shared items now.
         */
        val Alpha = PinchItemTransitions(1 shl 0)

        /**
         * Scale transition includes both x axis and y axis.
         */
        val Scale = PinchItemTransitions(1 shl 1)

        /**
         * Translation transition includes both x axis and y axis.
         */
        val Translate = PinchItemTransitions(1 shl 2)

        /**
         * All available transitions.
         */
        val All = Alpha + Scale + Translate
    }
}
