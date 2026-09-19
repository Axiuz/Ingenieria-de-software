package com.tupastilla.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.data.Grafo

/** Pantalla de entrada: la app no muestra nada hasta que hay sesion. */
class LoginFragment : FormularioAuth(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as MainActivity).mostrarBarra(false)
        view.findViewById<View>(R.id.entrar).setOnClickListener { entrar(view) }
        view.findViewById<View>(R.id.irARegistro).setOnClickListener {
            (activity as MainActivity).apilar(RegistroFragment(), "registro")
        }
        view.findViewById<android.widget.EditText>(R.id.entradaPassword)
            .setOnEditorActionListener { _, accion, _ ->
                (accion == EditorInfo.IME_ACTION_DONE).also { if (it) entrar(view) }
            }
    }

    private fun entrar(vista: View) {
        val correo = texto(vista, R.id.campoCorreo)
        val password = texto(vista, R.id.campoPassword)
        val correoOk = marcar(vista, R.id.campoCorreo, ValidadorCredenciales.correo(correo))
        val passwordOk = marcar(vista, R.id.campoPassword, if (password.isEmpty()) ErrorCampo.PASSWORD_CORTA else null)
        if (!correoOk || !passwordOk) return
        enviar(vista) { Grafo.sesion(requireContext()).iniciarSesion(correo, password) }
    }

    override fun botones(vista: View): List<View> =
        listOf(vista.findViewById(R.id.entrar), vista.findViewById(R.id.irARegistro))
}
