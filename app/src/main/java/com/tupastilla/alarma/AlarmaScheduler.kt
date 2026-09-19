package com.tupastilla.alarma

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tupastilla.data.Calendario
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Toma
import com.tupastilla.data.local.TuPastillaDatabase

/**
 * Una alarma por toma, no una por medicina. Al guardar una medicina se programan sus
 * proximas 48 horas; despues de un reinicio se vuelven a programar todas.
 */
class AlarmaScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * En API 24-30 basta setExactAndAllowWhileIdle. Desde API 31 hay que comprobar
     * canScheduleExactAlarms() y, si no lo tenemos, degradar a setWindow: mas vale un
     * aviso con unos minutos de holgura que ningun aviso.
     */
    fun puedeProgramarExactas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun programar(toma: Toma, cuando: Long = toma.programadaPara) {
        if (cuando <= System.currentTimeMillis()) return
        val intent = PendingIntent.getBroadcast(
            context, toma.id.toInt(),
            Intent(context, AlarmaTomaReceiver::class.java)
                .putExtra(EXTRA_TOMA_ID, toma.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (puedeProgramarExactas()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, intent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, cuando, VENTANA_HOLGURA_MS, intent)
        }
    }

    /** El recordatorio de los 10 minutos, si la toma sigue pendiente. */
    fun programarRepeticion(toma: Toma) =
        programar(toma, System.currentTimeMillis() + REPETICION_MS)

    fun cancelar(tomaId: Long) {
        PendingIntent.getBroadcast(
            context, tomaId.toInt(),
            Intent(context, AlarmaTomaReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let { alarmManager.cancel(it) }
    }

    /** Cancela la alarma de cada toma pendiente, sin limite de fecha. */
    suspend fun cancelarTodo() {
        TuPastillaDatabase.get(context).tomaDao()
            .pendientesEntre(0, Long.MAX_VALUE)
            .forEach { cancelar(it.id) }
    }

    /**
     * Rellena la ventana y programa todo lo pendiente que quede dentro. Se llama al
     * guardar una medicina, al arrancar la app y despues de un reinicio.
     */
    suspend fun reprogramarTodo(horas: Int = 48) {
        Grafo.medicamentos(context).rellenarVentana(horas)
        val ahora = System.currentTimeMillis()
        TuPastillaDatabase.get(context).tomaDao()
            .pendientesEntre(ahora, ahora + horas * 60 * 60 * 1000L)
            .forEach { programar(it) }
    }

    /** Solo para el boton de prueba de Ajustes: dispara el aviso de la proxima toma. */
    suspend fun sonarAhora(): Boolean {
        val inicio = Calendario.inicioDelDia(System.currentTimeMillis())
        val proxima = TuPastillaDatabase.get(context).tomaDao()
            .pendientesEntre(inicio, inicio + Calendario.DIA_MS)
            .firstOrNull() ?: return false
        context.sendBroadcast(
            Intent(context, AlarmaTomaReceiver::class.java).putExtra(EXTRA_TOMA_ID, proxima.id)
        )
        return true
    }

    companion object {
        const val EXTRA_TOMA_ID = "toma_id"
        const val REPETICION_MS = 10 * 60 * 1000L
        private const val VENTANA_HOLGURA_MS = 10 * 60 * 1000L
    }
}
