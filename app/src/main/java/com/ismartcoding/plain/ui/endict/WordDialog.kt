package com.ismartcoding.plain.ui.endict

import android.os.Bundle
import android.view.View
import com.ismartcoding.plain.databinding.DialogWordBinding
import com.ismartcoding.plain.fragment.EndictItemFragment
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.endict.views.PhoneticView

class WordDialog(val word: EndictItemFragment) : BaseBottomSheetDialog<DialogWordBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.title = word.word
        if (word.phonetic.isNotEmpty()) {
            val phs = word.phonetic.split("|")
            phs.forEachIndexed { index, ph ->
                binding.phonetic.run {
                    val v = PhoneticView(context, null)
                    v.initView(
                        lifecycle,
                        word.word,
                        (
                            if (phs.size > 1 && index == 0) {
                                "uk"
                            } else if (phs.size > 1 && index == 1) {
                                "us"
                            } else {
                                ""
                            }
                        ),
                        ph,
                    )
                    addView(v)
                }
            }
        } else {
            binding.phonetic.visibility = View.GONE
        }

        if (word.translation.isNotEmpty()) {
            binding.translation.text = word.translation.joinToString("\n")
        }

        binding.examples.run {
            if (word.examples.isEmpty()) {
                visibility = View.GONE
            } else {
                addTextRow(word.examples.take(3).joinToString("\n\n"))
            }
        }
    }
}
