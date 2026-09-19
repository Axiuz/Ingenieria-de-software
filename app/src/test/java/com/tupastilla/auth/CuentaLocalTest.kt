package com.tupastilla.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CuentaLocalTest {

    private class AlmacenFalso(override var duenoId: String? = null) : CuentaLocal.Almacen

    private var borrados = 0
    private val borrar: suspend () -> Unit = { borrados++ }

    @Test
    fun `debeBorrar solo cuando hay otro dueno`() {
        assertFalse(CuentaLocal.debeBorrar(null, "a"))
        assertFalse(CuentaLocal.debeBorrar("a", "a"))
        assertTrue(CuentaLocal.debeBorrar("a", "b"))
    }

    @Test
    fun `sin dueno previo adopta los datos sin borrarlos`() = runTest {
        val almacen = AlmacenFalso()
        val cuenta = CuentaLocal(almacen)

        assertFalse(cuenta.reclamar("ana", borrar))
        assertEquals(0, borrados)
        assertEquals("ana", almacen.duenoId)
        assertEquals("ana", cuenta.duenoId)
    }

    @Test
    fun `la misma cuenta conserva sus datos`() = runTest {
        val cuenta = CuentaLocal(AlmacenFalso("ana"))

        assertFalse(cuenta.reclamar("ana", borrar))
        assertEquals(0, borrados)
    }

    @Test
    fun `otra cuenta borra una vez y se queda con el telefono`() = runTest {
        val almacen = AlmacenFalso("ana")
        val cuenta = CuentaLocal(almacen)

        assertTrue(cuenta.reclamar("luis", borrar))
        assertEquals(1, borrados)
        assertEquals("luis", almacen.duenoId)
    }

    @Test
    fun `si borrar falla el dueno no cambia y el siguiente intento vuelve a borrar`() = runTest {
        val almacen = AlmacenFalso("ana")
        val cuenta = CuentaLocal(almacen)

        try {
            cuenta.reclamar("luis") { error("disco lleno") }
            fail("se esperaba la excepcion de borrar")
        } catch (e: IllegalStateException) {
            assertEquals("disco lleno", e.message)
        }
        assertEquals("ana", almacen.duenoId)

        assertTrue(cuenta.reclamar("luis", borrar))
        assertEquals(1, borrados)
    }

    @Test
    fun `dos entradas seguidas de la cuenta nueva solo borran la primera vez`() = runTest {
        val cuenta = CuentaLocal(AlmacenFalso("ana"))

        assertTrue(cuenta.reclamar("luis", borrar))
        assertFalse(cuenta.reclamar("luis", borrar))
        assertEquals(1, borrados)
    }
}
