package com.tupastilla.auth

import androidx.annotation.StringRes
import com.tupastilla.R

/** Traduce errores de validacion y fallos de la API a textos para el usuario. */
object MensajesAuth {

    @StringRes
    fun de(error: ErrorCampo): Int = when (error) {
        ErrorCampo.CORREO_INVALIDO -> R.string.error_correo_invalido
        ErrorCampo.PASSWORD_CORTA -> R.string.error_password_corta
        ErrorCampo.PASSWORD_SIN_LETRA -> R.string.error_password_sin_letra
        ErrorCampo.PASSWORD_SIN_NUMERO -> R.string.error_password_sin_numero
        ErrorCampo.PASSWORD_LARGA -> R.string.error_password_larga
        ErrorCampo.NOMBRE_VACIO -> R.string.error_nombre_vacio
    }

    @StringRes
    fun de(fallo: FalloAuth): Int = when (fallo) {
        FalloAuth.CREDENCIALES -> R.string.error_credenciales
        FalloAuth.CORREO_OCUPADO -> R.string.error_correo_ocupado
        FalloAuth.DATOS_INVALIDOS -> R.string.error_datos_invalidos
        FalloAuth.DEMASIADOS_INTENTOS -> R.string.error_demasiados_intentos
        FalloAuth.SESION_EXPIRADA -> R.string.error_sesion_expirada
        FalloAuth.RED -> R.string.error_red
        FalloAuth.SERVIDOR -> R.string.error_servidor
    }
}
