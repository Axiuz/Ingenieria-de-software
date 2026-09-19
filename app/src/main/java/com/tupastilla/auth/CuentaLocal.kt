package com.tupastilla.auth

import android.content.Context
import androidx.core.content.edit

/**
 * Recuerda de que cuenta son los datos locales (Room y preferencias) de este telefono.
 * Sin esto, al cerrar sesion y entrar con otra cuenta, la nueva veia los medicamentos,
 * el nombre y las alarmas de la anterior.
 */
class CuentaLocal(private val almacen: Almacen) {

    /**
     * Permite probar la logica sin Android. AlmacenCuentaPrefs es la version real y queda
     * fuera de la cobertura, como AlmacenSesionCifrado.
     */
    interface Almacen {
        var duenoId: String?
    }

    val duenoId: String? get() = almacen.duenoId

    /**
     * Borra los datos si son de otra cuenta; si no tienen dueno (son de antes del login) los adopta.
     * El dueno se guarda despues de borrar: si borrar falla, el siguiente login lo vuelve a intentar.
     */
    suspend fun reclamar(usuarioId: String, borrarDatos: suspend () -> Unit): Boolean {
        val borrar = debeBorrar(almacen.duenoId, usuarioId)
        if (borrar) borrarDatos()
        almacen.duenoId = usuarioId
        return borrar
    }

    companion object {
        fun debeBorrar(duenoId: String?, usuarioId: String): Boolean =
            duenoId != null && duenoId != usuarioId

        fun de(context: Context): CuentaLocal = CuentaLocal(AlmacenCuentaPrefs(context))
    }
}

class AlmacenCuentaPrefs(context: Context) : CuentaLocal.Almacen {

    private val sp = context.applicationContext
        .getSharedPreferences("tupastilla_cuenta", Context.MODE_PRIVATE)

    override var duenoId: String?
        get() = sp.getString(DUENO, null)
        set(valor) = sp.edit { putString(DUENO, valor) }

    private companion object {
        const val DUENO = "dueno_id"
    }
}
