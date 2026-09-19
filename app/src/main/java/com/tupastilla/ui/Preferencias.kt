package com.tupastilla.ui

import android.content.Context
import com.tupastilla.data.local.Densidad
import com.tupastilla.data.local.Perfil
import com.tupastilla.data.local.Rol

/**
 * Espejo en SharedPreferences de las tres cosas que hacen falta antes de que Room
 * pueda responder: el modo de vision, la densidad y si ya hay perfil. El tema se
 * aplica antes de super.onCreate y no se puede esperar a una consulta suspend.
 * Room sigue siendo la fuente de verdad; esto es solo una copia rapida.
 */
class Preferencias(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("tupastilla_apariencia", Context.MODE_PRIVATE)

    val modoVision: Int get() = sp.getInt(MODO, 1)
    val densidad: String get() = sp.getString(DENSIDAD, Densidad.ACCESIBLE) ?: Densidad.ACCESIBLE
    val hayPerfil: Boolean get() = sp.getBoolean(HAY_PERFIL, false)

    /** El rol decide que pestanas se muestran, y eso se decide antes de consultar Room. */
    val rol: String get() = sp.getString(ROL, Rol.AUTONOMO) ?: Rol.AUTONOMO

    fun guardar(perfil: Perfil?) {
        sp.edit().apply {
            if (perfil == null) {
                clear()
            } else {
                putInt(MODO, perfil.modoVision)
                putString(DENSIDAD, perfil.densidad)
                putString(ROL, perfil.rol)
                putBoolean(HAY_PERFIL, true)
            }
        }.apply()
    }

    private companion object {
        const val MODO = "modo_vision"
        const val DENSIDAD = "densidad"
        const val ROL = "rol"
        const val HAY_PERFIL = "hay_perfil"
    }
}
