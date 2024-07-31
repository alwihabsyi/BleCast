package com.blecast.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blecast.app.datastore.PreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val preferencesDataStore = PreferencesDataStore.getInstance(context)

            val serial = runBlocking {
                preferencesDataStore.getString(MainActivity.SERIAL).first()
            }

            if (serial.isNotEmpty()) {
                val serviceIntent = Intent(context, CastService::class.java).apply {
                    putExtra("SERIAL", serial)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
