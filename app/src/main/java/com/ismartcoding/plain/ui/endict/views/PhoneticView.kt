package com.ismartcoding.plain.ui.endict.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.databinding.ViewPhoneticBinding
import com.ismartcoding.plain.features.PlayAudioEvent
import com.ismartcoding.plain.features.PlayAudioResultEvent
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.extensions.setSelectableTextClickable
import com.ismartcoding.plain.ui.views.CustomViewBase

class PhoneticView(context: Context, attrs: AttributeSet?) : CustomViewBase(context, attrs) {
    private val binding = ViewPhoneticBinding.inflate(LayoutInflater.from(context), this, true)

    fun initView(
        lifecycle: Lifecycle,
        word: String,
        country: String,
        phonetic: String,
    ) {
        registerLifecycleOwner(lifecycle)
        binding.country.run {
            if (country.isEmpty()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = country.uppercase()
            }
        }
        binding.symbol.text = "/$phonetic/"
        binding.play.run {
            if (country.isEmpty()) {
                visibility = View.GONE
            } else {
                voiceDefault()
                binding.container.setSafeClick {
                    playSound(country, word)
                }
                binding.symbol.setSelectableTextClickable {
                    binding.container.performClick()
                }
            }
        }
        events.add(
            receiveEventHandler<PlayAudioResultEvent> { event ->
                if (event.uri.toString() == getUri(country, word)) {
                    binding.play.voiceDefault()
                }
            },
        )
    }

    private fun getUri(
        country: String,
        word: String,
    ): String {
        val fileUrl = BoxApi.getBoxFileUrl()
        if (fileUrl.isEmpty()) {
            return ""
        }
        val fileName = word.replace(" ", "-").replace(".", "-").trimEnd('-').lowercase()
        return "$fileUrl/endict/audio/$country/$fileName.mp3"
    }

    private fun playSound(
        country: String,
        word: String,
    ) {
        val fileUrl = getUri(country, word)
        if (fileUrl.isEmpty()) {
            return
        }
        binding.play.voicePlay()
        sendEvent(PlayAudioEvent(Uri.parse(fileUrl)))
    }
}
