package com.tupastilla.auth

enum class FalloAuth { CREDENCIALES, CORREO_OCUPADO, DATOS_INVALIDOS, DEMASIADOS_INTENTOS, SESION_EXPIRADA, RED, SERVIDOR }

class AuthException(val fallo: FalloAuth, causa: Throwable? = null) : Exception(fallo.name, causa)
