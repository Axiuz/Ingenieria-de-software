package com.tupastilla.ajustes

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.tupastilla.MainActivity
import com.tupastilla.R
import com.tupastilla.alarma.AlarmaScheduler
import com.tupastilla.alarma.Notificaciones
import com.tupastilla.data.Grafo
import com.tupastilla.data.local.Densidad
import com.tupastilla.data.local.Perfil
import com.tupastilla.data.local.Rol
import com.tupastilla.ui.ModosDeVision
import com.tupastilla.ui.Roles
import com.tupastilla.ui.colorDeAtributo
import com.tupastilla.ui.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * C16 · Apariencia, avisos, vinculacion y datos de prueba. Cambiar modo o densidad
 * guarda la preferencia y llama recreate(): el tema se aplica en onCreate.
 */
class AjustesFragment : Fragment(R.layout.fragment_ajustes) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pintarRoles(view)
        pintarModos(view)
        pintarDensidad(view)
        pintarAvisos(view)
        pintarAcciones(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Grafo.perfiles(requireContext()).observar().collect { perfil ->
                    pintarPerfil(view, perfil)
                    pintarEstadoAvisos(view, perfil)
                    view.findViewById<TextView>(R.id.estadoVinculo)
                        .setText(if (perfil?.vinculado == true) R.string.c16_vinculado else R.string.c16_sin_vincular)
                }
            }
        }
    }

    private fun pintarPerfil(vista: View, perfil: Perfil?) {
        val nombre = perfil?.nombre?.ifBlank { null } ?: getString(R.string.c16_sin_nombre)
        vista.findViewById<TextView>(R.id.nombrePerfil).text = nombre
        vista.findViewById<TextView>(R.id.iniciales).text = nombre
            .split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.take(1).uppercase(Locale.forLanguageTag("es")) }
        vista.findViewById<TextView>(R.id.rolPerfil).setText(
            when (perfil?.rol) {
                Rol.SUPERVISADO -> R.string.c16_rol_supervisado
                Rol.CUIDADOR -> R.string.c16_rol_cuidador
                else -> R.string.c16_rol_autonomo
            }
        )
    }

    /**
     * Los tres roles como botones de solo lectura: el rol lo firma el servidor en el
     * token y cambiarlo aqui seria saltarse la autorizacion.
     */
    private fun pintarRoles(vista: View) {
        val contenedor = vista.findViewById<LinearLayout>(R.id.roles)
        contenedor.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val actual = Grafo.perfiles(requireContext()).actual()?.rol
            if (!isAdded) return@launch

            Roles.todos.forEach { tipo ->
                val boton = LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_boton_opcion, contenedor, false) as MaterialButton
                boton.text = getString(tipo.nombreCorto)
                if (tipo.valor == Roles.de(actual).valor) {
                    boton.strokeColor = ColorStateList
                        .valueOf(requireContext().colorDeAtributo(R.attr.tpAccion))
                    boton.strokeWidth = requireContext().dp(3f)
                }
                boton.isClickable = false
                boton.isEnabled = tipo.valor == Roles.de(actual).valor
                (boton.layoutParams as LinearLayout.LayoutParams).topMargin =
                    if (contenedor.childCount > 0) requireContext().dp(8f) else 0
                contenedor.addView(boton)
            }
            vista.findViewById<TextView>(R.id.rolDescripcion)
                .setText(Roles.de(actual).descripcion)
        }
    }

    /** Los cuatro modos como botones de ancho completo, uno por fila. */
    private fun pintarModos(vista: View) {
        val contenedor = vista.findViewById<LinearLayout>(R.id.modos)
        val actual = com.tupastilla.ui.Preferencias(requireContext()).modoVision
        contenedor.removeAllViews()

        ModosDeVision.todos.forEach { modo ->
            val boton = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_boton_opcion, contenedor, false) as MaterialButton
            boton.text = getString(modo.nombreCorto)
            // El modo activo se marca con un borde de 3 dp, no solo con color.
            if (modo.numero == actual) {
                boton.strokeColor = ColorStateList
                    .valueOf(requireContext().colorDeAtributo(R.attr.tpAccion))
                boton.strokeWidth = requireContext().dp(3f)
            }
            boton.setOnClickListener { cambiarModo(modo.numero) }
            (boton.layoutParams as LinearLayout.LayoutParams).topMargin =
                if (contenedor.childCount > 0) requireContext().dp(8f) else 0
            contenedor.addView(boton)
        }
        vista.findViewById<TextView>(R.id.modoDescripcion)
            .setText(ModosDeVision.de(actual).descripcion)
    }

    private fun pintarDensidad(vista: View) {
        val actual = com.tupastilla.ui.Preferencias(requireContext()).densidad
        val grande = vista.findViewById<MaterialButton>(R.id.densidadGrande)
        val compacta = vista.findViewById<MaterialButton>(R.id.densidadCompacta)
        val accion = requireContext().colorDeAtributo(R.attr.tpAccion)

        listOf(grande to Densidad.ACCESIBLE, compacta to Densidad.COMPACTA)
            .forEach { (boton, valor) ->
                if (actual == valor) {
                    boton.strokeColor = ColorStateList.valueOf(accion)
                    boton.strokeWidth = requireContext().dp(3f)
                }
                boton.setOnClickListener { cambiarDensidad(valor) }
            }
    }

    private fun cambiarModo(modo: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            Grafo.perfiles(requireContext()).fijarModoVision(modo)
            activity?.recreate()
        }
    }

    private fun cambiarDensidad(densidad: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            Grafo.perfiles(requireContext()).fijarDensidad(densidad)
            activity?.recreate()
        }
    }

    private fun pintarAvisos(vista: View) {
        fila(vista, R.id.avisoExacto, R.string.c16_exacta, R.string.c16_exacta_desc)
        fila(vista, R.id.avisoRepetir, R.string.c16_repetir, R.string.c16_repetir_desc)
        fila(vista, R.id.avisoFamiliar, R.string.c16_familiar, R.string.c16_familiar_desc)
    }

    private fun fila(vista: View, filaId: Int, titulo: Int, descripcion: Int) {
        val fila = vista.findViewById<View>(filaId)
        fila.findViewById<TextView>(R.id.switchTitulo).setText(titulo)
        fila.findViewById<TextView>(R.id.switchDesc).setText(descripcion)
        fila.findViewById<MaterialSwitch>(R.id.interruptor).contentDescription = getString(titulo)
    }

    private fun pintarEstadoAvisos(vista: View, perfil: Perfil?) {
        val exacto = interruptor(vista, R.id.avisoExacto)
        val repetir = interruptor(vista, R.id.avisoRepetir)
        val familiar = interruptor(vista, R.id.avisoFamiliar)

        exacto.isChecked = perfil?.avisoExacto ?: true
        repetir.isChecked = perfil?.avisoRepetir ?: true
        familiar.isChecked = perfil?.avisoFamiliar ?: false

        val guardar = {
            viewLifecycleOwner.lifecycleScope.launch {
                Grafo.perfiles(requireContext())
                    .fijarAvisos(exacto.isChecked, repetir.isChecked, familiar.isChecked)
            }
            Unit
        }
        listOf(exacto, repetir, familiar).forEach { it.setOnClickListener { guardar() } }
    }

    private fun interruptor(vista: View, filaId: Int): MaterialSwitch =
        vista.findViewById<View>(filaId).findViewById(R.id.interruptor)

    private fun pintarAcciones(vista: View) {
        vista.findViewById<View>(R.id.filaVinculacion).setOnClickListener {
            (activity as MainActivity).apilar(VinculacionFragment(), "vinculacion")
        }

        // Boton de prueba: dispara el aviso de la proxima toma sin esperar a su hora.
        vista.findViewById<MaterialButton>(R.id.verAviso).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                Notificaciones.crearCanal(requireContext())
                val sono = AlarmaScheduler(requireContext()).sonarAhora()
                if (!sono) avisar(getString(R.string.a1_sin_meds_desc))
            }
        }

        vista.findViewById<MaterialButton>(R.id.datosPrueba).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                // La semilla depende del rol: el cuidador necesita varias personas.
                val rol = Grafo.perfiles(requireContext()).actual()?.rol ?: Rol.AUTONOMO
                Grafo.semilla(requireContext()).cargar(rol)
                launch(Dispatchers.IO) { AlarmaScheduler(requireContext()).reprogramarTodo() }
                (activity as MainActivity).irAPestana(0)
            }
        }

        vista.findViewById<MaterialButton>(R.id.cerrarSesion).setOnClickListener { confirmarCierre() }

        vista.findViewById<MaterialButton>(R.id.borrarTodo).setOnClickListener { confirmarBorrado() }
    }

    private fun confirmarBorrado() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.c16_borrar_confirmar)
            .setMessage(R.string.c16_borrar_desc)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.c16_borrar_todo) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Grafo.medicamentos(requireContext()).borrarTodo()
                    Grafo.perfiles(requireContext()).borrar()
                    (activity as MainActivity).abrirOnboarding()
                }
            }
            .show()
    }

    /** Pide confirmacion antes de cerrar sesion. */
    private fun confirmarCierre() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.c16_cerrar_sesion_confirmar)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.c16_cerrar_sesion) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Grafo.sesion(requireContext()).cerrarSesion()
                    (activity as MainActivity).abrirLogin()
                }
            }
            .show()
    }

    private fun avisar(mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(mensaje)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
