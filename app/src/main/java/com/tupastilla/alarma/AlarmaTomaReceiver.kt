package com.tupastilla.alarma

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Estado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Suena una toma: muestra el aviso y, si toca, deja programada la repeticion. */
class AlarmaTomaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tomaId = intent.getLongExtra(AlarmaScheduler.EXTRA_TOMA_ID, -1L)
        if (tomaId < 0) return

        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = Grafo.medicamentos(context).toma(tomaId)
                // Si ya la confirmo u omitio antes de que sonara, no la molestamos.
                if (item != null && item.toma.estado == Estado.PENDIENTE) {
                    Notificaciones.crearCanal(context)
                    Notificaciones.mostrar(context, tomaId, Notificaciones.avisoDeToma(context, item))

                    val perfil = Grafo.perfiles(context).actual()
                    val esRepeticion = intent.getBooleanExtra(EXTRA_REPETICION, false)
                    if (perfil?.avisoRepetir == true && !esRepeticion) {
                        AlarmaScheduler(context).programarRepeticion(item.toma)
                    }
                }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REPETICION = "es_repeticion"
    }
}
