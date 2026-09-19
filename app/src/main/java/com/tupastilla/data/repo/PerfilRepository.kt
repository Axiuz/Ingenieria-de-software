package com.tupastilla.data.repo

import com.tupastilla.data.local.Densidad
import com.tupastilla.data.local.Perfil
import com.tupastilla.data.local.PerfilDao
import com.tupastilla.data.local.Rol
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/**
 * [espejo] recibe cada escritura para que la apariencia quede tambien en
 * SharedPreferences: el tema se aplica antes de que Room pueda responder.
 */
class PerfilRepository(
    private val dao: PerfilDao,
    private val espejo: (Perfil?) -> Unit = {}
) {

    fun observar(): Flow<Perfil?> = dao.observar()

    suspend fun actual(): Perfil? = dao.obtener()

    /** El perfil que ya existe, o uno recien creado con los valores por defecto. */
    private suspend fun oNuevo(): Perfil = dao.obtener()
        ?: Perfil(nombre = "", rol = Rol.AUTONOMO, modoVision = 1, densidad = Densidad.ACCESIBLE)

    suspend fun fijarRol(rol: String) = guardar(oNuevo().copy(rol = rol))

    /** No pisa un nombre que el usuario ya haya puesto. */
    suspend fun fijarSesion(nombre: String, rol: String) {
        val actual = oNuevo()
        guardar(actual.copy(nombre = actual.nombre.ifBlank { nombre }, rol = rolLocal(rol)))
    }

    suspend fun fijarModoVision(modo: Int) = guardar(oNuevo().copy(modoVision = modo))

    suspend fun fijarDensidad(densidad: String) = guardar(oNuevo().copy(densidad = densidad))

    suspend fun fijarNombre(nombre: String) = guardar(oNuevo().copy(nombre = nombre))

    /** Filtro de la pantalla de Hoy en el rol cuidador. Null: todas sus personas. */
    suspend fun fijarPersonaActiva(personaId: Long?) =
        guardar(oNuevo().copy(personaActivaId = personaId))

    suspend fun fijarAvisos(exacto: Boolean, repetir: Boolean, familiar: Boolean) =
        guardar(oNuevo().copy(avisoExacto = exacto, avisoRepetir = repetir, avisoFamiliar = familiar))

    private suspend fun guardar(perfil: Perfil) {
        dao.guardar(perfil)
        espejo(perfil)
    }

    /** Codigo de seis digitos que vence a los 15 minutos (C12a). */
    suspend fun generarCodigo(ahora: Long = System.currentTimeMillis()): String {
        val codigo = Random.nextInt(100_000, 1_000_000).toString()
        guardar(oNuevo().copy(codigoVinculo = codigo, codigoGeneradoEn = ahora, vinculado = true))
        return codigo
    }

    suspend fun borrar() {
        dao.borrar()
        espejo(null)
    }

    companion object {
        /** ADMIN y cualquier rol desconocido pasan a AUTONOMO: la app no tiene pantallas de administrador. */
        fun rolLocal(rol: String): String =
            if (rol == Rol.SUPERVISADO || rol == Rol.CUIDADOR) rol else Rol.AUTONOMO

        const val VIGENCIA_CODIGO_MS = 15 * 60 * 1000L
    }
}
