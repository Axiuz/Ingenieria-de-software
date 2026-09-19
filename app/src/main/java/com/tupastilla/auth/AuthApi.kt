package com.tupastilla.auth

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class AuthApi(
    private val baseUrl: HttpUrl,
    private val cliente: OkHttpClient,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun registrar(correo: String, nombre: String, password: String, rol: String): Sesion =
        sesion("register", JSONObject()
            .put("email", correo).put("name", nombre).put("password", password).put("role", rol))

    suspend fun iniciarSesion(correo: String, password: String): Sesion =
        sesion("login", JSONObject().put("email", correo).put("password", password))

    suspend fun refrescar(refreshToken: String): Sesion =
        sesion("refresh", JSONObject().put("refreshToken", refreshToken))

    suspend fun cerrarSesion(refreshToken: String) {
        publicar("logout", JSONObject().put("refreshToken", refreshToken))
    }

    private suspend fun sesion(ruta: String, cuerpo: JSONObject): Sesion {
        val texto = publicar(ruta, cuerpo)
        return try {
            Sesion.deJson(texto)
        } catch (e: JSONException) {
            throw AuthException(FalloAuth.SERVIDOR, e)
        }
    }

    /**
     * Lee el cuerpo dentro del dispatcher de IO: leerlo fuera cerraba la app con
     * NetworkOnMainThreadException. Un corte de red al leerlo tambien es FalloAuth.RED.
     */
    private suspend fun publicar(ruta: String, cuerpo: JSONObject): String = withContext(io) {
        val url = baseUrl.newBuilder().addPathSegments("api/auth/$ruta").build()
        val peticion = Request.Builder()
            .url(url)
            .post(cuerpo.toString().toRequestBody(JSON))
            .build()
        try {
            cliente.newCall(peticion).execute().use { respuesta ->
                if (!respuesta.isSuccessful) throw AuthException(falloPara(ruta, respuesta.code))
                respuesta.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw AuthException(FalloAuth.RED, e)
        }
    }

    /** Un 401 en login son credenciales malas; en refresh, sesion vencida. */
    private fun falloPara(ruta: String, codigo: Int): FalloAuth = when {
        codigo == 401 && ruta == "login" -> FalloAuth.CREDENCIALES
        codigo == 401 -> FalloAuth.SESION_EXPIRADA
        codigo == 409 -> FalloAuth.CORREO_OCUPADO
        codigo == 400 || codigo == 413 -> FalloAuth.DATOS_INVALIDOS
        codigo == 429 -> FalloAuth.DEMASIADOS_INTENTOS
        else -> FalloAuth.SERVIDOR
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
