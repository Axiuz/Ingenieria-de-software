package com.tupastilla.alarma

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tupastilla.data.Grafo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Las dos acciones de la notificacion. Se resuelven aqui, sin abrir la app: para
 * confirmar una toma no deberia hacer falta desbloquear el telefono.
 */
class AccionTomaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tomaId = intent.getLongExtra(AlarmaScheduler.EXTRA_TOMA_ID, -1L)
        if (tomaId < 0) return

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Notificaciones.quitar(context, tomaId)
                when (intent.action) {
                    CONFIRMAR -> {
                        Grafo.medicamentos(context).confirmar(tomaId)
                        AlarmaScheduler(context).cancelar(tomaId)
                    }
                    POSPONER -> {
                        val item = Grafo.medicamentos(context).toma(tomaId)
                        item?.let { AlarmaScheduler(context).programarRepeticion(it.toma) }
                    }
                }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val CONFIRMAR = "com.tupastilla.CONFIRMAR"
        const val POSPONER = "com.tupastilla.POSPONER"
    }
}
