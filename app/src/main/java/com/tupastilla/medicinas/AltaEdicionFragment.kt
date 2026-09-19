package com.tupastilla.medicinas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tupastilla.MainActivity
import com.tupastilla.R

/**
 * B6 · Alta y edicion. Con primeraVez=true es tambien C15, el ultimo paso del
 * onboarding: mismo formulario, un boton de saltar y otra salida al guardar.
 */
class AltaEdicionFragment : Fragment(R.layout.fragment_alta_edicion) {

    private val modelo: EdicionViewModel by activityViewModels()

    private val medicinaId: Long?
        get() = arguments?.getLong(MEDICINA_ID, -1L)?.takeIf { it >= 0 }

    private val personaId: Long?
        get() = arguments?.getLong(PERSONA_ID, -1L)?.takeIf { it >= 0 }

    private val primeraVez: Boolean
        get() = arguments?.getBoolean(PRIMERA_VEZ, false) == true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.titulo)
            .setText(if (medicinaId == null) R.string.b6_nueva else R.string.b6_editar)

        view.findViewById<MaterialButton>(R.id.volver).setOnClickListener {
            (activity as MainActivity).volver()
        }
        view.findViewById<View>(R.id.filaHorarios).setOnClickListener {
            volcarCampos(view)
            (activity as MainActivity).apilar(HorariosFragment(), "horarios")
        }
        view.findViewById<MaterialButton>(R.id.guardar).setOnClickListener { guardar(view) }
        view.findViewById<MaterialButton>(R.id.saltar).apply {
            visibility = if (primeraVez) View.VISIBLE else View.GONE
            setOnClickListener { (activity as MainActivity).terminarOnboarding() }
        }

        // Al volver de B7 el formulario ya esta cargado: no hay que recargarlo.
        if (savedInstanceState == null && !modelo.borrador.yaCargado(medicinaId)) {
            modelo.preparar(medicinaId, personaId, getString(R.string.persona_yo)) {
                if (isAdded) pintar(view)
            }
        } else {
            pintar(view)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { pintarResumenHorarios(it) }
    }

    private fun pintar(vista: View) {
        val borrador = modelo.borrador
        entrada(vista, R.id.entradaNombre).setText(borrador.nombre)
        entrada(vista, R.id.entradaDosis).setText(borrador.dosis)
        entrada(vista, R.id.entradaForma).setText(borrador.forma)
        entrada(vista, R.id.entradaInstrucciones).setText(borrador.instrucciones)
        pintarResumenHorarios(vista)

        // El error del nombre se limpia en cuanto se escribe, no al volver a guardar.
        entrada(vista, R.id.entradaNombre).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                vista.findViewById<TextInputLayout>(R.id.campoNombre).error = null
            }

            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        })
    }

    private fun pintarResumenHorarios(vista: View) {
        val horarios = modelo.borrador.horarios
        vista.findViewById<TextView>(R.id.resumenHorarios).text =
            if (horarios.isEmpty()) getString(R.string.b6_sin_horario)
            else horarios.joinToString(" y ") { it.hora }
    }

    /** Pasa lo escrito al borrador antes de navegar o de guardar. */
    private fun volcarCampos(vista: View) {
        modelo.actualizar {
            copy(
                nombre = entrada(vista, R.id.entradaNombre).text.toString(),
                dosis = entrada(vista, R.id.entradaDosis).text.toString(),
                forma = entrada(vista, R.id.entradaForma).text.toString(),
                instrucciones = entrada(vista, R.id.entradaInstrucciones).text.toString()
            )
        }
    }

    private fun guardar(vista: View) {
        volcarCampos(vista)
        val valido = modelo.guardar {
            if (primeraVez) (activity as MainActivity).terminarOnboarding()
            else (activity as MainActivity).volver()
        }
        if (!valido) {
            vista.findViewById<TextInputLayout>(R.id.campoNombre).error =
                getString(R.string.b6_error_nombre)
        }
    }

    private fun entrada(vista: View, id: Int): TextInputEditText = vista.findViewById(id)

    companion object {
        private const val MEDICINA_ID = "medicina_id"
        private const val PERSONA_ID = "persona_id"
        private const val PRIMERA_VEZ = "primera_vez"

        fun nueva(medicinaId: Long? = null, personaId: Long? = null, primeraVez: Boolean = false) =
            AltaEdicionFragment().apply {
                arguments = Bundle().apply {
                    putLong(MEDICINA_ID, medicinaId ?: -1L)
                    putLong(PERSONA_ID, personaId ?: -1L)
                    putBoolean(PRIMERA_VEZ, primeraVez)
                }
            }
    }
}

/** El borrador ya corresponde a esta medicina: volver de B7 no debe recargarlo. */
private fun Borrador.yaCargado(medicinaId: Long?): Boolean =
    if (medicinaId == null) id == 0L && (nombre.isNotEmpty() || horarios.isNotEmpty())
    else id == medicinaId
