package com.tupastilla.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtClaimsTest {

    private val ahora = 1_800_000_000_000L
    private val ahoraSeg = ahora / 1000

    @Test
    fun `lee sujeto, correo, rol y expiracion`() {
        val claims = JwtClaims.leer(Tokens.jwt(ahoraSeg + 300, rol = "SUPERVISADO"))
        assertEquals(JwtClaims("u1", "ana@correo.mx", "SUPERVISADO", ahoraSeg + 300), claims)
    }

    @Test
    fun `devuelve null con un numero de partes distinto de tres`() {
        assertNull(JwtClaims.leer("a.b"))
        assertNull(JwtClaims.leer("a.b.c.d"))
    }

    @Test
    fun `devuelve null si el payload no es base64`() {
        assertNull(JwtClaims.leer("a.%%%.c"))
    }

    @Test
    fun `devuelve null si el payload no es JSON o le falta exp`() {
        val enc = Base64.getUrlEncoder().withoutPadding()
        assertNull(JwtClaims.leer("a.${enc.encodeToString("hola".toByteArray())}.c"))
        val sinExp = enc.encodeToString("""{"sub":"u1","email":"a@b.mx","role":"AUTONOMO"}""".toByteArray())
        assertNull(JwtClaims.leer("a.$sinExp.c"))
    }

    @Test
    fun `vigente segun la expiracion y el margen`() {
        assertTrue(JwtClaims.leer(Tokens.jwt(ahoraSeg + 300))!!.vigente(ahora))
        assertFalse(JwtClaims.leer(Tokens.jwt(ahoraSeg - 1))!!.vigente(ahora))
        assertFalse(JwtClaims.leer(Tokens.jwt(ahoraSeg + 20))!!.vigente(ahora))
        assertTrue(JwtClaims.leer(Tokens.jwt(ahoraSeg + 1))!!.vigente(ahora, margenMs = 0))
    }
}
