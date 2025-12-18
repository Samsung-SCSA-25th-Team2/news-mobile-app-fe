package com.example.mynewsmobileappfe.feature.news.nfc

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object HceServiceManager {
    private const val TAG = "HceServiceManager"

    fun isSending(): Boolean = LinkHceService.isSendModeOn()

    fun enableSending(context: Context, articleId: Long) {
        val appContext = context.applicationContext
        enableServiceComponent(appContext)
        LinkHceService.startSending(articleId)
        Log.d(TAG, "enableSending(articleId=$articleId)")
    }

    fun disableSending(context: Context) {
        val appContext = context.applicationContext
        LinkHceService.stopSending()
        disableServiceComponent(appContext)
        Log.d(TAG, "disableSending()")
    }

    private fun enableServiceComponent(context: Context) {
        val pm = context.packageManager
        val component = ComponentName(context, LinkHceService::class.java)

        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d(TAG, "LinkHceService component ENABLED")
    }

    private fun disableServiceComponent(context: Context) {
        val pm = context.packageManager
        val component = ComponentName(context, LinkHceService::class.java)

        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d(TAG, "LinkHceService component DISABLED")
    }
}
