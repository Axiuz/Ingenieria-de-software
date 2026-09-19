package com.tupastilla.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** El porcentaje de B9. Lo que importa son los bordes: sin datos y redondeo. */
class CumplimientoTest {

    @Test
    fun `sin tomas resueltas el porcentaje es cero y no una division por cero`() {
        assertEquals(0, Cumplimiento(confirmadas = 0, omitidas = 0, pendientesHoy = 3).porcentaje)
    }

    @Test
    fun `todas confirmadas son cien por ciento`() {
        assertEquals(100, Cumplimiento(confirmadas = 18, omitidas = 0, pendientesHoy = 2).porcentaje)
    }

    @Test
    fun `las pendientes de hoy no cuentan para el porcentaje`() {
        // 15 de 18 resueltas, mas 5 pendientes que no deben mover el resultado.
        val con = Cumplimiento(confirmadas = 15, omitidas = 3, pendientesHoy = 5)
        assertEquals(18, con.total)
        assertEquals(83, con.porcentaje)
    }

    @Test
    fun `el porcentaje se redondea, no se trunca`() {
        // 2 de 3 son 66.67: redondeado da 67, truncado daria 66.
        assertEquals(67, Cumplimiento(confirmadas = 2, omitidas = 1, pendientesHoy = 0).porcentaje)
    }
}
