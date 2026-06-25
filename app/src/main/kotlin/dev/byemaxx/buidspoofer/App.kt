package dev.byemaxx.buidspoofer

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class App : Application(), XposedServiceHelper.OnServiceListener {

    override fun onCreate() {
        super.onCreate()
        instance = this
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        Log.i(TAG, "Connected to ${service.frameworkName} API ${service.apiVersion}")
        syncPreferences(this)
    }

    override fun onServiceDied(service: XposedService) {
        if (xposedService === service) {
            xposedService = null
        }
        Log.w(TAG, "Xposed service disconnected")
    }

    companion object {
        private const val TAG = "BuidSpoofer"

        @Volatile
        private var instance: App? = null

        @Volatile
        private var xposedService: XposedService? = null

        fun syncPreferences(context: Context? = instance) {
            val appContext = context?.applicationContext ?: return
            val service = xposedService ?: return

            try {
                val local = TemplateManager.getSharedPreferences(appContext)
                val remote = service.getRemotePreferences(TemplateManager.REMOTE_PREFS_GROUP)
                val editor = remote.edit() ?: return
                editor.clear()
                
                local.all.forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                    }
                }
                
                editor.apply()
                Log.i(TAG, "Template preferences synchronized")
            } catch (error: Throwable) {
                Log.e(TAG, "Unable to synchronize template preferences", error)
            }
        }
    }
}
