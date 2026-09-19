package com.tupastilla.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidadorCredencialesTest {

    @Test
    fun `acepta un correo con mayusculas y espacios alrededor`() {
        assertNull(ValidadorCredenciales.correo("  Ana@Correo.MX "))
    }

    @Test
    fun `rechaza correos mal formados`() {
        listOf("ana", "ana@", "@correo.mx", "ana @correo.mx", "ana@correo.m", "ana@@correo.mx", "")
            .forEach { assertEquals(it, ErrorCampo.CORREO_INVALIDO, ValidadorCredenciales.correo(it)) }
    }

    @Test
    fun `rechaza correos de mas de 254 caracteres`() {
        val largo = "a".repeat(250) + "@correo.mx"
        assertEquals(ErrorCampo.CORREO_INVALIDO, ValidadorCredenciales.correo(largo))
    }

    @Test
    fun `normaliza el correo`() {
        assertEquals("ana@correo.mx", ValidadorCredenciales.normalizarCorreo("  Ana@Correo.MX "))
    }

    @Test
    fun `acepta una contrasena de exactamente diez caracteres con letra y numero`() {
        assertNull(ValidadorCredenciales.password("abcdefghi1"))
    }

    @Test
    fun `rechaza contrasenas debiles con el motivo exacto`() {
        assertEquals(ErrorCampo.PASSWORD_CORTA, ValidadorCredenciales.password("abcdefgh1"))
        assertEquals(ErrorCampo.PASSWORD_SIN_LETRA, ValidadorCredenciales.password("1234567890"))
        assertEquals(ErrorCampo.PASSWORD_SIN_NUMERO, ValidadorCredenciales.password("abcdefghij"))
    }

    @Test
    fun `rechaza contrasenas de mas de 72 bytes aunque tengan pocos caracteres`() {
        assertEquals(ErrorCampo.PASSWORD_LARGA, ValidadorCredenciales.password("ñ".repeat(40) + "1"))
    }

    @Test
    fun `las letras con acento cuentan como letra`() {
        assertNull(ValidadorCredenciales.password("áéíóúñ1234"))
    }

    @Test
    fun `valida el nombre`() {
        assertEquals(ErrorCampo.NOMBRE_VACIO, ValidadorCredenciales.nombre("   "))
        assertNull(ValidadorCredenciales.nombre("Ana"))
    }
}
