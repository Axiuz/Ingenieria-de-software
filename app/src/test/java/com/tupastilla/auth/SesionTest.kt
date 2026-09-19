package com.tupastilla.auth

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test

class SesionTest {

    @Test
    fun `ida y vuelta por JSON conserva todos los campos`() {
        val sesion = Sesion("acceso", "refresco", UsuarioSesion("u1", "ana@correo.mx", "Ana \"la\" Pérez", "CUIDADOR"))
        assertEquals(sesion, Sesion.deJson(sesion.aJson()))
    }

    @Test
    fun `lee la respuesta del servidor ignorando campos extra`() {
        val sesion = Sesion.deJson(Tokens.respuestaSesion(accessToken = "a.b.c"))
        assertEquals("a.b.c", sesion.accessToken)
        assertEquals(UsuarioSesion("u1", "ana@correo.mx", "Ana", "CUIDADOR"), sesion.usuario)
    }

    @Test(expected = JSONException::class)
    fun `falla si falta el usuario`() {
        Sesion.deJson("""{"accessToken":"a","refreshToken":"r"}""")
    }
}
