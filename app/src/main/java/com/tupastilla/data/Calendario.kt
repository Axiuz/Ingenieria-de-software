package com.tupastilla.data

import java.util.Calendar
import java.util.Locale

/**
 * Fechas con java.util.Calendar y no con java.time: minSdk es 24 y no queremos
 * arrastrar desugaring solo por esto.
 */
object Calendario {

    fun inicioDelDia(momento: Long): Long = Calendar.getInstance().apply {
        timeInMillis = momento
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun finDelDia(momento: Long): Long = inicioDelDia(momento) + DIA_MS

    fun masDias(momento: Long, dias: Int): Long = Calendar.getInstance().apply {
        timeInMillis = momento
        add(Calendar.DAY_OF_YEAR, dias)
    }.timeInMillis

    fun diaDeLaSemana(momento: Long): Int = Calendar.getInstance().apply {
        timeInMillis = momento
    }.get(Calendar.DAY_OF_WEEK)

    /** "08:00" a partir de un instante. */
    fun hhmm(momento: Long): String = Calendar.getInstance().let {
        it.timeInMillis = momento
        String.format(Locale.US, "%02d:%02d", it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE))
    }

    /** "08:00" a minutos desde medianoche; -1 si el texto no sirve. */
    fun minutosDe(hora: String): Int {
        val partes = hora.split(":")
        if (partes.size != 2) return -1
        val h = partes[0].toIntOrNull() ?: return -1
        val m = partes[1].toIntOrNull() ?: return -1
        if (h !in 0..23 || m !in 0..59) return -1
        return h * 60 + m
    }

    /** El instante de hoy (o del dia de [dia]) a esa hora. */
    fun enElDia(dia: Long, hora: String): Long {
        val minutos = minutosDe(hora)
        if (minutos < 0) return inicioDelDia(dia)
        return inicioDelDia(dia) + minutos * 60_000L
    }

    fun mismaFecha(a: Long, b: Long): Boolean = inicioDelDia(a) == inicioDelDia(b)

    /** "Lunes 24 de agosto" */
    fun fechaLarga(momento: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = momento }
        val dia = DIAS[c.get(Calendar.DAY_OF_WEEK) - 1]
        return dia.replaceFirstChar { it.uppercase(Locale.forLanguageTag("es")) } +
            " " + c.get(Calendar.DAY_OF_MONTH) + " de " + MESES[c.get(Calendar.MONTH)]
    }

    /** "24 ago" */
    fun fechaCorta(momento: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = momento }
        return c.get(Calendar.DAY_OF_MONTH).toString() + " " + MESES[c.get(Calendar.MONTH)].take(3)
    }

    /** "AGOSTO 2026" */
    fun mesYAno(momento: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = momento }
        return MESES[c.get(Calendar.MONTH)].uppercase(Locale.forLanguageTag("es")) + " " + c.get(Calendar.YEAR)
    }

    const val DIA_MS = 24 * 60 * 60 * 1000L

    val DIAS = listOf(
        "domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"
    )

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
}
