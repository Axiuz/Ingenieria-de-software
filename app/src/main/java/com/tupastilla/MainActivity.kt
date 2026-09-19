package com.tupastilla

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.tupastilla.ajustes.AjustesFragment
import com.tupastilla.alerta.ConfirmacionFragment
import com.tupastilla.auth.LoginFragment
import com.tupastilla.data.Grafo
import com.tupastilla.historial.HistorialFragment
import com.tupastilla.hoy.HoyFragment
import com.tupastilla.medicinas.MisMedicinasFragment
import com.tupastilla.data.local.Rol
import com.tupastilla.onboarding.BienvenidaFragment
import com.tupastilla.personas.PersonasFragment
import com.tupastilla.ui.Preferencias
import com.tupastilla.ui.Tema
import com.tupastilla.ui.colorDeAtributo

/**
 * Activity unica. El onboarding y las cuatro pestanas viven aqui; la alerta de toma
 * es una Activity aparte porque se lanza desde una notificacion de pantalla completa.
 */
class MainActivity : AppCompatActivity() {

    private data class Pestana(val vistaId: Int, val iconoId: Int, val etiqueta: Int)

    /** El cuidador administra personas, no una lista propia: la pestana 2 cambia. */
    private var esCuidador = false

    private val pestanas = listOf(
        Pestana(R.id.tabHoy, R.drawable.ic_hoy, R.string.tab_hoy),
        Pestana(R.id.tabMedicinas, R.drawable.ic_medicinas, R.string.tab_medicinas),
        Pestana(R.id.tabHistorial, R.drawable.ic_historial, R.string.tab_historial),
        Pestana(R.id.tabAjustes, R.drawable.ic_ajustes, R.string.tab_ajustes)
    )

    private var activa = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Tema.aplicar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configurarPestanas()

        if (savedInstanceState == null) {
            when {
                !Grafo.sesion(this).haySesion() -> abrirLogin()
                Preferencias(this).hayPerfil -> irAPestana(0)
                else -> abrirOnboarding()
            }
        }
    }

    /** Vacia la pila para que Atras no regrese a una pantalla con sesion. */
    fun abrirLogin() {
        mostrarBarra(false)
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, LoginFragment()).commit()
    }

    /** Con perfil previo va a Hoy; sin el, al onboarding. */
    fun entrarConSesion(teniaPerfil: Boolean) {
        if (teniaPerfil) terminarOnboarding() else abrirOnboarding()
    }

    /** C0. Sin pestanas: hasta que no hay perfil no hay a donde navegar. */
    fun abrirOnboarding() {
        mostrarBarra(false)
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, BienvenidaFragment()).commit()
    }

    /**
     * Ultimo paso del onboarding: aparecen las pestanas y se entra por Hoy.
     *
     * Se reconfiguran en sitio y no con recreate(): al recrear, savedInstanceState
     * deja de ser null y se restauraria el fragmento del onboarding en vez de Hoy.
     */
    fun terminarOnboarding() {
        configurarPestanas()
        irAPestana(0)
    }

    /** Relee el rol y pone icono, texto y accion a cada pestana. */
    private fun configurarPestanas() {
        esCuidador = Preferencias(this).rol == Rol.CUIDADOR
        pestanas.forEachIndexed { indice, pestana ->
            val etiqueta = if (indice == PESTANA_LISTA && esCuidador) {
                R.string.tab_personas
            } else {
                pestana.etiqueta
            }
            val vista = findViewById<View>(pestana.vistaId)
            vista.findViewById<ImageView>(R.id.icono).setImageResource(pestana.iconoId)
            vista.findViewById<TextView>(R.id.etiqueta).setText(etiqueta)
            vista.contentDescription = getString(etiqueta)
            vista.setOnClickListener { irAPestana(indice) }
        }
    }

    fun irAPestana(indice: Int) {
        activa = indice
        mostrarBarra(true)
        val fragment: Fragment = when (indice) {
            0 -> HoyFragment()
            1 -> if (esCuidador) PersonasFragment() else MisMedicinasFragment()
            2 -> HistorialFragment()
            else -> AjustesFragment()
        }
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
        pintarPestanas()
    }

    /** Abre una pantalla encima de la pestana activa, con boton de volver. */
    fun apilar(fragment: Fragment, etiqueta: String? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(etiqueta)
            .commit()
    }

    /** Reemplaza sin apilar: lo usa el onboarding, que avanza en linea recta. */
    fun avanzar(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    fun volver() {
        if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()
        else irAPestana(activa)
    }

    /**
     * A3 despues de confirmar desde la lista de Hoy. Sin pestanas: es una pantalla de
     * resultado, no un destino al que se navegue.
     */
    fun mostrarConfirmacion(texto: String) {
        mostrarBarra(false)
        val confirmacion = ConfirmacionFragment.nueva(texto).apply {
            alVolver = { irAPestana(0) }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, confirmacion).commit()
    }

    fun mostrarBarra(visible: Boolean) {
        findViewById<View>(R.id.bottomBar).visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * La pestana activa se marca con una barra superior ademas del color: en el modo
     * sin color el color solo no la distinguiria.
     */
    private fun pintarPestanas() {
        val encendido = colorDeAtributo(R.attr.tpAccion)
        val apagado = colorDeAtributo(R.attr.tpPendiente)
        pestanas.forEachIndexed { indice, pestana ->
            val seleccionada = indice == activa
            val color = if (seleccionada) encendido else apagado
            val vista = findViewById<View>(pestana.vistaId)
            vista.findViewById<ImageView>(R.id.icono).setColorFilter(color)
            vista.findViewById<TextView>(R.id.etiqueta).setTextColor(color)
            vista.findViewById<View>(R.id.indicador)
                .setBackgroundColor(if (seleccionada) encendido else 0x00000000)
            vista.isSelected = seleccionada
        }
    }

    private companion object {
        /** La pestana 2: "Medicinas" para el paciente, "Personas" para el cuidador. */
        const val PESTANA_LISTA = 1
    }
}
