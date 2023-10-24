package com.ismartcoding.plain.ui.models

import android.os.CountDownTimer
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.features.box.BoxHelper
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _boxes = MutableStateFlow(listOf<DBox>())
    val boxes: StateFlow<List<DBox>> get() = _boxes.asStateFlow()
    var showWebBadge = mutableStateOf(false)
    var httpServerError = mutableStateOf("")
    var timerUpdater: TimerUpdater? = null

    inner class TimerUpdater : CountDownTimer(
        1000 * 10,
        500,
    ) {
        override fun onTick(millisUntilFinished: Long) {
            httpServerError.value = HttpServerManager.getErrorMessage()
            if (httpServerError.value.isNotEmpty()) {
                timerUpdater?.cancel()
                showWebBadge.value = true
            } else {
                showWebBadge.value = !showWebBadge.value
            }
        }

        override fun onFinish() {
            showWebBadge.value = true
            httpServerError.value = HttpServerManager.getErrorMessage()
        }
    }

    fun startTimer() {
        if (timerUpdater == null) {
            timerUpdater = TimerUpdater().apply { start() }
        }
    }

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _boxes.value = BoxHelper.getItemsAsync()
        }
    }
}
