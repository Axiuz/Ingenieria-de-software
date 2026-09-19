package com.tupastilla.alarma

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Un reinicio borra todas las alarmas del sistema. Aqui se vuelven a programar las
 * tomas pendientes de las proximas 48 horas.
 */
class ReprogramarAlarmasReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Notificaciones.crearCanal(context)
                AlarmaScheduler(context).reprogramarTodo()
            } finally {
                pendiente.finish()
            }
        }
    }
}
