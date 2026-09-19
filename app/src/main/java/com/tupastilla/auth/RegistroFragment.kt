package com.tupastilla.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isNotEmpty
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Rol
import com.tupastilla.ui.dp

/** Crea la cuenta en la API. El rol se elige aqui, ya no en el onboarding. */
class RegistroFragment : FormularioAuth(R.layout.fragment_registro) {

    private var rol = Rol.AUTONOMO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)
        rol = savedInstanceState?.getString(ROL) ?: Rol.AUTONOMO
        view.findViewById<View>(R.id.volver).setOnClickListener { (activity as MainActivity).volver() }
        view.findViewById<View>(R.id.crearCuenta).setOnClickListener { crear(view) }
        pintarRoles(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ROL, rol)
    }

    private fun pintarRoles(vista: View) {
        val contenedor = vista.findViewById<LinearLayout>(R.id.opcionesRol)
        contenedor.removeAllViews()
        OPCIONES.forEach { (texto, valor) ->
            val opcion = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_opcion_rol, contenedor, false)
            opcion.findViewById<TextView>(R.id.texto).setText(texto)
            opcion.contentDescription = getString(texto)
            opcion.isSelected = valor == rol
            opcion.findViewById<View>(R.id.punto).alpha = if (valor == rol) 1f else 0.25f
            opcion.setOnClickListener {
                rol = valor
                pintarRoles(vista)
            }
            (opcion.layoutParams as LinearLayout.LayoutParams).topMargin =
                if (contenedor.isNotEmpty()) requireContext().dp(8f) else 0
            contenedor.addView(opcion)
        }
    }

    private fun crear(vista: View) {
        val nombre = texto(vista, R.id.campoNombre)
        val correo = texto(vista, R.id.campoCorreo)
        val password = texto(vista, R.id.campoPassword)
        val validos = listOf(
            marcar(vista, R.id.campoNombre, ValidadorCredenciales.nombre(nombre)),
            marcar(vista, R.id.campoCorreo, ValidadorCredenciales.correo(correo)),
            marcar(vista, R.id.campoPassword, ValidadorCredenciales.password(password))
        )
        if (validos.all { it }) {
            enviar(vista) { Grafo.sesion(requireContext()).registrar(correo, nombre, password, rol) }
        }
    }

    override fun botones(vista: View): List<View> = listOf(vista.findViewById(R.id.crearCuenta))

    private companion object {
        const val ROL = "rol"
        val OPCIONES = listOf(
            R.string.c10_autonomo to Rol.AUTONOMO,
            R.string.c10_supervisado to Rol.SUPERVISADO,
            R.string.c10_cuidador to Rol.CUIDADOR
        )
    }
}
