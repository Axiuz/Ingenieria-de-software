package com.tupastilla.data

import com.tupastilla.data.local.Horario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * El expansor decide cuantas alarmas se programan, asi que sus bordes importan:
 * un fallo aqui es una toma que no suena.
 */
class ExpansorHorariosTest {

    private fun dia(ano: Int, mes: Int, diaDelMes: Int, hora: Int = 0, minuto: Int = 0): Long =
        Calendar.getInstance().apply {
            set(ano, mes, diaDelMes, hora, minuto, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `un horario diario cae una vez por dia dentro de la ventana`() {
        val horario = Horario(id = 1, medicinaId = 1, hora = "08:00")
        val desde = dia(2026, Calendar.AUGUST, 24)
        val instantes = ExpansorHorarios.instantes(horario, desde, desde + 2 * Calendario.DIA_MS)

        assertEquals(2, instantes.size)
        assertEquals("08:00", Calendario.hhmm(instantes[0]))
    }

    @Test
    fun `una hora que ya paso hoy no entra en la ventana`() {
        val horario = Horario(id = 1, medicinaId = 1, hora = "08:00")
        // La ventana arranca a las 10:00: la toma de las 08:00 de hoy ya paso.
        val desde = dia(2026, Calendar.AUGUST, 24, hora = 10)
        val instantes = ExpansorHorarios.instantes(horario, desde, desde + Calendario.DIA_MS)

        assertEquals(1, instantes.size)
        assertTrue(instantes[0] > desde)
    }

    @Test
    fun `un horario de lunes y jueves se salta los demas dias`() {
        // Lunes 24 y jueves 27 de agosto de 2026.
        val horario = Horario(id = 1, medicinaId = 1, hora = "14:00", diasSemana = "2,5")
        val desde = dia(2026, Calendar.AUGUST, 24)
        val instantes = ExpansorHorarios.instantes(horario, desde, desde + 7 * Calendario.DIA_MS)

        assertEquals(2, instantes.size)
        instantes.forEach {
            assertTrue(Calendario.diaDeLaSemana(it) in setOf(Calendar.MONDAY, Calendar.THURSDAY))
        }
    }

    @Test
    fun `fuera de la vigencia no se genera ninguna toma`() {
        val desde = dia(2026, Calendar.AUGUST, 24)
        val horario = Horario(
            id = 1, medicinaId = 1, hora = "08:00",
            desde = desde, hasta = desde + Calendario.DIA_MS
        )
        val instantes = ExpansorHorarios.instantes(horario, desde + 5 * Calendario.DIA_MS, desde + 7 * Calendario.DIA_MS)

        assertTrue(instantes.isEmpty())
    }

    @Test
    fun `una hora invalida no revienta, simplemente no genera nada`() {
        val horario = Horario(id = 1, medicinaId = 1, hora = "25:99")
        val desde = dia(2026, Calendar.AUGUST, 24)

        assertTrue(ExpansorHorarios.instantes(horario, desde, desde + Calendario.DIA_MS).isEmpty())
    }

    @Test
    fun `la descripcion de los dias se lee como la escribiria una persona`() {
        assertEquals("Todos los días", ExpansorHorarios.descripcion("1,2,3,4,5,6,7"))
        assertEquals("Lunes y jueves", ExpansorHorarios.descripcion("2,5"))
        assertEquals("Lunes, miércoles y viernes", ExpansorHorarios.descripcion("2,4,6"))
        assertEquals("Domingo", ExpansorHorarios.descripcion("1"))
    }
}
