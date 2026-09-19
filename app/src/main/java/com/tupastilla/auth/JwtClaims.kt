package com.tupastilla.auth

import okio.ByteString.Companion.decodeBase64
import org.json.JSONException
import org.json.JSONObject

/**
 * Lee el payload del access token sin verificar la firma, que la comprueba el servidor.
 * El cliente solo lo usa para saber el rol y cuando caduca.
 */
data class JwtClaims(val sujeto: String, val correo: String, val rol: String, val expiraEnSegundos: Long) {

    /** Da el token por caducado 30 s antes para cubrir relojes desfasados y la latencia de la peticion. */
    fun vigente(ahoraMs: Long, margenMs: Long = MARGEN_MS): Boolean =
        expiraEnSegundos * 1000 - margenMs > ahoraMs

    companion object {
        const val MARGEN_MS = 30_000L

        fun leer(token: String): JwtClaims? {
            val partes = token.split(".")
            if (partes.size != 3) return null
            val payload = partes[1].decodeBase64()?.utf8() ?: return null
            return try {
                val json = JSONObject(payload)
                JwtClaims(
                    sujeto = json.getString("sub"),
                    correo = json.getString("email"),
                    rol = json.getString("role"),
                    expiraEnSegundos = json.getLong("exp")
                )
            } catch (e: JSONException) {
                null
            }
        }
    }
}
