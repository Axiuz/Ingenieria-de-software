package com.tupastilla.auth

import org.json.JSONObject

data class UsuarioSesion(val id: String, val correo: String, val nombre: String, val rol: String)

data class Sesion(val accessToken: String, val refreshToken: String, val usuario: UsuarioSesion) {

    fun aJson(): String = JSONObject()
        .put("accessToken", accessToken)
        .put("refreshToken", refreshToken)
        .put("user", JSONObject()
            .put("id", usuario.id)
            .put("email", usuario.correo)
            .put("name", usuario.nombre)
            .put("role", usuario.rol))
        .toString()

    companion object {
        fun deJson(texto: String): Sesion {
            val json = JSONObject(texto)
            val user = json.getJSONObject("user")
            return Sesion(
                accessToken = json.getString("accessToken"),
                refreshToken = json.getString("refreshToken"),
                usuario = UsuarioSesion(
                    id = user.getString("id"),
                    correo = user.getString("email"),
                    nombre = user.getString("name"),
                    rol = user.getString("role")
                )
            )
        }
    }
}
