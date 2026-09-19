package com.tupastilla.auth

class SesionRepository(
    private val api: AuthApi,
    private val almacen: AlmacenSesion,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    @Volatile private var cache: Sesion? = null

    val actual: Sesion?
        get() = cache ?: almacen.leer()?.also { cache = it }

    fun haySesion(): Boolean = actual != null

    suspend fun iniciarSesion(correo: String, password: String): Sesion =
        guardar(api.iniciarSesion(ValidadorCredenciales.normalizarCorreo(correo), password))

    suspend fun registrar(correo: String, nombre: String, password: String, rol: String): Sesion =
        guardar(api.registrar(ValidadorCredenciales.normalizarCorreo(correo), nombre.trim(), password, rol))

    /**
     * Refresca si el access token caduco. Solo borra la sesion si el servidor dice que el
     * refresh ya no vale; un fallo de red o del servidor conserva la sesion y se propaga.
     */
    suspend fun tokenVigente(): String? {
        val sesion = actual ?: return null
        val claims = JwtClaims.leer(sesion.accessToken)
        if (claims != null && claims.vigente(reloj())) return sesion.accessToken
        return try {
            guardar(api.refrescar(sesion.refreshToken)).accessToken
        } catch (e: AuthException) {
            if (e.fallo != FalloAuth.SESION_EXPIRADA) throw e
            olvidar()
            null
        }
    }

    /** Olvida la sesion local antes de avisar al servidor, asi cerrar sesion funciona sin conexion. */
    suspend fun cerrarSesion() {
        val sesion = actual
        olvidar()
        if (sesion != null) {
            runCatching { api.cerrarSesion(sesion.refreshToken) }
        }
    }

    private fun guardar(sesion: Sesion): Sesion {
        almacen.guardar(sesion)
        cache = sesion
        return sesion
    }

    private fun olvidar() {
        cache = null
        almacen.borrar()
    }
}
