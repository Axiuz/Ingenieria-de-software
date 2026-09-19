package com.tupastilla.auth

import java.util.Base64

object Tokens {
    private val codificador = Base64.getUrlEncoder().withoutPadding()

    private fun b64(texto: String) = codificador.encodeToString(texto.toByteArray())

    fun jwt(expSegundos: Long, rol: String = "CUIDADOR", correo: String = "ana@correo.mx"): String =
        listOf(
            b64("""{"alg":"HS256","typ":"JWT"}"""),
            b64("""{"sub":"u1","email":"$correo","role":"$rol","exp":$expSegundos}"""),
            "firma"
        ).joinToString(".")

    fun respuestaSesion(accessToken: String = jwt(4_000_000_000), refresh: String = "refresh-1") = """
        {"tokenType":"Bearer","accessToken":"$accessToken","expiresIn":900,"refreshToken":"$refresh",
         "user":{"id":"u1","email":"ana@correo.mx","name":"Ana","role":"CUIDADOR","createdAt":"2026-09-18T12:00:00.000Z"}}
    """.trimIndent()
}
