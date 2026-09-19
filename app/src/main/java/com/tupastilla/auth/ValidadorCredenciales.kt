package com.tupastilla.auth

enum class ErrorCampo { CORREO_INVALIDO, PASSWORD_CORTA, PASSWORD_SIN_LETRA, PASSWORD_SIN_NUMERO, PASSWORD_LARGA, NOMBRE_VACIO }

/**
 * Mismas reglas que la API (10 caracteres, letra y numero, maximo 72 bytes) para avisar
 * antes de mandar la peticion.
 */
object ValidadorCredenciales {

    const val PASSWORD_MINIMA = 10
    private const val BYTES_MAXIMOS = 72
    private val CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun normalizarCorreo(correo: String): String = correo.trim().lowercase()

    fun correo(correo: String): ErrorCampo? =
        if (CORREO.matches(normalizarCorreo(correo)) && correo.length <= 254) null
        else ErrorCampo.CORREO_INVALIDO

    fun password(password: String): ErrorCampo? = when {
        password.length < PASSWORD_MINIMA -> ErrorCampo.PASSWORD_CORTA
        password.toByteArray(Charsets.UTF_8).size > BYTES_MAXIMOS -> ErrorCampo.PASSWORD_LARGA
        password.none { it.isLetter() } -> ErrorCampo.PASSWORD_SIN_LETRA
        password.none { it.isDigit() } -> ErrorCampo.PASSWORD_SIN_NUMERO
        else -> null
    }

    fun nombre(nombre: String): ErrorCampo? =
        if (nombre.isBlank()) ErrorCampo.NOMBRE_VACIO else null
}
