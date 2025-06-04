package com.ismartcoding.plain.services


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.QSTileServiceAction
import com.ismartcoding.plain.events.StartHttpServerEvent
import com.ismartcoding.plain.preference.WebPreference
import com.ismartcoding.plain.web.HttpServerManager
import java.lang.ref.SoftReference

class QSTileService : TileService() {
    fun setState(state: Int) {
        if (state == Tile.STATE_INACTIVE) {
            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.label = getString(R.string.app_name)
            qsTile?.icon = Icon.createWithResource(applicationContext, R.drawable.app_icon)
        } else if (state == Tile.STATE_ACTIVE) {
            qsTile?.state = Tile.STATE_ACTIVE
            qsTile?.label = getString(R.string.app_name)
            qsTile?.icon = Icon.createWithResource(applicationContext, R.drawable.app_icon)
        }

        qsTile?.updateTile()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStartListening() {
        super.onStartListening()
        setState(Tile.STATE_INACTIVE)
        mMsgReceive = ReceiveMessageHandler(this)
        if (isTPlus()) {
            registerReceiver(mMsgReceive, IntentFilter(Constants.BROADCAST_ACTION_ACTIVITY), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(mMsgReceive, IntentFilter(Constants.BROADCAST_ACTION_ACTIVITY))
        }

        sendMsg(this, Constants.BROADCAST_ACTION_SERVICE, QSTileServiceAction.MSG_REGISTER_CLIENT.value, "")
    }

    override fun onStopListening() {
        super.onStopListening()

        unregisterReceiver(mMsgReceive)
        mMsgReceive = null
    }

    override fun onClick() {
        super.onClick()
        when (qsTile.state) {
            Tile.STATE_INACTIVE -> {
                coIO {
                    WebPreference.putAsync(this@QSTileService, true)
                    sendEvent(StartHttpServerEvent())
                }
            }

            Tile.STATE_ACTIVE -> {
                coIO {
                    WebPreference.putAsync(this@QSTileService, false)
                    HttpServerManager.stopServiceAsync(this@QSTileService)
                }
            }
        }
    }

    private var mMsgReceive: BroadcastReceiver? = null

    private class ReceiveMessageHandler(context: QSTileService) : BroadcastReceiver() {
        internal var mReference: SoftReference<QSTileService> = SoftReference(context)
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val context = mReference.get()
            when (intent?.getIntExtra("key", 0)) {
                QSTileServiceAction.MSG_STATE_RUNNING.value -> {
                    context?.setState(Tile.STATE_ACTIVE)
                }

                QSTileServiceAction.MSG_STATE_NOT_RUNNING.value -> {
                    context?.setState(Tile.STATE_INACTIVE)
                }

                QSTileServiceAction.MSG_STATE_START_SUCCESS.value -> {
                    context?.setState(Tile.STATE_ACTIVE)
                }

                QSTileServiceAction.MSG_STATE_START_FAILURE.value -> {
                    context?.setState(Tile.STATE_INACTIVE)
                }

                QSTileServiceAction.MSG_STATE_STOP_SUCCESS.value -> {
                    context?.setState(Tile.STATE_INACTIVE)
                }
            }
        }
    }

    private fun sendMsg(ctx: Context, action: String, what: Int, content: String) {
        try {
            val intent = Intent()
            intent.action = action
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.putExtra("key", what)
            intent.putExtra("content", content)
            ctx.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
