package com.tupastilla.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class AuthApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi

    @Before
    fun arrancar() {
        server = MockWebServer()
        server.start()
        api = AuthApi(server.url("/"), OkHttpClient(), Dispatchers.Unconfined)
    }

    @After
    fun apagar() {
        runCatching { server.shutdown() }
    }

    private fun responder(codigo: Int, cuerpo: String = "") =
        server.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))

    private suspend fun falloDe(accion: suspend () -> Unit): FalloAuth {
        try {
            accion()
        } catch (e: AuthException) {
            return e.fallo
        }
        fail("se esperaba AuthException")
        error("inalcanzable")
    }

    @Test
    fun `iniciarSesion manda el JSON correcto y parsea la sesion`() = runTest {
        responder(200, Tokens.respuestaSesion())
        val sesion = api.iniciarSesion("ana@correo.mx", "Segura12345")
        val peticion = server.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/api/auth/login", peticion.path)
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("ana@correo.mx", cuerpo.getString("email"))
        assertEquals("Segura12345", cuerpo.getString("password"))
        assertEquals("refresh-1", sesion.refreshToken)
        assertEquals("CUIDADOR", sesion.usuario.rol)
    }

    @Test
    fun `registrar manda nombre y rol`() = runTest {
        responder(201, Tokens.respuestaSesion())
        api.registrar("ana@correo.mx", "Ana", "Segura12345", "CUIDADOR")
        val peticion = server.takeRequest()
        assertEquals("/api/auth/register", peticion.path)
        val cuerpo = JSONObject(peticion.body.readUtf8())
        assertEquals("Ana", cuerpo.getString("name"))
        assertEquals("CUIDADOR", cuerpo.getString("role"))
    }

    @Test
    fun `refrescar y cerrar sesion usan sus rutas`() = runTest {
        responder(200, Tokens.respuestaSesion(refresh = "refresh-2"))
        responder(204)
        assertEquals("refresh-2", api.refrescar("refresh-1").refreshToken)
        api.cerrarSesion("refresh-2")
        assertEquals("/api/auth/refresh", server.takeRequest().path)
        val logout = server.takeRequest()
        assertEquals("/api/auth/logout", logout.path)
        assertEquals("refresh-2", JSONObject(logout.body.readUtf8()).getString("refreshToken"))
    }

    @Test
    fun `traduce los codigos HTTP a fallos`() = runTest {
        val casos = listOf(
            Triple(401, "login", FalloAuth.CREDENCIALES),
            Triple(401, "refresh", FalloAuth.SESION_EXPIRADA),
            Triple(409, "register", FalloAuth.CORREO_OCUPADO),
            Triple(400, "login", FalloAuth.DATOS_INVALIDOS),
            Triple(413, "login", FalloAuth.DATOS_INVALIDOS),
            Triple(429, "login", FalloAuth.DEMASIADOS_INTENTOS),
            Triple(500, "login", FalloAuth.SERVIDOR)
        )
        casos.forEach { (codigo, ruta, esperado) ->
            responder(codigo, """{"error":{"code":"X","message":"x"}}""")
            val fallo = falloDe {
                when (ruta) {
                    "login" -> api.iniciarSesion("a@b.mx", "x")
                    "refresh" -> api.refrescar("r")
                    else -> api.registrar("a@b.mx", "A", "x", "AUTONOMO")
                }
            }
            assertEquals("$codigo en $ruta", esperado, fallo)
        }
    }

    @Test
    fun `una respuesta 200 que no es sesion es fallo del servidor`() = runTest {
        responder(200, "<html>proxy</html>")
        assertEquals(FalloAuth.SERVIDOR, falloDe { api.iniciarSesion("a@b.mx", "x") })
    }

    @Test
    fun `sin servidor es fallo de red`() = runTest {
        server.shutdown()
        assertEquals(FalloAuth.RED, falloDe { api.iniciarSesion("a@b.mx", "x") })
    }

    @Test
    fun `un corte de red al leer el cuerpo es fallo de red`() = runTest {
        server.enqueue(
            MockResponse().setBody(Tokens.respuestaSesion())
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        )
        assertEquals(FalloAuth.RED, falloDe { api.iniciarSesion("ana@correo.mx", "Segura12345") })
    }

    @Test
    fun `el cuerpo se lee en el dispatcher de io`() = runTest {
        var hiloLectura: String? = null
        val cliente = OkHttpClient.Builder().addInterceptor { chain ->
            val respuesta = chain.proceed(chain.request())
            val original = respuesta.body!!
            val espia = object : ForwardingSource(original.source()) {
                override fun read(sink: Buffer, byteCount: Long): Long {
                    hiloLectura = Thread.currentThread().name
                    return super.read(sink, byteCount)
                }
            }
            respuesta.newBuilder().body(espia.buffer().asResponseBody(original.contentType(), -1)).build()
        }.build()
        val io = Executors.newSingleThreadExecutor { Thread(it, "io-prueba") }.asCoroutineDispatcher()
        try {
            responder(200, Tokens.respuestaSesion())
            AuthApi(server.url("/"), cliente, io).iniciarSesion("ana@correo.mx", "Segura12345")
            assertEquals("io-prueba", hiloLectura)
        } finally {
            io.close()
        }
    }
}
