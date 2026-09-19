package com.tupastilla.alarma

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tupastilla.R
import com.tupastilla.alerta.AlertaTomaActivity
import com.tupastilla.data.Calendario
import com.tupastilla.data.local.TomaConMedicina

/**
 * Canal de alta prioridad con intent de pantalla completa. Las dos acciones las
 * resuelve AccionTomaReceiver sin abrir la app: confirmar una toma no deberia
 * obligar a desbloquear el telefono.
 */
object Notificaciones {

    const val CANAL = "avisos_de_toma"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL,
            context.getString(R.string.notif_canal),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_canal_desc)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun avisoDeToma(context: Context, item: TomaConMedicina): Notification {
        val tomaId = item.toma.id
        val hora = Calendario.hhmm(item.toma.programadaPara)

        val pantallaCompleta = PendingIntent.getActivity(
            context, tomaId.toInt(),
            Intent(context, AlertaTomaActivity::class.java)
                .putExtra(AlarmaScheduler.EXTRA_TOMA_ID, tomaId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setContentTitle(context.getString(R.string.notif_titulo, item.medicina.nombre, hora))
            .setContentText(item.medicina.dosis)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setFullScreenIntent(pantallaCompleta, true)
            .setContentIntent(pantallaCompleta)
            .addAction(
                0, context.getString(R.string.a1_ya_la_tome),
                accion(context, tomaId, AccionTomaReceiver.CONFIRMAR)
            )
            .addAction(
                0, context.getString(R.string.notif_despues),
                accion(context, tomaId, AccionTomaReceiver.POSPONER)
            )
            .build()
    }

    fun mostrar(context: Context, tomaId: Long, notificacion: Notification) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(context).notify(tomaId.toInt(), notificacion)
        } catch (e: SecurityException) {
            // Sin POST_NOTIFICATIONS no hay aviso; la toma sigue en la lista de Hoy.
        }
    }

    fun quitar(context: Context, tomaId: Long) =
        NotificationManagerCompat.from(context).cancel(tomaId.toInt())

    /** requestCode distinto por accion: si no, el segundo PendingIntent pisa al primero. */
    private fun accion(context: Context, tomaId: Long, accion: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, (tomaId.toInt() * 10) + if (accion == AccionTomaReceiver.CONFIRMAR) 1 else 2,
            Intent(context, AccionTomaReceiver::class.java)
                .setAction(accion)
                .putExtra(AlarmaScheduler.EXTRA_TOMA_ID, tomaId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
