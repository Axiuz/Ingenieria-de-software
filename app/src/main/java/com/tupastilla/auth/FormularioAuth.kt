package com.tupastilla.auth

import android.view.View
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.alarma.AlarmaScheduler
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.TuPastillaDatabase
import com.tupastilla.ui.Preferencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base comun de login y registro: marca errores por campo, bloquea los botones mientras
 * espera y, al entrar, pasa nombre y rol de la cuenta al perfil local.
 */
abstract class FormularioAuth(layout: Int) : Fragment(layout) {

    protected fun texto(vista: View, campoId: Int): String =
        vista.findViewById<TextInputLayout>(campoId).editText?.text?.toString().orEmpty()

    protected fun marcar(vista: View, campoId: Int, error: ErrorCampo?): Boolean {
        vista.findViewById<TextInputLayout>(campoId).error = error?.let { getString(MensajesAuth.de(it)) }
        return error == null
    }

    /**
     * Lee hayPerfil antes de fijarSesion, porque fijarSesion crea el perfil: con perfil
     * previo se va directo a Hoy, sin onboarding.
     */
    protected fun enviar(vista: View, accion: suspend () -> Sesion) {
        val error = vista.findViewById<TextView>(R.id.error)
        error.visibility = View.GONE
        ocupado(vista, true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sesion = accion()
                CuentaLocal.de(requireContext()).reclamar(sesion.usuario.id) { borrarDatosLocales() }
                val teniaPerfil = Preferencias(requireContext()).hayPerfil
                Grafo.perfiles(requireContext()).fijarSesion(sesion.usuario.nombre, sesion.usuario.rol)
                (activity as MainActivity).entrarConSesion(teniaPerfil)
            } catch (e: AuthException) {
                error.setText(MensajesAuth.de(e.fallo))
                error.visibility = View.VISIBLE
                ocupado(vista, false)
            }
        }
    }

    /**
     * Cancela primero alarmas y notificaciones: tras vaciar la base los ids de toma se reutilizan
     * y una alarma vieja sonaria para una toma de la cuenta nueva. Luego vacia Room y preferencias,
     * lo que manda al onboarding.
     */
    private suspend fun borrarDatosLocales() {
        val contexto = requireContext().applicationContext
        withContext(Dispatchers.IO) {
            AlarmaScheduler(contexto).cancelarTodo()
            NotificationManagerCompat.from(contexto).cancelAll()
            TuPastillaDatabase.get(contexto).clearAllTables()
        }
        Preferencias(contexto).guardar(null)
    }

    private fun ocupado(vista: View, si: Boolean) {
        vista.findViewById<View>(R.id.cargando).visibility = if (si) View.VISIBLE else View.GONE
        botones(vista).forEach { it.isEnabled = !si }
    }

    protected abstract fun botones(vista: View): List<View>
}
