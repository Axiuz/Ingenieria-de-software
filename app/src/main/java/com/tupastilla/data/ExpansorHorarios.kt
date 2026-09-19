package com.tupastilla.data

import com.tupastilla.data.local.Horario
import com.tupastilla.data.local.Toma

/**
 * Convierte un horario ("08:00 los lunes y jueves") en tomas concretas dentro de una
 * ventana de tiempo. Se programa una alarma por toma, no una por medicina, asi que
 * esta es la pieza que decide cuantas alarmas hay.
 *
 * Sin dependencias de Android a proposito: se prueba en la JVM.
 */
object ExpansorHorarios {

    /** Los instantes en que [horario] cae dentro de [desde, hasta). */
    fun instantes(horario: Horario, desde: Long, hasta: Long): List<Long> {
        if (Calendario.minutosDe(horario.hora) < 0 || hasta <= desde) return emptyList()
        val dias = diasDe(horario.diasSemana)
        if (dias.isEmpty()) return emptyList()

        val resultado = mutableListOf<Long>()
        var dia = Calendario.inicioDelDia(desde)
        // Una vuelta de mas por si la ventana termina justo despues de medianoche.
        while (dia < hasta) {
            if (Calendario.diaDeLaSemana(dia) in dias) {
                val instante = Calendario.enElDia(dia, horario.hora)
                val dentroDeVigencia = instante >= horario.desde &&
                    (horario.hasta == null || instante <= horario.hasta)
                if (instante in desde until hasta && dentroDeVigencia) resultado.add(instante)
            }
            dia = Calendario.masDias(dia, 1)
        }
        return resultado
    }

    /** Las tomas pendientes que faltarian por crear para [horario] en esa ventana. */
    fun tomas(horario: Horario, desde: Long, hasta: Long): List<Toma> =
        instantes(horario, desde, hasta).map {
            Toma(medicinaId = horario.medicinaId, horarioId = horario.id, programadaPara = it)
        }

    /** "1,2,3" a un conjunto de Calendar.DAY_OF_WEEK. */
    fun diasDe(texto: String): Set<Int> =
        texto.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()

    /** El conjunto de dias de vuelta a texto, siempre ordenado. */
    fun textoDe(dias: Set<Int>): String = dias.sorted().joinToString(",")

    /** "Todos los días", "Lunes y jueves", "Lunes, miércoles y viernes". */
    fun descripcion(texto: String): String {
        val dias = diasDe(texto)
        if (dias.size == 7) return "Todos los días"
        if (dias.isEmpty()) return "Ningún día"
        val nombres = dias.sorted().map { Calendario.DIAS[it - 1] }
        val capitalizado = nombres.mapIndexed { i, n ->
            if (i == 0) n.replaceFirstChar { c -> c.uppercase() } else n
        }
        if (capitalizado.size == 1) return capitalizado[0]
        return capitalizado.dropLast(1).joinToString(", ") + " y " + capitalizado.last()
    }
}
