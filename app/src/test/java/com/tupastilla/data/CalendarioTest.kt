package com.tupastilla.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** Las conversiones de hora que usan el expansor y las tarjetas de toma. */
class CalendarioTest {

    @Test
    fun `minutosDe acepta las horas validas y rechaza el resto`() {
        assertEquals(0, Calendario.minutosDe("00:00"))
        assertEquals(480, Calendario.minutosDe("08:00"))
        assertEquals(1439, Calendario.minutosDe("23:59"))
        assertEquals(-1, Calendario.minutosDe("24:00"))
        assertEquals(-1, Calendario.minutosDe("08:60"))
        assertEquals(-1, Calendario.minutosDe("8"))
        assertEquals(-1, Calendario.minutosDe(""))
    }

    @Test
    fun `enElDia y hhmm son inversas`() {
        val hoy = Calendar.getInstance().timeInMillis
        assertEquals("20:30", Calendario.hhmm(Calendario.enElDia(hoy, "20:30")))
    }

    @Test
    fun `inicioDelDia cae en medianoche y finDelDia justo en la siguiente`() {
        val momento = Calendario.enElDia(System.currentTimeMillis(), "17:45")
        assertEquals("00:00", Calendario.hhmm(Calendario.inicioDelDia(momento)))
        assertEquals(Calendario.DIA_MS, Calendario.finDelDia(momento) - Calendario.inicioDelDia(momento))
    }
}
