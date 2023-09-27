package com.ismartcoding.plain.ui.views

import android.content.Context
import androidx.core.view.ViewCompat
import com.google.android.material.chip.Chip
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.features.TargetType
import com.ismartcoding.plain.features.rule.RuleAction
import com.ismartcoding.plain.features.rule.RuleDirection

data class ChipItem(val text: String, val value: String) {
    fun createView(context: Context): Chip {
        val chip = Chip(context)
        chip.id = ViewCompat.generateViewId()
        chip.text = text
        chip.tag = value
        chip.isCheckedIconVisible = false
        chip.isCheckable = true
        chip.chipBackgroundColor = context.getColorStateList(R.color.chip_color_state_list)
        chip.setTextColor(context.getColorStateList(R.color.chip_text_color_state_list))
        return chip
    }

    companion object {
        fun getRuleActions(): List<ChipItem> {
            val items = mutableListOf<ChipItem>()
            RuleAction.values().forEach {
                items.add(ChipItem(it.getText(), it.value))
            }
            return items
        }

        fun getRuleDirections(): List<ChipItem> {
            val items = mutableListOf<ChipItem>()
            RuleDirection.values().forEach {
                items.add(ChipItem(it.getText(), it.value))
            }
            return items
        }

        fun getPasswordTypes(): List<ChipItem> {
            val items = mutableListOf<ChipItem>()
            PasswordType.values().forEach {
                items.add(ChipItem(it.getText(), it.name))
            }
            return items
        }

        fun getRuleTargetTypes(): List<ChipItem> {
            val items = mutableListOf<ChipItem>()
            TargetType.values().forEach {
                items.add(ChipItem(it.getText(), it.value))
            }
            return items
        }

        fun getRouteTargetTypes(): List<ChipItem> {
            val items = mutableListOf<ChipItem>()
            setOf(TargetType.IP, TargetType.NET, TargetType.REMOTE_PORT, TargetType.INTERNET).forEach {
                items.add(ChipItem(it.getText(), it.value))
            }
            return items
        }
    }
}
