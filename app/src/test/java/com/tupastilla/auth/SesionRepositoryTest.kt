package com.tupastilla.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SesionRepositoryTest {

    private class AlmacenEnMemoria(var sesion: Sesion? = null) : AlmacenSesion {
        var lecturas = 0
        override fun leer(): Sesion? = sesion.also { lecturas++ }
        override fun guardar(sesion: Sesion) { this.sesion = sesion }
        override fun borrar() { sesion = null }
    }

    private val ahora = 1_800_000_000_000L
    private lateinit var server: MockWebServer
    private lateinit var almacen: AlmacenEnMemoria
    private lateinit var repo: SesionRepository

    @Before
    fun arrancar() {
        server = MockWebServer()
        server.start()
        almacen = AlmacenEnMemoria()
        repo = crear(almacen)
    }

    @After
    fun apagar() {
        runCatching { server.shutdown() }
    }

    private fun crear(almacen: AlmacenSesion) =
        SesionRepository(AuthApi(server.url("/"), OkHttpClient(), Dispatchers.Unconfined), almacen) { ahora }

    private fun sesionCon(expSeg: Long) =
        Sesion(Tokens.jwt(expSeg), "refresh-viejo", UsuarioSesion("u1", "ana@correo.mx", "Ana", "CUIDADOR"))

    @Test
    fun `sin sesion no hay token`() = runTest {
        assertFalse(repo.haySesion())
        assertNull(repo.tokenVigente())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `iniciarSesion normaliza el correo y guarda la sesion`() = runTest {
        server.enqueue(MockResponse().setBody(Tokens.respuestaSesion()))
        repo.iniciarSesion(" Ana@Correo.MX ", "Segura12345")
        assertEquals("ana@correo.mx", JSONObject(server.takeRequest().body.readUtf8()).getString("email"))
        assertTrue(repo.haySesion())
        assertEquals("refresh-1", almacen.sesion?.refreshToken)
    }

    @Test
    fun `registrar recorta el nombre y guarda la sesion`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(Tokens.respuestaSesion()))
        repo.registrar("ana@correo.mx", "  Ana  ", "Segura12345", "CUIDADOR")
        assertEquals("Ana", JSONObject(server.takeRequest().body.readUtf8()).getString("name"))
        assertTrue(repo.haySesion())
    }

    @Test
    fun `lee del almacen una sola vez y luego usa la cache`() {
        val guardada = AlmacenEnMemoria(sesionCon(ahora / 1000 + 600))
        val otro = crear(guardada)
        assertTrue(otro.haySesion())
        assertEquals("refresh-viejo", otro.actual?.refreshToken)
        assertEquals(1, guardada.lecturas)
    }

    @Test
    fun `con el access vigente no llama al servidor`() = runTest {
        almacen.sesion = sesionCon(ahora / 1000 + 600)
        assertEquals(almacen.sesion?.accessToken, repo.tokenVigente())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `con el access vencido refresca y guarda la nueva sesion`() = runTest {
        almacen.sesion = sesionCon(ahora / 1000 - 10)
        server.enqueue(MockResponse().setBody(Tokens.respuestaSesion(refresh = "refresh-nuevo")))
        val token = repo.tokenVigente()
        assertEquals("refresh-viejo", JSONObject(server.takeRequest().body.readUtf8()).getString("refreshToken"))
        assertEquals(almacen.sesion?.accessToken, token)
        assertEquals("refresh-nuevo", almacen.sesion?.refreshToken)
    }

    @Test
    fun `un access ilegible tambien se refresca`() = runTest {
        almacen.sesion = sesionCon(0).copy(accessToken = "no-es-jwt")
        server.enqueue(MockResponse().setBody(Tokens.respuestaSesion()))
        val token = repo.tokenVigente()
        assertEquals(1, server.requestCount)
        assertEquals(almacen.sesion?.accessToken, token)
    }

    @Test
    fun `si el refresh ya no vale borra la sesion`() = runTest {
        almacen.sesion = sesionCon(ahora / 1000 - 10)
        server.enqueue(MockResponse().setResponseCode(401))
        assertNull(repo.tokenVigente())
        assertNull(almacen.sesion)
        assertFalse(repo.haySesion())
    }

    @Test
    fun `si el servidor falla conserva la sesion y avisa`() = runTest {
        almacen.sesion = sesionCon(ahora / 1000 - 10)
        server.enqueue(MockResponse().setResponseCode(500))
        try {
            repo.tokenVigente()
            fail("se esperaba AuthException")
        } catch (e: AuthException) {
            assertEquals(FalloAuth.SERVIDOR, e.fallo)
        }
        assertTrue(repo.haySesion())
    }

    @Test
    fun `cerrarSesion borra aunque el servidor falle`() = runTest {
        almacen.sesion = sesionCon(ahora / 1000 + 600)
        server.enqueue(MockResponse().setResponseCode(500))
        repo.cerrarSesion()
        assertNull(almacen.sesion)
        assertEquals("/api/auth/logout", server.takeRequest().path)
    }

    @Test
    fun `cerrarSesion sin sesion no llama al servidor`() = runTest {
        repo.cerrarSesion()
        assertEquals(0, server.requestCount)
    }
}
